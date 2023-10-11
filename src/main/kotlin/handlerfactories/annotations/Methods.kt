package codes.rorak.hamley.handlerfactories.annotations

import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

// methods
fun MessageContext.toData(_name: String) =
    Commands.message(if (name == "") _name else name)
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(*defaultPermissions))
        .setGuildOnly(guildOnly)
        .setNSFW(nsfw);

fun UserContext.toData(_name: String) =
    Commands.user(if (name == "") _name else name)
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(*defaultPermissions))
        .setGuildOnly(guildOnly)
        .setNSFW(nsfw);

fun SlashCommand.toData(name: String) =
    Commands.slash(name, description)
        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(*defaultPermissions))
        .setGuildOnly(guildOnly)
        .setNSFW(nsfw);

fun Subcommand.toData(name: String) = SubcommandData(name, description);

fun Option.check(type: OptionType) {
    if (
        (maxLength != -1 || minLength != -1) &&
        (maxDoubleValue != -1.0 || minDoubleValue != -1.0 || maxLongValue != -1L || minLongValue != -1L)
    ) throw IllegalArgumentException("Cannot set min/max length together with min/max value!");
    if (
        (maxDoubleValue != -1.0 || minDoubleValue != -1.0) &&
        (maxLongValue != -1L || minLongValue != -1L)
    ) throw IllegalArgumentException("Cannot set min/max long value together with min/max double value!");
    if (type != OptionType.CHANNEL && channelTypes.isNotEmpty())
        throw IllegalArgumentException("Cannot set channelTypes when the type is not a channel!");
}
fun Option.toData(type: OptionType, _name: String) =
    check(type).let {
        OptionData(type, if (name == "") _name else name, description)
            .setRequired(required)
            .setAutoComplete(autocomplete)
            .also { dt ->
                if (channelTypes.isNotEmpty()) dt.setChannelTypes(*channelTypes)
                if (choices.isNotEmpty()) dt.addChoices(choices.map { Command.Choice(it, it) })
                if (maxLength != -1) dt.setMaxLength(maxLength);
                if (minLength != -1) dt.setMinLength(minLength);
                if (maxDoubleValue != -1.0) dt.setMaxValue(maxDoubleValue);
                if (minDoubleValue != -1.0) dt.setMinValue(minDoubleValue);
                if (maxLongValue != -1L) dt.setMaxValue(maxLongValue);
                if (minLongValue != -1L) dt.setMinValue(minLongValue);
            };
    };