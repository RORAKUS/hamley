package codes.rorak.hamley.bot.administration


import codes.rorak.hamley.bot
import codes.rorak.hamley.bot.administration.Administration.banMember
import codes.rorak.hamley.bot.administration.Administration.kickMember
import codes.rorak.hamley.bot.administration.Administration.muteMember
import codes.rorak.hamley.bot.administration.Administration.unbanMember
import codes.rorak.hamley.bot.administration.Administration.unmuteMember
import codes.rorak.hamley.bot.administration.Administration.warnMember
import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.OnCommandAutocomplete
import codes.rorak.hamley.handlerfactories.annotations.Option
import codes.rorak.hamley.handlerfactories.annotations.SlashCommand
import codes.rorak.hamley.util.Config.db
import codes.rorak.hamley.util.toUser
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command

object AdministrationCommands : InteractionListener {

    @SlashCommand("Umlčí daného uživatele", true)
    fun mute(
        @Option("Uživatel, kterého chceš umlčet", true) user: Member?,
        @Option("Na jak dlouho ho chceš umlčet (např. 2d, 5h, 10min, 5m, 1y, forever)") duration: String?,
        @Option("Důvod proč ho umlčet") reason: String?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        muteMember(user, event.member!!, duration, reason, event);
    }

    @SlashCommand("Vyhodí daného uživatele", true)
    fun kick(
        @Option("Uživatel, kterého chceš vyhodit", true) user: Member?,
        @Option("Důvod proč ho vyhodit") reason: String?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        kickMember(user, event.member!!, reason, event);
    }

    @SlashCommand("Zakáže danému uživateli přístup k serveru", true)
    fun ban(
        @Option("Uživatel, kterému chceš zakázet přístup k serveru", true) user: Member?,
        @Option("Na jak dlouho (např. 2d, 5h, 10min, 5m, 1y, forever)") duration: String?,
        @Option("Důvod proč mu chceš přístup zakázat") reason: String?,
        @Option("Z jaké časové periody chceš smazat jeho zprávy") delete_messages: String?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        banMember(user, event.member!!, duration, reason, delete_messages, event);
    }

    @SlashCommand("Zruší umlčení danému uživateli", true)
    fun unmute(
        @Option("Uživatel, kterého chceš od-umlčet", true) user: Member?,
        @Option("Důvod proč ho chceš od-umlčet") reason: String?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        unmuteMember(user, event.member!!, reason, event);
    }

    @SlashCommand( "Zruší danému uživateli zákaz přístupu na server", true)
    fun unban(
        @Option(
            "Uživatelské jméno (nebo označení či více označení) uživatele kterému chceš povolit přístup k serveru",
            true,
            true
        ) user: String,
        @Option("Důvod proč mu chceš zase povolit přístup k serveru") reason: String?,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        val mentions = event.getOption("user")!!.mentions.getMentions(Message.MentionType.USER);
        if (mentions.isNotEmpty()) {
            for (usr in mentions)
                unbanMember(usr.id.toUser().name, event.member!!, reason, event);
            return;
        }

        unbanMember(user, event.member!!, reason, event);
    }

    @SlashCommand("Varuje daného uživatele", true)
    fun warn(
        @Option("Uživatel, kterého chceš varovat", true) user: Member?,
        @Option("Důvod proč ho chceš varovat", true) reason: String,
        event: SlashCommandInteractionEvent
    ) {
        if (event.guild != bot.guild) return;

        warnMember(user, event.member!!, reason, event);
    }

    @OnCommandAutocomplete
    fun unban(event: CommandAutoCompleteInteractionEvent) {
        if (event.guild != bot.guild) return;

        val options = db.banlist.keys
            .map { it.toUser().name }
            .filter { it.startsWith(event.focusedOption.value) }
            .map { Command.Choice(it, it) };

        event.replyChoices(options).queue();
    }
}