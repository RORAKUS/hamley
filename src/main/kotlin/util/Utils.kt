package codes.rorak.hamley.util

import codes.rorak.hamley.bot
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.MimeBodyPart
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.requests.RestAction
import org.jetbrains.annotations.Contract
import java.awt.Color
import java.io.File
import java.lang.IllegalArgumentException
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit


fun String.toRole() = bot.guild.getRoleById(this) ?: throw Exception("Role with id $this does not exist!");
fun Long.toRole() = toString().toRole();
fun Long.toTextChannel() = toString().toTextChannel();
fun String.toTextChannel() =
    bot.guild.getTextChannelById(this) ?: throw Exception("Text channel with id $this does not exist!");

fun String.toMember(): Member = bot.guild.retrieveMemberById(this).complete();
fun Long.toMember() = toString().toMember();
fun String.toUser(): User = bot.bot.retrieveUserById(this).complete();
fun Long.toUser() = toString().toUser();
fun String.toMemberOrNull() = try {
    toMember(); } catch (ex: Throwable) {
    null; }

fun Long.toMemberOrNull() = toString().toMemberOrNull();
fun Long.toUserOrNull() = toString().toUserOrNull();
fun String.toUserOrNull() = try {
    this.toUser(); } catch (_: Throwable) {
    null; };
fun Triple<String, String, Boolean>.toMessageS(): Message? =
    try {
        if (third) second.toUser().openPrivateChannel().complete().retrieveMessageById(first).complete()
        else second.toTextChannel().retrieveMessageById(first).complete();
    }
    catch (ex: ErrorResponseException) {
        warn("008: Error: ${ex.errorCode}");
        null;
    }
    catch (_: IllegalArgumentException) { null; }
fun Triple<Long, Long, Boolean>.toMessage() = Triple(first.toString(), second.toString(), third).toMessageS();
fun Pair<String, String>.toMessageS() = Triple(first, second, false).toMessageS();
fun Pair<Long, Long>.toMessage() = Triple(first, second, false).toMessage();

fun <K, V> Map<K, V>.getKey(value: V) =
    entries.firstOrNull { it.value == value }?.key

fun String.toDuration(): OffsetDateTime? {
    if (this matches Regex("[fF]orever|[nN]ever|[iI]nfinite")) return null;

    val now = OffsetDateTime.now();

    var mr = Regex("^(\\d+) ?[yY](ears?)?\$").matchEntire(this);
    if (mr != null) return now.plusYears(mr.groupValues[1].toLong());
    mr = Regex("^(\\d+) ?(mo|Mo)(nths?)?\$").matchEntire(this);
    if (mr != null) return now.plusMonths(mr.groupValues[1].toLong());
    mr = Regex("^(\\d+) ?[wW](eeks?)?\$").matchEntire(this);
    if (mr != null) return now.plusWeeks(mr.groupValues[1].toLong());
    mr = Regex("^(\\d+) ?[dD](ays?)?\$").matchEntire(this);
    if (mr != null) return now.plusDays(mr.groupValues[1].toLong());
    mr = Regex("^(\\d+) ?[hH](ours?)?\$").matchEntire(this);
    if (mr != null) return now.plusHours(mr.groupValues[1].toLong());
    mr = Regex("^(\\d+) ?[mM](in(utes?)?)?\$").matchEntire(this);
    if (mr != null) return now.plusMinutes(mr.groupValues[1].toLong());
    mr = Regex("^(\\d+) ?[sS](ec(onds?)?)?\$").matchEntire(this);
    if (mr != null) return now.plusSeconds(mr.groupValues[1].toLong());

    throw IllegalStateException("Wrong time string!");
}

fun String.toIntTimeUnit(): Pair<Int, TimeUnit>? {
    if (this matches Regex("[fF]orever|[nN]ever|[iI]nfinite")) return null;

    var mr = Regex("^(\\d+) ?[dD](ays?)?\$").matchEntire(this);
    if (mr != null) return Pair(mr.groupValues[1].toInt(), TimeUnit.DAYS);
    mr = Regex("^(\\d+) ?[hH](ours?)?\$").matchEntire(this);
    if (mr != null) return Pair(mr.groupValues[1].toInt(), TimeUnit.HOURS);
    mr = Regex("^(\\d+) ?[mM](in(utes?)?)?\$").matchEntire(this);
    if (mr != null) return Pair(mr.groupValues[1].toInt(), TimeUnit.MINUTES);
    mr = Regex("^(\\d+) ?[sS](ec(onds?)?)?\$").matchEntire(this);
    if (mr != null) return Pair(mr.groupValues[1].toInt(), TimeUnit.SECONDS);

    throw IllegalStateException("Wrong time string!");
}

@Contract("_, true, null -> fail")
fun Message.editAndAppend(toAppend: String, deleteLines: Int? = null): Message {
    editMessageComponents(emptyList()).queue(); // remove button
    // append message
    val embed = EmbedBuilder(embeds[0]);
    if (deleteLines != null)
        embed.setDescription(embeds[0].description?.split("\n")?.dropLast(deleteLines * 3)?.joinToString("\n"));
    embed.appendDescription("\n\n$toAppend");
    editMessageEmbeds(listOf(embed.build())).queue();
    return this;
}

fun retrieveEditOrWarn(messageId: Long, message: String, channelId: Long, deleteLines: Int? = null): Message? {
    return retrieveEditOrWarn(messageId.toString(), message, channelId, deleteLines);
}

