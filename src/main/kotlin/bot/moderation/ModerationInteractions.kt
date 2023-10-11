package codes.rorak.hamley.bot.moderation

import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.OnButtonPress
import codes.rorak.hamley.util.tryDeleteComplete
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

object ModerationInteractions: InteractionListener {
    @OnButtonPress
    fun selfdeletebtn(event: ButtonInteractionEvent) {
        // safe delete self message
        event.message.tryDeleteComplete();
        event.deferEdit().queue();
    }
}