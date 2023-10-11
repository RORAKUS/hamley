package codes.rorak.hamley

import codes.rorak.hamley.bot.Bot
import codes.rorak.hamley.util.Config
import codes.rorak.hamley.util.LoggerExceptionHandler
import codes.rorak.hamley.util.info
import codes.rorak.hamley.util.loggerInit
import java.io.File

val bot = Bot;
val botThread = Thread(bot, "Bot");
fun main(args: Array<String>) {
    Thread.currentThread().name = "Main";

    val debug = if (args.isNotEmpty()) args[0] == "debug" else false;

    loggerInit(debug);
    Config.reload();

    botThread.uncaughtExceptionHandler = LoggerExceptionHandler;
    botThread.start();
    info("Bot thread started!");
}

object Resource {
    fun getAsFile(name: String): File = File(javaClass.getResource(name)!!.toURI());
};