fun retrieveEditOrWarn(messageId: String, message: String, channelId: Long, deleteLines: Int? = null): Message? {
    return try {
        channelId.toTextChannel()
            .retrieveMessageById(messageId).complete()
            .editAndAppend(message, deleteLines);
    } catch (ex: ErrorResponseException) {
        warn("009: ${ex.meaning}: message $messageId or channel $channelId was not found!");
        null;
    }
}

fun IReplyCallback.dfrRplE() {
    deferReply(true).queue();
}
fun IReplyCallback.dfrRpl() = deferReply().queue();

fun String.replVar(vararg replacers: Pair<String, String>): String =
    let {
        var str = it;
        for (pair in replacers) {
            val mr = Regex("%(.+)%").matchEntire(pair.first);
            str = if (mr != null)
                str.replace(
                    Regex("\\{IF \\$${mr.groupValues[1]}}([\\s\\S]*?)\\{ENDIF \\$${mr.groupValues[1]}}"),
                    if (pair.second == "true") "$1" else ""
                )
                    .replace(
                        Regex("\\{IF !\\$${mr.groupValues[1]}}([\\s\\S]*?)\\{ENDIF !\\$${mr.groupValues[1]}}"),
                        if (pair.second == "true") "" else "$1"
                    );
            else str.replace(pair.first, pair.second);
        }
        str
    }

fun String.replVarRegex(vararg replacers: Pair<Regex, String>) = let {
    var str = it;
    for (pair in replacers) str = str.replace(pair.first, pair.second);
    str;
};

fun UserSnowflake.modifyRoles(toAdd: Collection<Role>, toRemove: Collection<Role>, reason: String? = null) {
    toAdd.forEach { role ->
        bot.guild.addRoleToMember(this, role).also { if (reason != null) it.reason(reason) }.complete();
    }
    toRemove.forEach { role ->
        bot.guild.removeRoleFromMember(this, role).also { if (reason != null) it.reason(reason) }.complete();
    }
}

fun <T> List<Map<*, List<T>>>.flatten() = map { it.values.toList() }.flatten().flatten();

fun String.discordToHTML() = replVarRegex(
    Pair(Regex("\\n"), "<br>"),
    Pair(Regex("\\*\\*(?!\\*)([\\s\\S]+?)\\*\\*"), "<b>$1</b>"),
    Pair(Regex("__(?!_)([\\s\\S]+?)__"), "<u>$1</u>"),
    Pair(Regex("(?<!~)~~([\\s\\S]+?)~~"), "<s>$1</s>"),
    Pair(Regex("\\*(?!\\*)([\\s\\S]+?)\\*"), "<i>$1</i>"),
    Pair(Regex("_(?!_)([\\s\\S]+?)_"), "<i>$1</i>"),
    Pair(Regex("(?<!`)`([\\s\\S]+?)`"), "<code>$1</code>"),
    Pair(Regex("^# (.+)", RegexOption.MULTILINE), "<h1>$1</h1>"),
    Pair(Regex("^## (.+)", RegexOption.MULTILINE), "<h2>$1</h2>"),
    Pair(Regex("^### (.+)", RegexOption.MULTILINE), "<h3>$1</h3>"),
    Pair(Regex("\\[([\\s\\S]+?)]\\((.+?)\\)"), "<a href=\"$2\">$1</a>")
);
fun String.escapePings(): String {
    var str = this;

    val channels = Regex("<#(\\d+)>");
    val roles = Regex("<@&(\\d+)>");
    val users = Regex("<@(\\d+)>");

    channels.findAll(str).forEach {
        val channel = it.groupValues[1].toTextChannel();
        str = str.replaceFirst(it.value, "<b><a href=\"${channel.jumpUrl}\">#${channel.name}</a></b>");
    };
    roles.findAll(str).forEach {
        val role = it.groupValues[1].toRole();
        str = str.replaceFirst(it.value, "<b><span style=\"color: ${role.color.toText() ?: Color(Role.DEFAULT_COLOR_RAW).toText()};\">@${role.name}</span></b>");
    };
    users.findAll(str).forEach {
        val user = it.groupValues[1].toMember();
        str = str.replaceFirst(it.value, "<b><a href=\"https://discord.com/users/${user.id}\">@${user.effectiveName}</a></b>");
    };

    return str;
}
fun RestAction<Message>.queueAndDel() = try { complete().delete().queueAfter(1, TimeUnit.SECONDS); } catch (_: ErrorResponseException) {}

val GenericComponentInteractionCreateEvent.args: List<String> get() {
    val list = componentId.split("-");
    return list.subList(1, list.size);
}

fun Message.tryDeleteComplete() = try { delete().complete(); Unit; } catch (_: ErrorResponseException) {}

fun String.commaSplit() = split(",").map(String::trim);

fun Color?.toText(): String? {
    if (this == null) return null;
    fun c(num: Int) = num.toString(16).uppercase();
    return "#${c(red)}${c(green)}${c(blue)}";
}

val jakarta.mail.Message.fromAddress: String? get() {
    return if ((from?.size ?: 0) > 0)
        Regex(".+<(.+)>").matchEntire(from?.get(0)?.toString() ?: "")?.groups?.get(1)?.value ?: from?.get(0).toString()
    else null;
};

@OptIn(ExperimentalStdlibApi::class)
fun jakarta.mail.Message.downloadAttachment(folder: String): File? {
    val multipartMsg = content as Multipart;
    var file: File? = null;
    for (i in 0..< multipartMsg.count) {
        val part = multipartMsg.getBodyPart(i) as MimeBodyPart;
        if (!Part.ATTACHMENT.equals(part.disposition, true)) continue;
        file = File(folder, part.fileName);
        part.saveFile(file);
        break;
    }
    return file;
}

fun String?.nullIfDelete() = if (this == "\$\$delete") null else this;