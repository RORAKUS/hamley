package codes.rorak.hamley.bot.automod

import codes.rorak.hamley.bot
import codes.rorak.hamley.bot.automod.Automod.handleVerification
import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.OnButtonPress
import codes.rorak.hamley.handlerfactories.annotations.UserContext
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.getKey
import codes.rorak.hamley.util.toMemberOrNull
import codes.rorak.hamley.util.toUser
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object AutomodInteractions : InteractionListener {
    @UserContext("Verify", [Permission.MANAGE_ROLES],true)
    fun userVerifyContext(event: UserContextInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        handleVerification(event, event.targetMember, event.member, event.target);
    }
    @OnButtonPress
    fun vrfbtn(event: ButtonInteractionEvent) {
        if (!event.isFromGuild || event.guild != bot.guild) return;

        val id = db.infoMessages.getKey(event.messageId) ?: return;
        val target = id.toMemberOrNull();
        val user = id.toUser();

        handleVerification(event, target, event.member, user, event.message);
    }
}