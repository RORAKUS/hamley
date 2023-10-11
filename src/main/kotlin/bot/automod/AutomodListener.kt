package codes.rorak.hamley.bot.automod

import codes.rorak.hamley.bot
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction
import java.util.function.BiFunction

object AutomodListener : ListenerAdapter() {
    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (event.guild != bot.guild) return;
        // add auto roles
        for (role in config.automod.autoRoles)
            event.guild.addRoleToMember(event.member, role.toRole()).queue();

        // send a message
        val channel = config.automod.infoChannel.toTextChannel();

        val msg = messages.memberJoin.toMessageEmbed(
            Pair("{userName}", event.user.name),
            Pair("{userMention}", event.member.asMention),
            Pair("{adminrole}", config.automod.verifyPerms.toRole().asMention)
        );
        channel.sendMessageEmbeds(msg)
            .setAllowedMentions(listOf(MentionType.USER, MentionType.ROLE))
            .addActionRow(Button.success("vrfbtn", "Verify").withEmoji(Emoji.fromUnicode("✅")))
            .queue {
                db.infoMessages[event.member.id] = it.id;
                saveDb();
            };
        // send ping and then delete
        channel.sendMessage(config.automod.verifyPerms.toRole().asMention)
            .setAllowedMentions(listOf(MentionType.ROLE))
            .queue {
                it.delete().queue();
            };
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        if (event.guild != bot.guild) return;

        // modify verification message
        if (db.infoMessages.contains(event.user.id)) {
            retrieveEditOrWarn(
                db.infoMessages[event.user.id]!!,
                messages.constants.userAlreadyLeft,
                config.automod.infoChannel
            );
            db.infoMessages.remove(event.user.id);
            saveDb();
        }

        // send a message if not kick nor banned
        if (!db.kicklist.containsKey(event.user.id) && !db.banlist.containsKey(event.user.id)) {
            val channel = config.automod.infoChannel.toTextChannel();

            val msg = messages.memberLeave.toMessageEmbed(
                Pair("{user}", event.user.name),
                Pair("{userMention}", event.user.asMention)
            );
            channel.sendMessageEmbeds(msg).queue();
        }

        // send message to all channel
        config.automod.allInfoChannel.toTextChannel()
            .sendMessage(messages.constants.leaveInfo.replVar(Pair("{userMention}", event.user.asMention)))
            .setAllowedMentions(listOf(MentionType.USER)).queue();
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) = reactionChange(event, bot.guild::addRoleToMember);
    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) = reactionChange(event, bot.guild::removeRoleFromMember);


    private fun reactionChange(event: GenericMessageReactionEvent, roleChangeFunction: BiFunction<UserSnowflake, Role, AuditableRestAction<Void>>) {
        if (!event.isFromGuild || event.guild != bot.guild) return;
        if (event.user?.isBot == true) return;

        val roleSelectObj = db.roleSelects.find { it.messageId == event.messageIdLong && it.channel == event.channel.idLong } ?: return;
        val role = roleSelectObj.roles.entries.find { it.value == event.reaction.emoji.name }?.key ?: return;
        roleChangeFunction.apply(event.retrieveMember().complete(), role.toRole()).reason("Autorole select").queue();
    }
}