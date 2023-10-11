package codes.rorak.hamley.bot.automod

import codes.rorak.hamley.bot
import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.Option
import codes.rorak.hamley.handlerfactories.annotations.SlashCommand
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object AutomodCommands : InteractionListener {
    @SlashCommand("Verifikuje uživatele",true)
    fun verify(
        @Option("Uživatel, kterého verifikovat", true) user: Member?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        val targetUser = event.getOption("user")!!.asUser

        Automod.handleVerification(event, user, event.member, targetUser);
    }
}