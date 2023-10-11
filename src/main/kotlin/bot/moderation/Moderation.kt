package codes.rorak.hamley.bot.moderation

import codes.rorak.hamley.bot
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.messages
import codes.rorak.hamley.util.nullIfDelete
import codes.rorak.hamley.util.toMessageEmbed
import codes.rorak.hamley.util.toTextChannel
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.awt.Color
import java.util.concurrent.TimeUnit

object Moderation {
    fun createOneTimeInvite() =
        config.moderation.enterChannel.toTextChannel().createInvite().setMaxUses(1).setMaxAge(7, TimeUnit.DAYS).complete().url;
    fun reloadPresence() {
        config.presence?.let {
            bot.bot.presence.setStatus(it.status);
            bot.bot.presence.activity =
                if (it.activity?.first != null) Activity.of(it.activity.first, it.activity.second) else null;
        };
    }
    fun sendConfirmation(action: String, yesId: String, event: IReplyCallback) {
        val embed = messages.actionConfirm.toMessageEmbed(Pair("{action}", action));
        val confirmRow = ActionRow.of(
            Button.success(yesId, "Ano"),
            Button.danger("selfdeletebtn", "Ne")
        );

        event.hook.sendMessageEmbeds(embed).addComponents(confirmRow).setEphemeral(true).queue();
    }

    internal fun generateEmbed(
        title: String?,
        description: String?,
        url: String?,
        color: String?,
        thumbnail: String?,
        image: String?,
        author_name: String?,
        author_url: String?,
        author_image: String?,
        footer: String?,
        footer_icon: String?,
        fields: String?,
        default_author: Boolean?,
    ) =
        EmbedBuilder()
            .setTitle(title.nullIfDelete(), url.nullIfDelete())
            .setDescription(description.nullIfDelete()?.replace("\\n", "\n"))
            .setColor(if (color.nullIfDelete() != null) Color.decode(color) else null)
            .setThumbnail(thumbnail.nullIfDelete())
            .setImage(image.nullIfDelete())
            .setAuthor(
                author_name.nullIfDelete() ?: if (default_author == true) messages.defaultAuthor.name else null,
                author_url.nullIfDelete() ?: if (default_author == true) messages.defaultAuthor.url else null,
                author_image.nullIfDelete() ?: if (default_author == true) messages.defaultAuthor.iconUrl else null
            )
            .setFooter(footer.nullIfDelete(), footer_icon.nullIfDelete())
            .apply {
                fields.nullIfDelete()?.split("///")?.forEach {
                    if (it.isEmpty()) return@forEach;
                    val args = it.split(";;;");
                    addField(args[0], args[1], args[2] == "true");
                };
            }.build();
}