package codes.rorak.hamley.util

import com.sun.mail.imap.IMAPFolder
import jakarta.mail.Folder
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.event.MessageCountAdapter
import jakarta.mail.event.MessageCountEvent
import java.util.*
import java.util.function.Consumer

object ImapMonitor {
    lateinit var inbox: IMAPFolder private set;

    private val listeners: MutableList<Consumer<Message>> = mutableListOf();
    fun init(host: String, port: Int, username: String, password: String) {
        val properties = Properties().apply {
            put("mail.imap.host", host);
            put("mail.imap.port", port);

            setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            setProperty("mail.imap.socketFactory.fallback", "false");
            setProperty("mail.imap.socketFactory.port", port.toString());
        };

        val session = Session.getDefaultInstance(properties);
        val store = session.getStore("imap");
        store.connect(username, password);

        inbox = store.getFolder("INBOX") as IMAPFolder;
        inbox.open(Folder.READ_WRITE);

        inbox.addMessageCountListener(innerListener);
    }

    fun registerListeners(vararg listeners: Consumer<Message>) =
        this.listeners.addAll(listeners);

    private val innerListener = object: MessageCountAdapter() {
        override fun messagesAdded(e: MessageCountEvent?) {
            debug("Message received: ${e.toString()}");
            if (e == null) return;

            e.messages.forEach { msg ->
                debug("New message! From: ${msg.from?.get(0).toString()} / ${msg.fromAddress}, Subject: ${msg.subject}");
                listeners.forEach { fn ->
                    fn.accept(msg);
                }
            };
        }
    }
}