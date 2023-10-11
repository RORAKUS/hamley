package codes.rorak.hamley.bot.automod

import codes.rorak.hamley.bot
import codes.rorak.hamley.bot.administration.Administration
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.Config.saveDb
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

object Automod {
    private fun verify(member: Member) {
        val addRoles = config.automod.verifyRoles.map(Long::toRole);
        val removeRoles = config.automod.autoRoles.map(Long::toRole);

        member.modifyRoles(addRoles, removeRoles, "Verification");
    }

    fun loadRoleSelects() {
        db.roleSelects.forEach { rs ->
            val embed = rs.message.toMessageEmbed(append = "\n\n" + rs.roles.entries.joinToString("\n") { o -> "${o.value} - ${o.key.toRole().asMention}" });

            // send or get the message
            val msg =
                if (rs.messageId == null) rs.channel.toTextChannel().sendMessageEmbeds(embed).complete();
                else Pair(rs.messageId!!, rs.channel).toMessage()?.apply { editMessageEmbeds(embed).queue() };

            // set reactions
            rs.roles.forEach { r ->
                msg?.addReaction(Emoji.fromUnicode(r.value))?.queue();
            };

            rs.messageId = msg?.idLong;

            // check roles
            msg?.reactions?.forEach { r ->
                val role = rs.roles.entries.find { it.value == r.emoji.name }?.key?.toRole() ?: return;
                val reactionUsers = r.retrieveUsers().complete().filterNot { it.isBot }.map(User::getId);
                bot.guild.findMembersWithRoles(role).onSuccess { usersWithRole ->
                    // if IN role AND NOT IN reaction --> remove role
                    usersWithRole.filter { it.id !in reactionUsers }
                        .forEach { bot.guild.removeRoleFromMember(it, role).reason("Autorole post-select").queue(); };
                    // if IN reaction AND NOT IN role --> add role
                    reactionUsers.filter { it !in usersWithRole.map(Member::getId) }
                        .forEach { bot.guild.addRoleToMember(UserSnowflake.fromId(it), role).reason("Autorole post-select").queue(); };
                };
            };
        };
        saveDb();
    }


    fun handleVerification(
        event: IReplyCallback,
        target: Member?,
        sender: Member?,
        targetUser: User,
        infoMessage: Message? = null
    ) {
        event.dfrRplE();

        if (!(canVerify(sender) ?: return)) {
            sendNoPermsResponse(event);
            return;
        }
        if (target == null) {
            sendAlreadyLeftResponse(event, targetUser);
            return;
        }
        if (isVerified(target)) {
            sendAlreadyVerifiedResponse(event, target);
            return;
        }

        try {
            verify(target);
        } catch (e: Throwable) {
            if (e is ErrorResponseException) sendAlreadyLeftResponse(event, targetUser);
            else throw e;
        }

        // handle if muted
        if (Administration.isMuted(target))
            Administration.justMute(target, "Verification auto-mute");

        if (db.infoMessages.contains(target.id))
            try {
                val message = infoMessage ?: config.automod.infoChannel.toTextChannel().retrieveMessageById(
                    db.infoMessages[target.id]!!
                ).complete();
                message.editAndAppend(messages.constants.alreadyVerified);
            } catch (ex: ErrorResponseException) {
                warn("005: ${ex.meaning}: message ${db.infoMessages[target.id]} or channel ${config.automod.infoChannel} was not found!");
                return;
            } finally {
                db.infoMessages.remove(target.id);
                saveDb();
            }

        // delete all messages in waitlist
        config.automod.waitlistChannel.toTextChannel().history.retrievePast(100).complete().forEach {
            if (it.author == targetUser) it.delete().queue();
        }

        sendSuccessResponse(event, target);
        target.user.openPrivateChannel().complete()
            .sendMessageEmbeds(messages.userWelcomeMessage.toMessageEmbed())
            .setAllowedMentions(listOf(Message.MentionType.CHANNEL))
            .queue();

        sendAllInfoJoin(target);
    }

    private fun isVerified(member: Member): Boolean = isVerified(member as Member?)!!;
    private fun isVerified(member: Member?): Boolean? =
        member?.roles?.contains(config.automod.userNotVerifiedRole.toRole())?.not();

    private fun canVerify(author: Member?) = author?.roles?.contains(config.automod.verifyPerms.toRole());

    private fun sendSuccessResponse(event: IReplyCallback, member: Member) {
        event.hook.sendMessageEmbeds(
            messages.verificationSuccessful.toMessageEmbed(
                Pair("{userMention}", member.asMention)
            )
        )
            .setEphemeral(true)
            .setAllowedMentions(listOf(Message.MentionType.USER))
            .queue();
    }

    private fun sendAlreadyLeftResponse(event: IReplyCallback, user: User) {
        event.hook.sendMessageEmbeds(messages.userAlreadyLeft.toMessageEmbed(Pair("{user}", user.name)))
            .setEphemeral(true)
            .queue();
    }

    private fun sendAlreadyVerifiedResponse(event: IReplyCallback, member: Member) {
        event.hook.sendMessageEmbeds(
            messages.userAlreadyVerified.toMessageEmbed(
                Pair("{userMention}", member.asMention)
            )
        )
            .setEphemeral(true)
            .setAllowedMentions(listOf(Message.MentionType.USER))
            .queue();
    }

    private fun sendNoPermsResponse(event: IReplyCallback) {
        event.hook.sendMessageEmbeds(
            messages.noPerms.toMessageEmbed(
                Pair("{action}", messages.constants.userVerification)
            )
        )
            .setEphemeral(true)
            .queue();
    }

    private fun sendAllInfoJoin(member: Member) {
        val message = if (member.flags.contains(Member.MemberFlag.DID_REJOIN)) messages.constants.joinAgainInfo else messages.constants.joinInfo;

        config.automod.allInfoChannel.toTextChannel()
            .sendMessage(message.replVar(Pair("{userMention}", member.asMention)))
            .setAllowedMentions(listOf(Message.MentionType.USER)).queue();
    }
}