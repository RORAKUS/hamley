package codes.rorak.hamley.bot

import codes.rorak.hamley.bot.administration.*
import codes.rorak.hamley.bot.automod.Automod
import codes.rorak.hamley.bot.automod.AutomodCommands
import codes.rorak.hamley.bot.automod.AutomodInteractions
import codes.rorak.hamley.bot.automod.AutomodListener
import codes.rorak.hamley.bot.mail.Mail
import codes.rorak.hamley.bot.mail.MailCommands
import codes.rorak.hamley.bot.mail.MailInteractions
import codes.rorak.hamley.bot.mail.MailListener
import codes.rorak.hamley.bot.moderation.Moderation
import codes.rorak.hamley.bot.moderation.ModerationCommands
import codes.rorak.hamley.bot.moderation.ModerationInteractions
import codes.rorak.hamley.bot.moderation.ModerationListener
import codes.rorak.hamley.bot.reporting.ReportingBackgroundWorker
import codes.rorak.hamley.bot.reporting.ReportingImapListener
import codes.rorak.hamley.handlerfactories.InteractionEventSystem.addInteractionListener
import codes.rorak.hamley.handlerfactories.InteractionEventSystem.enableInteractionEventSystem
import codes.rorak.hamley.util.Config.config
import codes.rorak.hamley.util.ImapMonitor
import codes.rorak.hamley.util.LoggerExceptionHandler
import codes.rorak.hamley.util.info
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.exceptions.InvalidTokenException
import net.dv8tion.jda.api.requests.GatewayIntent

object Bot : Runnable {
    lateinit var bot: JDA private set;
    lateinit var guild: Guild private set;
    override fun run() {
        val builder = JDABuilder
            .createDefault(config.token)
            .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES)
            .enableInteractionEventSystem()
            .addEventListeners(LoggerEventListener);

        try {
            bot = builder.build().awaitReady();
        } catch (ex: InvalidTokenException) {
            error("011: Invalid token!");
        }

        info("Configuring the bot...");
        configure();
        info("Bot successfully configured!");
    }

    private fun configure() {
        // set guild
        guild = bot.getGuildById(config.guild) ?: throw Exception("Guild with id ${config.guild} was not found!");
        // set presence
        Moderation.reloadPresence();

        registerImapMonitor();

        registerListeners();
        registerInteractions();
        startBackgroundWorkers();

        Mail.refreshAllMessages();
        Automod.loadRoleSelects();
    }

    private fun registerListeners() {
        bot.addEventListener(
            AdministrationListener, AutomodListener,
            ModerationListener, MailListener
        );
        ImapMonitor.registerListeners(ReportingImapListener::onNewMessageReceived);
    }
    private fun registerInteractions() {
        addInteractionListener(
            AutomodCommands, AutomodInteractions,
            AdministrationCommands, AdministrationInteractions,
            ModerationCommands, ModerationInteractions,
            MailCommands, MailInteractions
        );
    }

    private fun startBackgroundWorkers() {
        Thread(AdministrationBackgroundWorker, "ADM-Background").apply {
            uncaughtExceptionHandler = LoggerExceptionHandler
        }.start();

        Thread(ReportingBackgroundWorker(ImapMonitor.inbox), "REP-Background").apply {
            uncaughtExceptionHandler = LoggerExceptionHandler
        }.start();
    }

    private fun registerImapMonitor() = with (config.reporting.email) {
        ImapMonitor.init(imapHost, imapPort, address, password);
    };
}