package codes.rorak.hamley.bot.moderation

import codes.rorak.hamley.bot
import codes.rorak.hamley.bot.automod.Automod
import codes.rorak.hamley.bot.mail.Mail
import codes.rorak.hamley.bot.moderation.Moderation.createOneTimeInvite
import codes.rorak.hamley.bot.moderation.Moderation.generateEmbed
import codes.rorak.hamley.bot.moderation.Moderation.reloadPresence
import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.Option
import codes.rorak.hamley.handlerfactories.annotations.SlashCommand
import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.messages
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object ModerationCommands: InteractionListener {
    @SlashCommand("Says somethink as the bot")
    fun say(
        @Option("What to say", true) message: String,
        event: SlashCommandInteractionEvent
    ) {
        event.channel.sendMessage(message)
            .setAllowedMentions(listOf(MentionType.EVERYONE, MentionType.HERE, MentionType.USER, MentionType.ROLE, MentionType.CHANNEL))
            .queue();

        event.reply("Done").setEphemeral(true).queue();
    }
    @SlashCommand("Reloads the whole config")
    fun reload(event: SlashCommandInteractionEvent) {
        event.dfrRplE();

        Config.reload();
        reloadPresence();
        Mail.refreshAllMessages();
        Automod.loadRoleSelects();

        event.hook.sendMessage("Successfully reloaded!").setEphemeral(true).queue();
    }
    @SlashCommand("Removes number of messages in a channel", true)
    fun clear(
        @Option("Number of messages to clear") count: Int?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        event.dfrRplE();

        val list = mutableListOf<Message>();
        var countC = count ?: Int.MAX_VALUE;
        event.channel.iterableHistory.forEachAsync { message ->
            list.add(message);
            if (list.size == 100) {
                event.channel.purgeMessages(list);
                list.clear();
            }
            return@forEachAsync --countC > 0;
        }.thenRun {
            event.channel.purgeMessages(list); // purge remaining messages
        };

        event.hook.sendMessage(
            messages.constants.successfullyCleared.replVar(Pair("{count}", count?.toString() ?: "všechny"))
        ).setEphemeral(true).queue();
    }
    @SlashCommand("Sends you an invite link",true)
    fun invite(
        @Option("Jestli je tato pozvánka jednorázová nebo ne") onetime: Boolean?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        if (onetime != true) event.reply(config.moderation.invite).setEphemeral(true).queue();
        else event.reply(createOneTimeInvite()).setEphemeral(true).queue();
    }

    @SlashCommand("Pošle speciální embed zprávu")
    fun embed(
        @Option("Text zprávy, který není v embedu (je mimo)") message: String?,
        @Option("Hlavní nadpis zprávy") title: String?,
        @Option("Popisek zprávy") description: String?,
        @Option("Odkaz nadpisu zprávy") url: String?,
        @Option("Hex kód barvy lišty") color: String?,
        @Option("Odkaz na malý vnořený obrázek") thumbnail: String?,
        @Option("Odkaz na velký obrázek") image: String?,
        @Option("Jméno autora") author_name: String?,
        @Option("Odkaz autora") author_url: String?,
        @Option("Odkaz na obrázek na profilovku autora") author_image: String?,
        @Option("Text ve spodu zprávy") footer: String?,
        @Option("Odkaz textu ve spodu zprávy") footer_icon: String?,
        @Option("Text polí ve zprávě. Jednotlivá pole odděl `///`, údaje `;;;`. Titul;;;text;;;inline") fields: String?,
        @Option("Pokud chceš jako autora bota Hamley, nastav toto na true") default_author: Boolean?,
        event: SlashCommandInteractionEvent
    ) {
        event.dfrRpl();

        val embed =
            try {
                generateEmbed(title, description, url, color, thumbnail, image, author_name, author_url, author_image, footer, footer_icon, fields, default_author);
            }
            catch (ex: Exception) {
                event.hook.sendMessageEmbeds(messages.error.toMessageEmbed(Pair("{errorMessage}", ex.localizedMessage))).queue();
                return;
            };

        event.channel.sendMessage(message ?: "")
            .setEmbeds(embed)
            .setAllowedMentions(listOf(MentionType.ROLE, MentionType.CHANNEL, MentionType.USER, MentionType.EVERYONE, MentionType.HERE, MentionType.EMOJI, MentionType.SLASH_COMMAND))
            .complete();

        event.hook.sendMessage(messages.constants.embedSuccessfullySent).queueAndDel();
    }

    @Suppress("FunctionName")
    @SlashCommand("Upraví embed zprávu")
    fun edit_embed(
        @Option("Id zprávy, kterou chceš upravit. (google: How to copy discord message id?)", true) message_id: String,
        @Option("Text zprávy, který není v embedu (je mimo)") message: String?,
        @Option("Hlavní nadpis zprávy") title: String?,
        @Option("Popisek zprávy") description: String?,
        @Option("Odkaz nadpisu zprávy") url: String?,
        @Option("Hex kód barvy lišty") color: String?,
        @Option("Odkaz na malý vnořený obrázek") thumbnail: String?,
        @Option("Odkaz na velký obrázek") image: String?,
        @Option("Jméno autora") author_name: String?,
        @Option("Odkaz autora") author_url: String?,
        @Option("Odkaz na obrázek na profilovku autora") author_image: String?,
        @Option("Text ve spodu zprávy") footer: String?,
        @Option("Odkaz textu ve spodu zprávy") footer_icon: String?,
        @Option("Text polí ve zprávě. Jednotlivá pole odděl `///`, údaje `;;;`. Titul;;;text;;;inline") fields: String?,
        @Option("Pokud chceš jako autora bota Hamley, nastav toto na true") default_author: Boolean?,
        event: SlashCommandInteractionEvent
    ) {
        event.dfrRpl();

        val msg = Triple(message_id, if (event.isFromGuild) event.channel.id else event.user.id, !event.isFromGuild).toMessageS();
        val exemb = msg?.embeds?.get(0);

        val embed =
            try {
                generateEmbed(
                    title ?: exemb?.title,
                    description ?: exemb?.description,
                    url ?: exemb?.url,
                    color ?: exemb?.color?.toText(),
                    thumbnail ?: exemb?.thumbnail?.url,
                    image ?: exemb?.image?.url,
                    author_name ?: exemb?.author?.name,
                    author_url ?: exemb?.author?.url,
                    author_image ?: exemb?.author?.iconUrl,
                    footer ?: exemb?.footer?.text,
                    footer_icon ?: exemb?.footer?.iconUrl,
                    fields ?: exemb?.fields?.joinToString("///") { "${it.name};;;${it.value};;;${it.isInline}" },
                    default_author
                );
            }
            catch (ex: Exception) {
                event.hook.sendMessageEmbeds(messages.error.toMessageEmbed(Pair("{errorMessage}", ex.localizedMessage))).queue();
                return;
            };

        val newMsg = message ?: msg?.contentRaw ?: "";
        if (newMsg.isNotEmpty())
            msg?.editMessage(message ?: msg.contentRaw)
                ?.setEmbeds(embed)
                ?.complete();
        else
            msg?.editMessageEmbeds(embed)?.complete();

        event.hook.sendMessage(messages.constants.embedSuccessfullySent).queueAndDel();
    }
}