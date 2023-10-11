package codes.rorak.hamley.bot.mail

import codes.rorak.hamley.bot
import codes.rorak.hamley.bot.mail.Mail.emailObj
import codes.rorak.hamley.bot.mail.Mail.refreshInProcessMessage
import codes.rorak.hamley.bot.mail.Mail.replySuccessfullyEdited
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.io.File

object MailListener: ListenerAdapter() {
    override fun onMessageReceived(event: MessageReceivedEvent) {
        // check guild
        if (event.isFromGuild && event.guild != bot.guild) return;
        // get action (channel)
        val action = db.actionChannels[event.author.id] ?: return;
        // check channel
        if (event.isFromGuild && action.first != event.channel.id) return;

        val emailObj = event.author.emailObj;

        // delete prompt message
        Triple(action.third, emailObj.inProcessMessage?.second ?: "", emailObj.inProcessMessage?.third ?: false).toMessageS()?.tryDeleteComplete();
        // remove from action channels
        db.actionChannels.remove(event.author.id);
        // delete message
        try {
            event.message.delete().complete();
        } catch (_: Exception) {}

        // cancel handle
        if (event.message.contentRaw == "cancel")
            event.message.reply(messages.constants.emailEditCanceled).queueAndDel();
        else
            // according to action do:
            when (action.second) {
                messages.constants.subject -> onSubjectEnter(event.author, event.message);
                messages.constants.text -> onTextEnter(event.author, event.message);
                messages.constants.attachments -> onAttachmentsEnter(event.author, event.message);
                messages.constants.remAttachments -> onRemAttachmentsEnter(event.author, event.message);
                messages.constants.recipients -> onRecipientsEnter(event.author, event.message);
                messages.constants.remRecipients -> onRemRecipientsEnter(event.author, event.message);
                else -> except("001: Constants changed! Got ${action.second}, skipping...");
            }
    }

    private fun onSubjectEnter(user: User, message: Message) {
        user.emailObj.inProcess!!.subject = message.contentRaw;
        saveDb();

        refreshInProcessMessage(user);

        message.replySuccessfullyEdited();
    }
    private fun onTextEnter(user: User, message: Message) {
        user.emailObj.inProcess!!.message = message.contentRaw;
        saveDb();

        refreshInProcessMessage(user);

        message.replySuccessfullyEdited();
    }
    private fun onAttachmentsEnter(user: User, message: Message) {
        val msg = message.reply(messages.constants.attachmentLoading).complete();

        user.emailObj.inProcess!!.attachments.putAll(
            message.attachments.associate {
                val uf = Config.uniqueFile;

                if (it.width != -1 && it.height != -1)
                    it.proxy.downloadToFile(uf, it.width, it.height).complete(uf);
                else
                    it.proxy.downloadToFile(uf).complete(uf);

                Pair(it.fileName, uf.absolutePath);
            }
        );
        saveDb();

        refreshInProcessMessage(user);

        msg.editMessage(messages.constants.emailSuccessfullyEdited).queueAndDel();
    }
    private fun onRemAttachmentsEnter(user: User, message: Message) {
        val remAttachments = message.contentRaw.commaSplit().toSet();
        val email = user.emailObj.inProcess!!;

        email.attachments.forEach { (t, u) ->
            if (t in remAttachments)
                try { File(u).delete(); } catch (ex: Exception) { warn(ex.message ?: "no message"); };
        };

        email.attachments.keys.removeAll(message.contentRaw.commaSplit().toSet());
        saveDb();

        refreshInProcessMessage(user);

        message.replySuccessfullyEdited();
    }
    private fun onRecipientsEnter(user: User, message: Message) {
        val recipients = message.contentRaw.commaSplit().toMutableSet();
        val emailObj = user.emailObj;
        val inProcess = emailObj.inProcess!!;
        val spreadGroups = inProcess.groups.spreadGroups(emailObj);

        inProcess.removedRecipients.removeAll(recipients);

        // remove already existing and in groups recipients
        recipients.removeIf {
            if (spreadGroups.contains(it)) {
                message.reply(messages.constants.addressAlreadyInGroup.replVar(
                    Pair("{group}", inProcess.groups.find { it1 -> emailObj.groups[it1]!!.addresses.contains(it) }!!),
                    Pair("{address}", it)
                ));
                true;
            }
            else false;
        };

        // add them
        inProcess.recipients.addAll(recipients);
        saveDb();

        refreshInProcessMessage(user);

        message.replySuccessfullyEdited();
    }
    private fun onRemRecipientsEnter(user: User, message: Message) {
        val addresses = message.contentRaw.commaSplit().toSet();
        val mail = user.emailObj.inProcess!!;
        val inGroupOnly = addresses - (mail.recipients intersect addresses);

        mail.recipients.removeAll(addresses);
        user.emailObj.inProcess!!.removedRecipients.addAll(inGroupOnly);

        saveDb();

        refreshInProcessMessage(user);

        message.replySuccessfullyEdited();
    }

}