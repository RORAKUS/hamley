package codes.rorak.hamley.handlerfactories

import codes.rorak.hamley.handlerfactories.annotations.Option
import codes.rorak.hamley.handlerfactories.annotations.toData
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message.Attachment
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.security.InvalidParameterException
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

internal fun OptionMapping.asType(type: KType): Any? {
    return when (type.classifier) {
        String::class -> asString;
        Int::class -> asInt;
        Boolean::class -> asBoolean;
        User::class -> asUser;
        Member::class -> asMember;
        GuildChannelUnion::class -> asChannel;
        Role::class -> asRole;
        IMentionable::class -> asMentionable;
        Attachment::class -> asAttachment;
        Double::class -> asDouble;
        Long::class -> asLong;
        else -> throw InvalidParameterException("Invalid parameter type $this!");
    };
}
internal fun KType.toOptionType(): OptionType {
    return when (this.classifier) {
        String::class -> OptionType.STRING;
        Int::class -> OptionType.INTEGER;
        Boolean::class -> OptionType.BOOLEAN;
        User::class, Member::class -> OptionType.USER;
        GuildChannelUnion::class -> OptionType.CHANNEL;
        Role::class -> OptionType.ROLE;
        IMentionable::class -> OptionType.MENTIONABLE;
        Attachment::class -> OptionType.ATTACHMENT;
        Double::class, Long::class -> OptionType.NUMBER;
        else -> throw InvalidParameterException("Invalid parameter type $this!");
    };
}
internal fun KParameter.checkType() {
    val annotation = findAnnotation<Option>();

    if (type.classifier == Member::class) {
        if (!type.isMarkedNullable) throw InvalidParameterException("Parameter of type Member must be nullable!");
        else return;
    }

    if (annotation?.required != true xor type.isMarkedNullable)
        throw InvalidParameterException("When a type is required, it isn't nullable. When a type is not required, it is nullable");
}
internal inline fun <reified T> KFunction<*>.isEvent()
    = (parameters.size == 2 && parameters[1].type == T::class.createType() && returnType == Unit::class.createType());
internal fun KFunction<*>.isSlashCommand()
    = (parameters.size >= 2 && parameters.last().type == SlashCommandInteractionEvent::class.createType() && returnType == Unit::class.createType());

internal fun List<KParameter>.toOptions() =
    filter { it.hasAnnotation<Option>(); }
    .map { p ->
        p.checkType();
        val pAnnotation = p.findAnnotation<Option>()!!;
        pAnnotation.toData(p.type.toOptionType(), p.name!!);
    };

internal inline fun <reified T: Annotation> T.param(name: String) =
    T::class.declaredMemberProperties.first { it.name == name }.get(this);
internal inline fun <reified T: Annotation> T.getName() = param("name") as String;
internal inline fun <reified T: Annotation> T.getComparator() = param("comparator") as Comparator;


internal val String.firstWord: String get() {
    return split(Regex("(?=[A-Z])"))[1].lowercase();
};