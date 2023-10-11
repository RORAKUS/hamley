package codes.rorak.hamley.util

import codes.rorak.hamley.Resource
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.addDeserializer
import com.fasterxml.jackson.module.kotlin.addSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.awt.Color
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

object Config {
    public const val ATTACHMENT_FOLDER = "attachments";
    private const val CONFIG_FILE = "config.json";
    private const val MESSAGES_FILE = "messages.json";
    private const val DB_FILE = "db.json";

    private val attachmentFolder = File(ATTACHMENT_FOLDER);
    private val configFile = File(CONFIG_FILE);
    private val messagesFile = File(MESSAGES_FILE);
    private val dbFile = File(DB_FILE);

    private val mapper = jacksonObjectMapper();


    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss XXXXX");

    lateinit var config: ConfigModel.Config private set;
    lateinit var messages: ConfigModel.Messages private set;
    var db: ConfigModel.Db private set;

    init {
        mapper.registerModule(
            SimpleModule()
                .addSerializer(OffsetDateTime::class.java, OffsetDateTimeSerializer())
                .addDeserializer(OffsetDateTime::class.java, OffsetDateTimeDeserializer())
                .addSerializer(Color::class, ColorSerializer())
                .addDeserializer(Color::class, ColorDeserializer())
        );
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);

        db = ConfigModel.Db();
    }

    fun reload() {
        info("Reloading the config...");
        if (!attachmentFolder.exists())
            attachmentFolder.mkdir();
        if (!messagesFile.exists())
            Resource.getAsFile(MESSAGES_FILE).copyTo(messagesFile);
        if (!dbFile.exists())
            saveDb()
        if (!configFile.exists()) {
            Resource.getAsFile(CONFIG_FILE).copyTo(configFile);
            warn("007: Config file not found - created one. Please enter your details there...");
            exitProcess(0);
        }

        config = mapper.readValue(configFile, ConfigModel.Config::class.java);
        messages = mapper.readValue(messagesFile, ConfigModel.Messages::class.java);
        db = mapper.readValue(dbFile, ConfigModel.Db::class.java);

        clearUnusedAttachments();

        info("Config successfully reloaded!");
    }

    private fun clearUnusedAttachments() {
        val validFiles = db.emails.map { (_, emailObj) ->
            emailObj.inProcess?.attachments?.map { it.value }
        }.filterNotNull().flatten();

        attachmentFolder.listFiles()?.forEach {
            if (!validFiles.contains(it.absolutePath)) it.delete();
        } ?: warn("Warning 9687");
    }
    fun saveDb() {
        mapper.writeValue(dbFile, db);
        debug("Database saved!", 1);
    }

    val uniqueFile: File get() {
        return File(attachmentFolder, "attachment-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yy--HH-mm-ss--nn"))}--${(0..10000).random()}");
    };
}