@file:Suppress("DuplicatedCode")

package codes.rorak.hamley.bot.administration

import codes.rorak.hamley.bot
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.ItemComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import org.jetbrains.annotations.Contract
import java.security.InvalidParameterException
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit
import java.util.function.BooleanSupplier

object Administration {
    private fun checkPermissions(
        author: Member,
        addPermission: Role? = null,
        action: String,
        event: IReplyCallback?
    ): Boolean {
        if (!(author.roles.contains(config.administration.perms.toRole()) || (addPermission != null && author.roles.contains(
                addPermission
            )) || author.roles.contains(
                config.botOwnerRole.toRole()
            )
                    )
        ) {
            if (event == null) throw PermissionException("No permissions!");
            event.hook.sendMessageEmbeds(
                messages.noPerms.toMessageEmbed(
                    Pair("{action}", action)
                )
            ).setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private fun Member.canPunish(target: Member, action: String, event: IReplyCallback?): Boolean {
        if (!canInteract(target)) {
            if (event == null) throw PermissionException("No permissions!");
            event.hook.sendMessageEmbeds(
                messages.noPerms.toMessageEmbed(
                    Pair("{action}", action)
                )
            ).setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    private fun memberValid(member: Member?, event: IReplyCallback?, ifFalse: BooleanSupplier? = null): Boolean {
        if (member == null) {
            if (ifFalse == null || ifFalse.asBoolean) {
                if (event == null) throw InvalidParameterException("Invalid member!");
                event.hook.sendMessage(messages.constants.invalidMember)
                    .setEphemeral(true)
                    .queue();
            }
            return false;
        }
        return true;
    }

    private fun getRealDuration(duration: String, event: IReplyCallback?): OffsetDateTime? {
        return try {
            duration.toDuration();
        } catch (ex: IllegalStateException) {
            if (event == null) throw ex;
            event.hook.sendMessageEmbeds(
                messages.wrongDurationFormat.toMessageEmbed(
                    Pair("{strtime}", duration),
                    Pair("%isShort%", "false")
                )
            )
                .setEphemeral(true)
                .queue();
            OffsetDateTime.MIN;
        }
    }

    private fun getRealTimeFrame(timeFrame: String, event: IReplyCallback?): Pair<Int, TimeUnit>? {
        return try {
            timeFrame.toIntTimeUnit();
        } catch (ex: IllegalStateException) {
            if (event == null) throw ex;
            event.hook.sendMessageEmbeds(
                messages.wrongDurationFormat.toMessageEmbed(
                    Pair("{strtime}", timeFrame),
                    Pair("%isShort%", "true")
                )
            )
                .setEphemeral(true)
                .queue();
            Pair(-1, TimeUnit.SECONDS);
        }
    }

    private fun sendPrivateMessage(
        user: User,
        author: Member,
        reason: String,
        duration: OffsetDateTime?,
        actionName: String
    ): Boolean {
        return try {
            user.openPrivateChannel().complete().sendMessageEmbeds(
                messages.administrationPrivate.toMessageEmbed(
                    Pair("{action}", actionName),
                    Pair("{server}", bot.guild.name),
                    Pair("{author}", author.asMention),
                    Pair("{reason}", reason),
                    Pair("%isTime%", (duration != OffsetDateTime.MIN).toString()),
                    Pair("{time}", if (duration != null) Config.DATE_TIME_FORMATTER.format(duration) else "navždy")
                )
            ).complete();
            true;
        } catch (_: ErrorResponseException) {
            false;
        } catch (ex: UnsupportedOperationException) {
            warn("002: Unsupported operation: ${ex.message}")
            false;
        }
    }

    private fun sendGlobalMessage(
        username: String,
        userMention: String?,
        author: Member,
        reason: String,
        duration: OffsetDateTime?,
        actionName: String,
        components: Collection<ItemComponent> = emptyList()
    ): Message {
        return config.administration.channel.toTextChannel().sendMessageEmbeds(
            messages.administration.toMessageEmbed(
                Pair("{userName}", username),
                Pair("{action}", actionName),
                Pair("{userMention}", userMention ?: username),
                Pair("{authorMention}", author.asMention),
                Pair("{reason}", reason),
                Pair("%isTime%", (duration != OffsetDateTime.MIN).toString()),
                Pair("{time}", if (duration != null) Config.DATE_TIME_FORMATTER.format(duration) else "navždy")
            )
        ).setAllowedMentions(listOf(Message.MentionType.USER)).also {
            if (components.isNotEmpty()) it.setActionRow(components)
        }.complete();
    }

    private fun IReplyCallback.replySuccess(usernameOrMention: String, actionName: String) {
        hook.sendMessageEmbeds(
            messages.administrationSuccessful.toMessageEmbed(
                Pair("{action}", actionName),
                Pair("{user}", usernameOrMention)
            )
        )
            .setEphemeral(true)
            .setAllowedMentions(listOf(Message.MentionType.USER))
            .queue();
    }

    fun warnMember(member: Member?, author: Member, reason: String?, event: IReplyCallback? = null) {
        event?.dfrRplE();

        if (!checkPermissions(author, config.administration.warn.perms?.toRole(), "/warn", event))
            return;

        val actionName = config.administration.warn.actionName;
        val realReason = reason ?: config.administration.warn.defaultReason;

        // check member not null
        if (!memberValid(member, event))
            return;
        member!!;

        // check author can punish member
        if (!author.canPunish(member, "/warn", event))
            return;

        // send info messages
        sendPrivateMessage(member.user, author, realReason, OffsetDateTime.MIN, actionName);
        sendGlobalMessage(
            member.effectiveName, member.asMention, author, realReason, OffsetDateTime.MIN,
            actionName
        );

        // reply success
        event?.replySuccess(member.asMention, actionName);
    }


    fun muteMember(member: Member?, author: Member, duration: String?, reason: String?, event: IReplyCallback? = null) {
        event?.dfrRplE();

        if (!checkPermissions(author, config.administration.mute.perms?.toRole(), "/mute", event))
            return;

        val actionName = config.administration.mute.actionName;
        val realReason = reason ?: config.administration.mute.defaultReason;
        val realDuration = getRealDuration(duration ?: config.administration.mute.defaultTime, event);

        // check member not null
        if (!memberValid(member, event))
            return;
        member!!;

        // check author can punish member
        if (!author.canPunish(member, "/mute", event))
            return;

        // check valid duration
        if (realDuration == OffsetDateTime.MIN)
            return;

        // check if muted
        if (isMuted(member)) {
            if (event == null) throw IllegalStateException("Member is already muted!");
            event.hook.sendMessageEmbeds(
                messages.memberAlreadyMuted.toMessageEmbed(
                    Pair("{userName}", member.effectiveName),
                    Pair("{userMention}", member.asMention),
                    Pair("{time}", db.mutelist[member.id]!!.unmute?.toString() ?: "nikdy")
                )
            )
                .setEphemeral(true)
                .setAllowedMentions(listOf(Message.MentionType.USER))
                .queue();
            return;
        }

        // mute
        val hadRoles = justMute(member, realReason, true)!!;

        // send info messages
        sendPrivateMessage(member.user, author, realReason, realDuration, actionName);
        val message = sendGlobalMessage(
            member.effectiveName, member.asMention, author, realReason, realDuration, actionName,
            listOf(Button.success("unmutebtn", "Unmute").withEmoji(Emoji.fromUnicode("\uD83D\uDD0A")))
        );

        // add to mutelist
        db.mutelist[member.id] =
            ConfigModel.MutelistObj(realDuration, hadRoles.map(Role::getIdLong).toTypedArray(), message.idLong);
        saveDb();

        // reply success
        event?.replySuccess(member.asMention, actionName);
    }

    fun kickMember(member: Member?, author: Member, reason: String?, event: IReplyCallback? = null) {
        event?.dfrRplE();

        if (!checkPermissions(author, config.administration.kick.perms?.toRole(), "/kick", event))
            return;

        val actionName = config.administration.kick.actionName;
        val realReason = reason ?: config.administration.kick.defaultReason;

        // check member not null
        if (!memberValid(member, event))
            return;
        member!!;

        // check author can punish member
        if (!author.canPunish(member, "/kick", event))
            return;

        // send info messages (first!)
        sendPrivateMessage(member.user, author, realReason, OffsetDateTime.MIN, actionName);
        val message = sendGlobalMessage(
            member.effectiveName,
            member.asMention,
            author,
            realReason,
            OffsetDateTime.MIN,
            actionName
        );

        // add to kicklist
        db.kicklist[member.id] = message.id;
        saveDb();
        // kick
        member.kick().reason(realReason).complete();

        // reply success
        event?.replySuccess(member.asMention, actionName);
    }

    fun banMember(
        member: Member?,
        author: Member,
        duration: String?,
        reason: String?,
        deletionTimeFrame: String?,
        event: IReplyCallback? = null
    ) {
        event?.dfrRplE();

        if (!checkPermissions(author, config.administration.ban.perms?.toRole(), "/ban", event))
            return;

        val actionName = config.administration.ban.actionName;
        val realReason = reason ?: config.administration.ban.defaultReason;

        val realDuration = getRealDuration(duration ?: config.administration.ban.defaultTime, event);
        // check valid duration
        if (realDuration == OffsetDateTime.MIN)
            return;

        val realDeletionTimeFrame = getRealTimeFrame(deletionTimeFrame ?: "never", event) ?: Pair(0, TimeUnit.SECONDS);
        // check valid time frame
        if (realDeletionTimeFrame.first == -1)
            return;

        // check member not null
        if (!memberValid(member, event))
            return;
        member!!;

        // check author can punish member
        if (!author.canPunish(member, "/ban", event))
            return;

        // send info messages
        sendPrivateMessage(member.user, author, realReason, realDuration, actionName);
        val message = sendGlobalMessage(
            member.effectiveName, member.asMention, author, realReason, realDuration, actionName,
            listOf(Button.success("unbanbtn", "Unban").withEmoji(Emoji.fromUnicode("\uD83D\uDD28")))
        );

        // add to banlist
        db.banlist[member.id] = ConfigModel.BanlistObj(realDuration, message.idLong);
        saveDb();
        // ban
        member.ban(realDeletionTimeFrame.first, realDeletionTimeFrame.second).reason(realReason).complete();

        // reply success
        event?.replySuccess(member.asMention, actionName);
    }

    fun unmuteMember(
        member: Member?,
        author: Member,
        reason: String?,
        event: IReplyCallback? = null,
        memberId: String? = null
    ) {
        event?.dfrRplE();

        if (!checkPermissions(author, config.administration.unmute.perms?.toRole(), "/unmute", event))
            return;

        val actionName = config.administration.unmute.actionName;
        val realReason = reason ?: config.administration.unmute.defaultReason;

        // check member not null
        if (!memberValid(member, event) {
                if (!db.mutelist.containsKey(memberId)) return@memberValid true;
                val user = memberId!!.toUser();
                val userMuteObj = db.mutelist[user.id]!!;
                // send info messages
                sendPrivateMessage(user, author, realReason, OffsetDateTime.MIN, actionName);
                sendGlobalMessage(
                    user.effectiveName,
                    user.asMention,
                    author,
                    realReason,
                    OffsetDateTime.MIN,
                    actionName
                );

                // edit mute message
                retrieveEditOrWarn(
                    userMuteObj.muteMessageId,
                    messages.constants.alreadyUnmuted,
                    config.administration.channel
                );
                // remove from mutelist
                db.mutelist.remove(user.id);
                saveDb();

                // reply success
                event?.replySuccess(user.asMention, actionName);

                false;
            })
            return;
        member!!;

        // check author can punish member
        if (!author.canPunish(member, "/unmute", event))
            return;

        // check member really muted
        if (!isMuted(member)) {
            if (event == null) throw IllegalStateException("Member is not muted!");
            event.hook.sendMessageEmbeds(
                messages.memberNotAdminActioned.toMessageEmbed(
                    Pair("{userName}", member.effectiveName),
                    Pair("{oppositeAction}", config.administration.mute.actionName),
                    Pair("{userMention}", member.asMention),
                    Pair("{action}", actionName)
                )
            )
                .setEphemeral(true)
                .setAllowedMentions(listOf(Message.MentionType.USER))
                .queue();
            return;
        }

        // unmute
        val memberMuteObj = db.mutelist[member.id]!!;
        val addRoles = memberMuteObj.roles.map(Long::toRole);
        val removeRoles = config.administration.mute.addRoles.map(Long::toRole);

        bot.guild.modifyMemberRoles(member, addRoles, removeRoles).reason(realReason).complete();

        // send info messages
        sendPrivateMessage(member.user, author, realReason, OffsetDateTime.MIN, actionName);
        sendGlobalMessage(member.effectiveName, member.asMention, author, realReason, OffsetDateTime.MIN, actionName);

        // edit mute message
        retrieveEditOrWarn(
            memberMuteObj.muteMessageId,
            messages.constants.alreadyUnmuted,
            config.administration.channel
        );
        // remove from mutelist
        db.mutelist.remove(member.id);
        saveDb();

        // reply success
        event?.replySuccess(member.asMention, actionName);
    }

    fun unbanMember(username: String, author: Member, reason: String?, event: IReplyCallback? = null) {
        event?.dfrRplE();

        if (!checkPermissions(author, config.administration.unban.perms?.toRole(), "/unban", event))
            return;

        val actionName = config.administration.unban.actionName;
        val realReason = reason ?: config.administration.unban.defaultReason;

        // find and check the user
        val user = db.banlist.keys.map { it.toUser() }.find { it.name == username };
        if (user == null) {
            if (event == null) throw IllegalStateException("User $username is not banned!");
            event.hook.sendMessageEmbeds(
                messages.memberNotAdminActioned.toMessageEmbed(
                    Pair("{userName}", username),
                    Pair("{oppositeAction}", config.administration.ban.actionName),
                    Pair("{userMention}", username),
                    Pair("{action}", actionName)
                )
            )
                .setEphemeral(true)
                .setAllowedMentions(listOf(Message.MentionType.USER))
                .queue();
            return;
        }

        // send messages
        sendGlobalMessage(user.effectiveName, user.asMention, author, realReason, OffsetDateTime.MIN, actionName);

        // UNBAN
        val userBanObj = db.banlist[user.id]!!;
        // change ban message
        retrieveEditOrWarn(
            userBanObj.banMessageId,
            messages.constants.alreadyUnbanned,
            config.administration.channel
        );
        // unban user
        bot.guild.unban(user).reason(realReason).complete();
        // remove from banlist
        db.banlist.remove(user.id);
        saveDb();

        // reply success
        event?.replySuccess(user.asMention, actionName);
    }

    fun isMuted(member: Member): Boolean {
        return db.mutelist.containsKey(member.id);
    }

    @Contract("_, false -> null")
    fun justMute(member: Member, reason: String, ret: Boolean = false): Set<Role>? {
        val removeRoles = config.administration.mute.removeRoles.map(Long::toRole);
        val addRoles = config.administration.mute.addRoles.map(Long::toRole);
        val hadRoles = if (ret) removeRoles.intersect(member.roles.toSet()) else null;

        member.modifyRoles(addRoles, removeRoles, "Muted: $reason");

        return hadRoles;
    }

    internal fun IModalCallback.sendReasonModal(
        id: String,
        memberId: String,
        title: String,
        placeholder: String,
        defaultReason: String,
        required: Boolean = false
    ) {
        replyModal(
            Modal.create("$id-$memberId", title)
                .addActionRow(
                    TextInput.create("reason", "Důvod", TextInputStyle.PARAGRAPH)
                        .setRequired(required)
                        .setPlaceholder(placeholder)
                        .setValue(defaultReason)
                        .setMaxLength(100)
                        .build()
                )
                .build()
        ).queue();
    }
}