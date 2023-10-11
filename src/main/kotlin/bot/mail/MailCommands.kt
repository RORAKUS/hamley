package codes.rorak.hamley.bot.mail

import codes.rorak.hamley.bot.mail.Mail.addAddressesToGroup
import codes.rorak.hamley.bot.mail.Mail.emailObj
import codes.rorak.hamley.bot.mail.Mail.groupChoices
import codes.rorak.hamley.bot.mail.Mail.removeAddressFromGroup
import codes.rorak.hamley.bot.mail.Mail.sendMailProcess
import codes.rorak.hamley.bot.mail.Mail.startMailProcess
import codes.rorak.hamley.bot.mail.Mail.stopMailProcess
import codes.rorak.hamley.bot.mail.Mail.viewMail
import codes.rorak.hamley.handlerfactories.InteractionListener
import codes.rorak.hamley.handlerfactories.annotations.OnCommandAutocomplete
import codes.rorak.hamley.handlerfactories.annotations.Option
import codes.rorak.hamley.handlerfactories.annotations.SlashCommand
import codes.rorak.hamley.handlerfactories.annotations.Subcommand
import codes.rorak.hamley.util.commaSplit
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command


@SlashCommand("Posílání emailů")
object MailCommands: InteractionListener {
    // region Main
    @Subcommand("Započne proces posílání emailu")
    fun start(event: SlashCommandInteractionEvent) =
        startMailProcess(event.user, event);
    @Subcommand("Ukončí proces posílání emailu")
    fun stop(event: SlashCommandInteractionEvent) =
        stopMailProcess(event.user, event);
    @Subcommand("Odešle aktuální zprávu aktuální zprávu")
    fun send(event: SlashCommandInteractionEvent) =
        sendMailProcess(event.user, event);
    @Subcommand("Ukáže aktuální email")
    fun view(event: SlashCommandInteractionEvent) =
        viewMail(event.user, event);
    // endregion

    // region Options
    @Subcommand("Přidá emailovou adresu do skupiny", "group")
    fun add(
        @Option("Skupina, do které chceš adresu přidat", true, true) group: String,
        @Option("Adresa či více adres oddělených čárkou, kterou chceš přidat", true) address: String,
        event: SlashCommandInteractionEvent
    ) = addAddressesToGroup(event.user, group, address.commaSplit(), event);
    @Subcommand("Odebere emailovou adresu ze skupiny", "group")
    fun remove(
        @Option("Skupina, ze které chceš adresu odebrat", true, true) group: String,
        @Option("Adresa, kterou chceš odebrat", true, true) address: String,
        event: SlashCommandInteractionEvent
    ) = removeAddressFromGroup(event.user, group, address, event);

    @OnCommandAutocomplete(subcommandGroup = "group")
    fun add(event: CommandAutoCompleteInteractionEvent) {
        val options = groupChoices(event.user, event.focusedOption.value);

        event.replyChoices(options).queue();
    }
    @OnCommandAutocomplete(subcommandGroup = "group")
    fun remove(event: CommandAutoCompleteInteractionEvent) {
        val options =
            if (event.focusedOption.name == "group")
                groupChoices(event.user, event.focusedOption.value);
            else
                event.user.emailObj.groups[event.getOption("group")?.asString ?: ""]?.addresses
                    ?.filter { it.startsWith(event.focusedOption.value); }
                    ?.map {
                        Command.Choice(it, it);
                    };

        event.replyChoices(options ?: emptyList()).queue();
    }
    // endregion
}