package codes.rorak.hamley.bot.administration

import codes.rorak.hamley.bot
import codes.rorak.hamley.bot.administration.Administration.kickMember
import codes.rorak.hamley.bot.administration.Administration.sendReasonModal
import codes.rorak.hamley.bot.administration.Administration.unbanMember
import codes.rorak.hamley.bot.administration.Administration.unmuteMember
import codes.rorak.hamley.bot.automod.Automod.handleVerification
import codes.rorak.hamley.handlerfactories.Comparator
import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.OnButtonPress
import codes.rorak.hamley.handlerfactories.annotations.OnModalSubmit
import codes.rorak.hamley.handlerfactories.annotations.UserContext
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException

object AdministrationInteractions : InteractionListener {
    @UserContext("Unban", [Permission.BAN_MEMBERS])
    fun unbanUserContext(event: UserContextInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        event.sendReasonModal(
            "unbanmdl",
            event.target.id,
            "Unban User",
            "Důvod proč odebrat ban uživateli",
            config.administration.unban.defaultReason
        );
    }

    @OnButtonPress
    fun unbanbtn(event: ButtonInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        val memberId = db.banlist.entries.find { it.value.banMessageId == event.messageIdLong }?.key
            ?: throw Exception("Error 2670");

        event.sendReasonModal(
            "unbanmdl",
            memberId,
            "Unban User",
            "Důvod proč odebrat ban uživateli",
            config.administration.unban.defaultReason
        );
    }

    @OnModalSubmit(comparator = Comparator.STARTS_WITH)
    fun unbanmdl(event: ModalInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        val id = event.modalId.split("-")[1];

        unbanMember(id.toUser().name, event.member!!, event.getValue("reason")?.asString, event);
    }

    @OnButtonPress
    fun unmutebtn(event: ButtonInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        val memberId = db.mutelist.entries.find { it.value.muteMessageId == event.messageIdLong }?.key
            ?: throw Exception("Error 2669");

        event.sendReasonModal(
            "unmutemdl",
            memberId,
            "Unmute Member",
            "Důvod proč odebrat umlčení uživateli",
            config.administration.unmute.defaultReason
        );
    }

    @OnModalSubmit(comparator = Comparator.STARTS_WITH)
    fun unmutemdl(event: ModalInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        val id = event.modalId.split("-")[1];

        unmuteMember(id.toMemberOrNull(), event.member!!, event.getValue("reason")?.asString, event, id);
    }

    @OnButtonPress
    fun approvekickbtn(event: ButtonInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        val id = db.kickMessages.getKey(event.messageId) ?: return;
        val target = id.toMemberOrNull();
        val user = id.toUser();

        handleVerification(event, target, event.member!!, user);

        // edit message
        try {
            event.message.editAndAppend(messages.constants.alreadyJoinedOK, 1);
        } catch (ex: ErrorResponseException) {
            warn("003: ${ex.meaning}: message ${event.messageId} was not found!");
        }
        // remove from kick messages
        db.kickMessages.remove(id);
        saveDb();
    }

    @OnButtonPress
    fun kickagainbtn(event: ButtonInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        val id = db.kickMessages.getKey(event.messageId) ?: return;
        val target = id.toMemberOrNull();

        if (target != null)
            kickMember(target, event.member!!, "Automatické znovu-vyhození", event);

        // edit the message
        try {
            event.message.editAndAppend(messages.constants.successfullyRekicked, 1);
        }
        catch (ex: ErrorResponseException) {
            warn("004: ${ex.meaning}: message ${event.message.id} was not found!");
        }
        // remove from kickMessages
        db.kickMessages.remove(id);
        saveDb();
    }
}