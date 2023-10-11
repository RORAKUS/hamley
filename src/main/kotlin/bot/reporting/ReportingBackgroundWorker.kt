package codes.rorak.hamley.bot.reporting

import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.warn
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import jakarta.mail.Folder

class ReportingBackgroundWorker(private val imapFolder: IMAPFolder): Runnable {
    override fun run() {
        while (true) {
            try {
                imapFolder.store.isConnected;
                imapFolder.idle();
            } catch (ex: Exception) {
                warn("Not idling, ${ex.message}! Trying to reconnect...");
                try {
                    imapFolder.open(Folder.READ_ONLY);
                }
                catch (ex: Exception) {
                    warn("Cannot open folder, ${ex.message}! Trying to reconnect the store...");
                    try {
                        imapFolder.store.connect(config.reporting.email.address, config.reporting.email.password);
                    }
                    catch (ex: Exception) {
                        warn("Cannot connect to store, ${ex.message}! Retrying in 10 seconds...");
                        Thread.sleep(5000);
                    }
                }
            }
        }
    }
}