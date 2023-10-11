package codes.rorak.hamley.bot.mail

import codes.rorak.hamley.bot.moderation.Moderation.sendConfirmation
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import net.axay.simplekotlinmail.delivery.sendSync
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.selections.SelectOption
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateRequest
import org.simplejavamail.MailException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.BooleanSupplier

object Mail {
    fun startMailProcess(user: User, event: IReplyCallback) {
        event.dfrRpl();

        // check permissions
        if (!canWriteEmail(user)) {
            event.replyNoPerms();
            return;
        }
        // check if no email in process
        if (isWritingEmail(user)) {
            event.replyInvalidEmailAction(true, messages.constants.startMail);
            return;
        }

        // start email
        user.emailObj.inProcess = ConfigModel.EmailMessage();
        saveDb();

        // send email view
        viewMail(user, event, false);
    }
    fun stopMailProcess(user: User, event: IReplyCallback, otherAction: BooleanSupplier? = null) =
        genericConfirmAction(user, event, messages.constants.stopMail, "mailconfirmstop", otherAction);
    fun sendMailProcess(user: User, event: IReplyCallback, otherAction: BooleanSupplier? = null) =
        genericConfirmAction(user, event, messages.constants.sendMail, "mailconfirmsend", otherAction);
    fun viewMail(user: User, event: IReplyCallback, deferReply: Boolean = true) {
        if (deferReply) event.dfrRpl();

        if (!canWriteEmail(user)) {
            event.replyNoPerms();
            return;
        }
        if (!isWritingEmail(user)) {
            event.replyInvalidEmailAction(false, messages.constants.viewMail);
            return;
        }

        // sends message
        val embed = emailViewMessage(user.emailObj, user);
        val message = event.hook.sendMessageEmbeds(embed).setMailActionRow(user.emailObj).complete();

        // handle already existing message according to the mafia
        if (user.emailObj.inProcessMessage != null)
            user.emailObj.inProcessMessage!!.toMessageS()?.tryDeleteComplete();

        // edit inProcessMessage (id)
        user.emailObj.inProcessMessage = Triple(message.id, if (event.isFromGuild) event.channel!!.id else event.user.id, !event.isFromGuild);
        saveDb();
    }

    fun addAddressesToGroup(user: User, group: String, addresses: Collection<String>, event: IReplyCallback) {
        event.dfrRplE();

        if (!canWriteEmail(user)) {
            event.replyNoPerms(true);
            return;
        }
        addresses.forEach {
            if (it.inGroup(group, user)) {
                event.replyInvalidEmailAction(action = messages.constants.addAddressToGroup, group = true, subject = it, ephemeral = true);
                return;
            }
        };

        // add to db
        user.emailObj.groups[group]!!.addresses.addAll(addresses);
        saveDb();

        // reply
        event.hook.sendMessage(messages.constants.addAddressToGroupSuccess).setEphemeral(true).queue();
    }
    fun removeAddressFromGroup(user: User, group: String, address: String, event: IReplyCallback) {
        event.dfrRplE();

        if (!canWriteEmail(user)) {
            event.replyNoPerms(true);
            return;
        }

        // confirm
        sendConfirmation(messages.constants.removeAddressFromGroup, "mailconfirmremovefromgroup-${group}-${address}", event);
    }

