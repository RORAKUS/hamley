package codes.rorak.hamley.bot.moderation

import codes.rorak.hamley.bot
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.replVar
import codes.rorak.hamley.util.warn
import net.dv8tion.jda.api.events.guild.invite.GuildInviteCreateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object ModerationListener: ListenerAdapter() {
    override fun onGuildInviteCreate(event: GuildInviteCreateEvent) {
        if (event.guild != bot.guild) return;

        if (event.invite.inviter == bot.bot.selfUser) return;

        event.invite.delete().complete();
        try {
            event.invite.inviter!!.openPrivateChannel().complete()
                .sendMessage(messages.constants.noNewInvites.replVar(Pair("{invite}", config.moderation.invite)))
                .queue();
        }
        catch (ex: Throwable) {
            warn("006: User ${event.invite.inviter?.effectiveName} hasn't allowed private messages (or is just null :D)");
        }
    }
}