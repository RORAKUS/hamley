package codes.rorak.hamley.bot.reporting

import codes.rorak.hamley.util.*
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.Config.messages
import jakarta.mail.Message
import net.dv8tion.jda.api.utils.FileUpload
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ReportingImapListener {
    fun onNewMessageReceived(msg: Message) {
        if (msg.fromAddress != config.reporting.filters.address) return;

        val dateFormat = DateTimeFormatter.ofPattern("d.M.yyyy");

        val embed = when (msg.subject) {
            config.reporting.filters.actualReports -> messages.actualReport.toMessageEmbed(
                Pair("{date}", LocalDateTime.now().format(dateFormat)),
                Pair("{hour}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("h")))
            );
            config.reporting.filters.dailyReports -> messages.dailyReport.toMessageEmbed(
                Pair("{date}", LocalDateTime.now().minusDays(1).format(dateFormat))
            );
            else -> return;
        };

        val pdfFile = msg.downloadAttachment(Config.ATTACHMENT_FOLDER) ?: return;

        // convert to images
        val jpgs = PdfConverter.convert(pdfFile) ?: listOf(pdfFile);

        config.reporting.channel.toTextChannel()
            .sendMessageEmbeds(embed)
            .addFiles(jpgs.map { FileUpload.fromData(it) })
            .complete();

        pdfFile.delete();
        jpgs.forEach(File::delete);
    }
}