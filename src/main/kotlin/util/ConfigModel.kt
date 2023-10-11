package codes.rorak.hamley.util


import codes.rorak.hamley.util.Config.messages
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.activation.FileDataSource
import net.axay.simplekotlinmail.delivery.mailerBuilder
import net.axay.simplekotlinmail.email.emailBuilder
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity.ActivityType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Role
import org.simplejavamail.api.email.AttachmentResource
import org.simplejavamail.api.email.Email
import org.simplejavamail.api.mailer.Mailer
import java.awt.Color
import java.time.OffsetDateTime
import java.util.function.UnaryOperator

@Suppress("ArrayInDataClass")
class ConfigModel {
    //region Config
    data class Config(
        val token: String,
        val guild: Long,
        val botOwnerRole: Long,
        val presence: Presence?,
        val reporting: Reporting,
        val automod: Automod,
        val moderation: Moderation,
        val administration: Administration
    );

    data class Presence(
        val status: OnlineStatus = OnlineStatus.ONLINE,
        @JsonFormat(shape = JsonFormat.Shape.ARRAY) val activity: Pair<ActivityType, String>?
    );

    data class Reporting(
        val channel: Long,
        val email: ImapEmail,
        val filters: ReportingFilters,
        val pdfPK: String
    );

    data class Automod(
        val infoChannel: Long,
        val allInfoChannel: Long,
        val waitlistChannel: Long,
        val verifyPerms: Long,
        val autoRoles: Array<Long>,
        val verifyRoles: Array<Long>,
        val userNotVerifiedRole: Long
    );

    data class Moderation(
        val invite: String,
        val enterChannel: Long
    );

    data class Administration(
        val perms: Long,
        val channel: Long,
        val mute: AdminMuteAction,
        val kick: AdminAction,
        val ban: AdminTempAction,
        val unmute: AdminAction,
        val unban: AdminAction,
        val warn: AdminAction
    );


    data class ImapEmail(
        val address: String,
        val password: String,
        val imapHost: String,
        val imapPort: Int
    );
    data class ReportingFilters(
        val address: String,
        val dailyReports: String,
        val actualReports: String
    );
    data class AdminAction(
        val perms: Long? = null,
        val defaultReason: String,
        val actionName: String
    );
    data class AdminTempAction(
        val perms: Long? = null,
        val defaultReason: String,
        val actionName: String,
        val defaultTime: String
    );
    data class AdminMuteAction(
        val perms: Long? = null,
        val defaultReason: String,
        val actionName: String,
        val defaultTime: String,
        val addRoles: Array<Long>,
        val removeRoles: Array<Long>
    );
    //endregion

    //region Messages
    data class Messages(
        val constants: MessageConstants,
        val defaultAuthor: EmbedAuthor,
        val administration: Embed,
        val administrationPrivate: Embed,
        val invitePrivate: Embed,
        val memberJoin: Embed,
        val memberLeave: Embed,
        val dailyReport: Embed,
        val actualReport: Embed,
        val emailTemplate: Embed,
        val noPerms: Embed,
        val userAlreadyLeft: Embed,
        val verificationSuccessful: Embed,
        val userAlreadyVerified: Embed,
        val administrationSuccessful: Embed,
        val wrongDurationFormat: Embed,
        val memberAlreadyMuted: Embed,
        val memberNotAdminActioned: Embed,
        val invalidEmailAction: Embed,
        val actionConfirm: Embed,
        val mailEditing: Embed,
        val error: Embed,
        val userWelcomeMessage: Embed
    );
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Embed(
        val title: String? = null,
        val description: String? = null,
        val url: String? = null,
        val timestamp: OffsetDateTime? = null,
        val color: Color = Color(Role.DEFAULT_COLOR_RAW),
        val thumbnail: String? = null,
        val image: String? = null,
        val author: EmbedAuthor? = null,
        val footer: EmbedFooter? = null,
        val fields: Array<EmbedField> = emptyArray()
    );
    data class MessageConstants(
        val alreadyVerified: String,
        val userAlreadyLeft: String,
        val userVerification: String,
        val invalidMember: String,
        val alreadyUnmuted: String,
        val alreadyUnbanned: String,
        val alreadyJoined: String,
        val alreadyJoinedOK: String,
        val inviteAlreadySent: String,
        val cannotSendInvite: String,
        val joinInfo: String,
        val joinAgainInfo: String,
        val leaveInfo: String,
        val successfullyRekicked: String,
        val timeRanOut: String,
        val successfullyCleared: String,
        val noNewInvites: String,
        val mailSending: String,
        val stopMail: String,
        val startMail: String,
        val viewMail: String,
        val sendMail: String,
        val addAddressToGroup: String,
        val removeAddressFromGroup: String,
        val stopMailSuccess: String,
        val sendMailSuccess: String,
        val addAddressToGroupSuccess: String,
        val removeAddressFromGroupSuccess: String,
        val failureToSendMail: String,
        val invalidMessage: String,
        val userAlreadyBinding: String,
        val subject: String,
        val text: String,
        val attachments: String,
        val attachmentSingle: String,
        val recipients: String,
        val remAttachments: String,
        val remRecipients: String,
        val emailSuccessfullyEdited: String,
        val emailEditCanceled: String,
        val mailDoesntContain: String,
        val addressAlreadyInGroup: String,
        val attachmentLoading: String,
        val cannotSendMailWithoutRecipients: String,
        val embedSuccessfullySent: String
    );
    data class EmbedAuthor(
        val name: String? = null,
        val url: String? = null,
        val iconUrl: String? = null
    );
    data class EmbedFooter(
        val text: String? = null,
        val iconUrl: String? = null
    );
    data class EmbedField(
        val name: String = "\u200E",
        val value: String = "\u200E",
        val inline: Boolean = false
    );
    //endregion

