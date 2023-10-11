package codes.rorak.hamley.util

import java.io.File
import java.io.PrintStream
import java.lang.Thread.UncaughtExceptionHandler
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

private const val LOG_DELETE_THRESHOLD = 10L; // in days

private val logsFolder = File("logs");
private var debug: Boolean = false;
fun loggerInit(_debug: Boolean = false) {
    if (!logsFolder.exists()) logsFolder.mkdir();

    // delete old logs
    for (file in logsFolder.listFiles()!!) {
        if (LocalDateTime.now().minusDays(LOG_DELETE_THRESHOLD).toEpochSecond(ZoneOffset.UTC) > file.lastModified() / 1000) // divide by 1000 - convert milliseconds
            file.delete();
    }

    debug = _debug;
}

object LoggerExceptionHandler : UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread?, ex: Throwable?) {
        msg(true, ex?.message ?: "", "ERROR", System.err, thread!!);
        ex?.printStackTrace(System.err);
        logToFile(ex?.stackTraceToString() ?: "Unknown exception??");
        exitProcess(1);
    }
}

// region Log functions

fun info(message: String) = msg(true, message, "INFO");
fun warn(message: String) = msg(true, message, "WARNING");
fun except(ex: Throwable)  = with (ex) {
    except(message ?: "");
    printStackTrace(System.err);
    logToFile(stackTraceToString());
}
fun except(message: String) = msg(true, message, "ERROR", System.err);
fun error(message: String): Nothing = except(message).run { exitProcess(1); };
fun error(ex: Throwable): Nothing {
    except(ex);
    exitProcess(1);
}
fun debug(message: String, depth: Int = 0) = with(Thread.currentThread().stackTrace[3+depth]) {
    if (debug) msg(true, "[$className.$methodName:$lineNumber] $message", "DEBUG");
};
// endregion
fun msg(
    logToFile: Boolean,
    message: String,
    type: String = "LOG",
    stream: PrintStream = System.out,
    thread: Thread = Thread.currentThread()
) {
    val time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    val msg = "[$time] [THREAD/${thread.name}] [$type] $message";

    stream.println(msg);
    if (logToFile)
        logToFile(msg);
}

private fun logToFile(message: String) {
    File(logsFolder, "log-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))}.txt")
        .appendText("$message\n");
}