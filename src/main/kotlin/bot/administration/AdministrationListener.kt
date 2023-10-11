package codes.rorak.hamley.bot.administration

import codes.rorak.hamley.bot
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import codes.rorak.hamley.util.retrieveEditOrWarn
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button

object AdministrationListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild != bot.guild) return;
//        // remute
//        if (isMuted(event.member))
//            justMute(event.member, "Re-join auto-mute");

        // handle kicklist
        val kickMessageId = db.kicklist[event.member.id];
        if (kickMessageId != null) {
            // edit message
            retrieveEditOrWarn(kickMessageId, messages.constants.alreadyJoined, config.administration.channel)
                ?.editMessageComponents(
                    ActionRow.of(
                        Button.success("approvekickbtn", "Approve & Verify").withEmoji(Emoji.fromUnicode("✅")),
                        Button.danger("kickagainbtn", "Kick Again").withEmoji(Emoji.fromUnicode("\uD83E\uDDB6"))
                    )
                )
                ?.complete()?.let { msg ->
                    // add to kickMessages
                    db.kickMessages[event.member.id] = msg.id;
                };
            // remove from kicklist
            db.kicklist.remove(event.member.id);
            saveDb();
        }
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        if (event.guild != bot.guild) return;
        // change kick info message
        if (db.kickMessages.contains(event.user.id)) {
            retrieveEditOrWarn(db.kickMessages[event.user.id]!!, messages.constants.userAlreadyLeft, config.administration.channel, 1);
            db.kickMessages.remove(event.user.id);
            saveDb();
        }
    }
}