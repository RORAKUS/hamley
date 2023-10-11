package codes.rorak.hamley.bot

import codes.rorak.hamley.util.error
import codes.rorak.hamley.util.info
import net.dv8tion.jda.api.events.ExceptionEvent
import net.dv8tion.jda.api.events.session.*
import net.dv8tion.jda.api.hooks.ListenerAdapter

object LoggerEventListener : ListenerAdapter() {
    override fun onReady(event: ReadyEvent) = info("Successfully logged in as ${event.jda.selfUser.asTag}.");
    override fun onSessionDisconnect(event: SessionDisconnectEvent) = info("Disconnected.");
    override fun onException(event: ExceptionEvent) = error(event.cause);
    override fun onSessionRecreate(event: SessionRecreateEvent) = info("Reconnected.");
    override fun onSessionResume(event: SessionResumeEvent) = info("Resumed.");
    override fun onShutdown(event: ShutdownEvent) = info("Successfully shut down.");

}