    //region Db
    data class Db(
        val emails: MutableMap<String, MemberEmail> = mutableMapOf(),
        val actionChannels: MutableMap<String /* user id */, Triple<String /* channel id or empty if private */, String /* action */, String /* confirm message */>> = mutableMapOf(),
        val banlist: MutableMap<String, BanlistObj> = mutableMapOf(),
        val mutelist: MutableMap<String, MutelistObj> = mutableMapOf(),
        val kicklist: MutableMap<String, String> = mutableMapOf(),
        val infoMessages: MutableMap<String, String> = mutableMapOf(),
        val kickMessages: MutableMap<String, String> = mutableMapOf(),
        val roleSelects: MutableList<RoleSelectObj> = mutableListOf()
    );
    data class MemberEmail(
        val address: String,
        val emailName: String = address,
        val password: String,
        val smtpHost: String,
        val smtpPort: Int,
        val groups: Map<String, EmailGroup> = emptyMap(),
        val signature: String? = null,
        var inProcess: EmailMessage? = null,
        var inProcessMessage: Triple<String /* message id */, String /* channel OR user id */, Boolean /* is private? */ >? = null
    );
    data class MutelistObj(
        val unmute: OffsetDateTime?,
        val roles: Array<Long>,
        val muteMessageId: Long
    );
    data class BanlistObj(
        val unban: OffsetDateTime?,
        val banMessageId: Long
    );
    data class RoleSelectObj(
        val channel: Long,
        var messageId: Long?,
        val message: Embed,
        val roles: Map<String, String>
    );


    data class EmailMessage(
        var subject: String = "Bez předmětu",
        val recipients: MutableSet<String> = mutableSetOf(),
        val removedRecipients: MutableSet<String> = mutableSetOf(),
        val groups: MutableSet<String> = mutableSetOf(),
        var message: String = "",
        val attachments: MutableMap<String, String> = mutableMapOf()
    );
    data class EmailGroup(
        val addresses: MutableSet<String> = mutableSetOf(),
        val role: Long? = null,
        val channel: Long? = null
    );

    //endregion
}

fun ConfigModel.Embed.toMessageEmbed(vararg replacers: Pair<String, String>, append: String = ""): MessageEmbed =
    EmbedBuilder()
        .setTitle(title?.replVar(*replacers), url)
        .setDescription(description?.replVar(*replacers) + append)
        .setTimestamp(timestamp)
        .setColor(color)
        .setThumbnail(thumbnail?.replVar(*replacers))
        .setImage(image?.replVar(*replacers))
        .setAuthor(author?.name?.replVar(*replacers), author?.url?.replVar(*replacers), author?.iconUrl?.replVar(*replacers))
        .setFooter(footer?.text?.replVar(*replacers), footer?.iconUrl?.replVar(*replacers))
        .apply {
            for (field in this@toMessageEmbed.fields)
                addField(field.name.replVar(*replacers), field.value.replVar(*replacers), field.inline)
        }.apply {
            if (author == null)
                setAuthor(messages.defaultAuthor.name?.replVar(*replacers), messages.defaultAuthor.url?.replVar(*replacers), messages.defaultAuthor.iconUrl?.replVar(*replacers))
        }
        .build();

fun ConfigModel.MemberEmail.toMailer(): Mailer {
    return mailerBuilder(smtpHost, smtpPort, address, password);
}
fun ConfigModel.EmailMessage.toEmail(sender: ConfigModel.MemberEmail): Email? {
    val addresses = getAllAddresses(sender);
    if (addresses.isEmpty()) return null;

    return emailBuilder {
        from(sender.emailName, sender.address);
        bccAddresses(addresses);

        withSubject(this@toEmail.subject);
        withHTMLText("${message.discordToHTML().escapePings()}${if (sender.signature != null) "<br><br>${sender.signature}" else ""}");

        withAttachments(this@toEmail.attachments.map {
            AttachmentResource(it.key, FileDataSource(it.value))
        });
    };
}
fun ConfigModel.EmailMessage.realGroups(emailObj: ConfigModel.MemberEmail) =
    groups.map { emailObj.groups[it]!! };
fun ConfigModel.EmailMessage.getAllAddresses(emailObj: ConfigModel.MemberEmail, nameTransform: UnaryOperator<String>? = null) =
    groups.spreadGroups(emailObj, nameTransform) +
    (if (nameTransform != null) setOf(nameTransform.apply("Separate")) else emptySet()) +
    recipients - removedRecipients;
fun Set<String>.spreadGroups(emailObj: ConfigModel.MemberEmail, nameTransform: UnaryOperator<String>? = null) =
    map { if (nameTransform != null) listOf(nameTransform.apply("Group $it"), *emailObj.groups[it]!!.addresses.toTypedArray()) else emailObj.groups[it]!!.addresses }
    .flatten();