    internal fun stopMailForeal(user: User, event: IReplyCallback) {
        event.dfrRpl();

        cleanUserMailMessage(db.emails[user.id]!!, user.id);

        event.hook.sendMessage(messages.constants.stopMailSuccess).queueAndDel();
    }
    internal fun sendMailForeal(user: User, event: IReplyCallback) {
        event.dfrRplE();

        val emailObj = db.emails[user.id]!!;
        val email = emailObj.inProcess!!;

        try {
            val mailMessage = email.toEmail(emailObj);
            if (mailMessage == null) {
                event.hook.sendMessage(messages.constants.cannotSendMailWithoutRecipients)
                    .setEphemeral(true).queue();
                return;
            }

            mailMessage.sendSync(emailObj.toMailer());

            // send to the announcement channel
            val embed = EmbedBuilder()
                .setAuthor(emailObj.emailName, null, user.effectiveAvatarUrl)
                .setTitle(email.subject)
                .setDescription(email.message)
                .build();
            
            val attachments = email.attachments.map { (name, path) ->
                FileUpload.fromData(File(path), name);
            };
            
            email.realGroups(emailObj).forEach { grp ->
                grp.channel?.toTextChannel()
                    ?.sendMessage(grp.role?.toRole()?.asMention ?: "")
                    ?.addEmbeds(embed)
                    ?.setFiles(attachments)
                    ?.setAllowedMentions(listOf(Message.MentionType.ROLE))
                    ?.complete();
            };

            // answer
            event.hook.sendMessage(messages.constants.sendMailSuccess).setEphemeral(true).queue();
        }
        catch (ex: MailException) {
            // answer negative + except
            except("010: $ex");
            event.hook.sendMessage(messages.constants.failureToSendMail).setEphemeral(true).queue();
        }
        // cleanup message
        cleanUserMailMessage(emailObj, user.id);
    }
    internal fun removeAddressFromGroupForeal(user: User, group: String, address: String, event: IReplyCallback) {
        event.dfrRplE();

        db.emails[user.id]!!.groups[group]!!.addresses.remove(address);
        saveDb();

        event.hook.sendMessage(messages.constants.removeAddressFromGroupSuccess).setEphemeral(true).queue();
    }
    internal fun promptMessageProcess(event: GenericComponentInteractionCreateEvent, metadataString: String, options: Collection<String>? = null) {
        event.dfrRpl();

        // check message id
        if (event.checkIfRightMessage()) return;
        // check if no other listener
        if (db.actionChannels.containsKey(event.user.id)) {
            // reply reject + delete after
            val currentBind = Pair("{what}", db.actionChannels[event.user.id]!!.second);
            event.hook.sendMessage(messages.constants.userAlreadyBinding.replVar(currentBind)).queueAndDel();
            return;
        }

        // send the prompt message
        val message = event.hook.sendMessageEmbeds(messages.mailEditing.toMessageEmbed(
            Pair("{action}", metadataString),
            Pair("%isOptions%", (options != null).toString()),
            Pair("{options}", options?.joinToString("\n") ?: "null")
        )).complete();

        // listen for the message
        db.actionChannels[event.user.id] = Triple(event.channel.id, metadataString, message.id);
        saveDb();
    }

    fun isWritingEmail(user: User) = user.emailObj.inProcess != null;
    fun canWriteEmail(user: User) = db.emails.containsKey(user.id);

    private fun cleanUserMailMessage(emailObj: ConfigModel.MemberEmail, userId: String) {
        emailObj.inProcessMessage?.toMessageS()?.tryDeleteComplete();

        Triple(db.actionChannels[userId]?.third ?: "", emailObj.inProcessMessage?.second ?: "", emailObj.inProcessMessage?.third ?: false)
            .toMessageS()?.tryDeleteComplete();

        // delete all attachments from disk
        emailObj.inProcess?.attachments?.forEach { (_, filename) ->
            try {
                Files.delete(Path.of(filename))
            } catch (_: Exception) {}
        };

        db.actionChannels.remove(userId);

        emailObj.inProcessMessage = null;
        emailObj.inProcess = null;

        saveDb();
    }
    private fun genericConfirmAction(user: User, event: IReplyCallback, action: String, yesId: String, otherAction: BooleanSupplier?) {
        event.dfrRpl();

        if (otherAction?.asBoolean == true) return;
        // check permissions
        if (!canWriteEmail(user)) {
            event.replyNoPerms();
            return;
        }
        // check if email in process
        if (!isWritingEmail(user)) {
            event.replyInvalidEmailAction(false, action);
            return;
        }


        // confirm
        sendConfirmation(action, yesId, event);
    }
    private fun <T : MessageCreateRequest<T>?> MessageCreateRequest<T>.setMailActionRow(emailObj: ConfigModel.MemberEmail): T {
        // What has to be there:
            // 1 - group select
            // 2 - subject, message, attachments
            // 3 - manual to add, manual to remove
            // 4 - send, discard
        return addActionRow(
            groupSelectMenu(emailObj)
        ).addActionRow(
            Button.primary("mailsubjectbtn", "Upravit předmět").withEmoji(Emoji.fromUnicode("✉\uFE0F")),
            Button.primary("mailmessagebtn", "Upravit zprávu").withEmoji(Emoji.fromUnicode("\uD83D\uDCDD")),
            Button.primary("mailattachmentbtn", "Nahrát přílohu").withEmoji(Emoji.fromUnicode("\uD83D\uDCC4")),
            Button.secondary("mailremoveattachmentbtn", "Odebrat přílohu").withEmoji(Emoji.fromUnicode("❌"))
        ).addActionRow(
            Button.secondary("mailaddrecbtn", "Přidat příjemce manuálně").withEmoji(Emoji.fromUnicode("\uD83D\uDC65")),
            Button.secondary("mailremrecbtn", "Odebrat příjemce manuálně").withEmoji(Emoji.fromUnicode("\uD83D\uDE45")),
            Button.secondary("mailshowrecbtn", "Zobrazit všechny příjemce").withEmoji(Emoji.fromUnicode("\uD83D\uDC40"))
        ).addActionRow(
            Button.success("mailsendbtn", "Odeslat zprávu").withEmoji(Emoji.fromUnicode("\uD83D\uDCE8")),
            Button.danger("maildiscardbtn", "Zrušit zprávu").withEmoji(Emoji.fromUnicode("\uD83D\uDD1A"))
        );
    }

