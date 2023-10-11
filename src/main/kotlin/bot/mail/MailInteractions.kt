package codes.rorak.hamley.bot.mail

import codes.rorak.hamley.bot.mail.Mail.checkIfRightMessage
import codes.rorak.hamley.bot.mail.Mail.emailGroup
import codes.rorak.hamley.bot.mail.Mail.emailObj
import codes.rorak.hamley.bot.mail.Mail.promptMessageProcess
import codes.rorak.hamley.bot.mail.Mail.refreshInProcessMessage
import codes.rorak.hamley.bot.mail.Mail.removeAddressFromGroupForeal
import codes.rorak.hamley.bot.mail.Mail.sendMailForeal
import codes.rorak.hamley.bot.mail.Mail.sendMailProcess
import codes.rorak.hamley.bot.mail.Mail.stopMailForeal
import codes.rorak.hamley.bot.mail.Mail.stopMailProcess
import codes.rorak.hamley.handlerfactories.Comparator
import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.OnButtonPress
import codes.rorak.hamley.handlerfactories.annotations.OnStringSelect
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
object MailInteractions: InteractionListener {

    // region Email change
    @OnStringSelect
    fun mailgroupslc(event: StringSelectInteractionEvent) {
        event.deferEdit().queue();

        if (event.checkIfRightMessage()) return;

        val emailObj = event.user.emailObj;
        val email = emailObj.inProcess!!;

        event.values.forEach {
            if (email.groups.contains(it)) {
                email.groups.remove(it);

                val group = event.user.emailGroup(it);
                email.removedRecipients.removeIf { rc -> rc in group.addresses };
            }
            else email.groups.add(it);
        };

        saveDb();
        refreshInProcessMessage(event.user, event);
    }

    @OnButtonPress
    fun mailsubjectbtn(event: ButtonInteractionEvent) =
        promptMessageProcess(event, messages.constants.subject);
    @OnButtonPress
    fun mailmessagebtn(event: ButtonInteractionEvent) =
        promptMessageProcess(event, messages.constants.text);
    @OnButtonPress
    fun mailattachmentbtn(event: ButtonInteractionEvent) =
        promptMessageProcess(event, messages.constants.attachments);
    @OnButtonPress
    fun mailremoveattachmentbtn(event: ButtonInteractionEvent) =
        promptMessageProcess(event, messages.constants.remAttachments, event.user.emailObj.inProcess!!.attachments.keys);
    @OnButtonPress
    fun mailaddrecbtn(event: ButtonInteractionEvent) =
        promptMessageProcess(event, messages.constants.recipients);
    @OnButtonPress
    fun mailremrecbtn(event: ButtonInteractionEvent) = with (event.user.emailObj) {
        promptMessageProcess(event, messages.constants.remRecipients, inProcess!!.getAllAddresses(this) { "====== $it ======" });
    }
    // endregion

    // region Email actions
    @OnButtonPress
    fun mailshowrecbtn(event: ButtonInteractionEvent) = with (event.user.emailObj) {
        event.reply(inProcess!!.getAllAddresses(this) { "====== $it ======" }.joinToString("\n"))
            .setEphemeral(true)
            .queue();
    }
    @OnButtonPress
    fun mailsendbtn(event: ButtonInteractionEvent) {
        sendMailProcess(event.user, event) {
            event.checkIfRightMessage()
        };
    };
    @OnButtonPress
    fun maildiscardbtn(event: ButtonInteractionEvent) {
        stopMailProcess(event.user, event) {
            event.checkIfRightMessage();
        };
    };

    // endregion

    // region Confirmations
    @OnButtonPress
    fun mailconfirmstop(event: ButtonInteractionEvent) {
        stopMailForeal(event.user, event);

        event.message.tryDeleteComplete();
    }

    @OnButtonPress
    fun mailconfirmsend(event: ButtonInteractionEvent) {
        sendMailForeal(event.user, event);

        event.message.tryDeleteComplete();
    }

    @OnButtonPress(comparator = Comparator.STARTS_WITH)
    fun mailconfirmremovefromgroup(event: ButtonInteractionEvent) {
        val args = event.args;
        removeAddressFromGroupForeal(event.user, args[0], args[1], event);

        event.message.tryDeleteComplete();
    }
    // endregion
}