    internal fun IReplyCallback.replyNoPerms(ephemeral: Boolean = false) {
        hook.sendMessageEmbeds(messages.noPerms.toMessageEmbed(
            Pair("{action}", messages.constants.mailSending)
        )).setEphemeral(ephemeral).queue();
    }
    internal fun IReplyCallback.replyInvalidEmailAction(inProgress: Boolean = false, action: String, group: Boolean = false, subject: String? = null, ephemeral: Boolean = false) {
        hook.sendMessageEmbeds(messages.invalidEmailAction.toMessageEmbed(
            Pair("{action}", action),
            Pair("%inProcess%", inProgress.toString()),
            Pair("%group%", group.toString()),
            Pair("{subject}", subject ?: "null")
        )).setEphemeral(ephemeral).queue();
    }
    internal fun Message.replySuccessfullyEdited() {
        reply(messages.constants.emailSuccessfullyEdited).queueAndDel();
    }
    internal fun User.emailGroup(name: String) = emailObj.groups[name]!!;
    internal fun String.inGroup(group: String, user: User) = user.emailGroup(group).addresses.contains(this);
    internal fun GenericComponentInteractionCreateEvent.checkIfRightMessage(): Boolean {
        if (messageId != user.emailObj.inProcessMessage?.first) {
            // reply reject + try delete
            hook.sendMessage(messages.constants.invalidMessage).queue();

            user.emailObj.inProcessMessage?.toMessageS()?.tryDeleteComplete();
            return true;
        }
        return false;
    };
    internal val User.emailObj: ConfigModel.MemberEmail get() {
        return db.emails[id]!!;
    };

    internal fun refreshInProcessMessage(user: User, event: IReplyCallback? = null) {
        val emailObj = user.emailObj;
        try {
            if (event != null)
                event.hook.editOriginalEmbeds(emailViewMessage(emailObj, user)).complete();
            else
                emailObj.inProcessMessage?.toMessageS()?.editMessageEmbeds(emailViewMessage(emailObj, user))?.complete();
        }
        catch (_: ErrorResponseException) {}
    }
    fun refreshAllMessages() {
        db.emails.filter { it.value.inProcessMessage != null }
            .forEach { (id, _) -> refreshInProcessMessage(id.toUser()); }
    }
    internal fun groupChoices(user: User, focusedOption: String) =
        user.emailObj.groups
            .map { it.key; }
            .filter { it.startsWith(focusedOption); }
            .map { Command.Choice(it, it); };
    private fun groupSelectMenu(emailObj: ConfigModel.MemberEmail) =
        StringSelectMenu.create("mailgroupslc").addOptions(emailObj.groups.map { SelectOption.of(it.key, it.key) }).build()
    internal fun emailViewMessage(emailObj: ConfigModel.MemberEmail, user: User): MessageEmbed {
        val mail = emailObj.inProcess!!;

        return messages.emailTemplate.toMessageEmbed(
            Pair("{subject}", mail.subject),
            Pair("{to}", (mail.groups.map { "__${it}__" } + mail.recipients).joinToString("; ")),
            Pair("{remFromGroups}", mail.removedRecipients.joinToString("; ")),
            Pair("{message}", mail.message),
            Pair("{from}", emailObj.address),
            Pair("{fromIcon}", user.effectiveAvatarUrl),
            Pair("{attachments}", mail.attachments.keys.joinToString(", ") { "`[$it]`" })
        );
    }
}