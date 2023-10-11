package codes.rorak.hamley.handlerfactories

import codes.rorak.hamley.bot
import codes.rorak.hamley.handlerfactories.annotations.*
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

object InteractionEventSystem: ListenerAdapter() {
    public val customCommands = mutableListOf<CommandData>();
    private val listeners = mutableListOf<InteractionListener>();

    private fun registerListeners(vararg _listeners: InteractionListener) {
        listeners.addAll(_listeners);

        // register
        val commands = mutableListOf<CommandData>();
        listeners.forEach cls@{ cl ->
            val clAnnotation = cl::class.findAnnotation<SlashCommand>();
            val clName = if (clAnnotation?.name == "") cl::class.simpleName!!.firstWord else clAnnotation?.name;
            val subcommands = mutableMapOf<String, MutableList<SubcommandData>>();

            cl::class.declaredFunctions.forEach fns@{ fn ->
                // User contexts
                if (fn.hasAnnotation<UserContext>()) {
                    if (!fn.isEvent<UserContextInteractionEvent>()) return@fns;
                    val annotation = fn.findAnnotation<UserContext>()!!;
                    commands.add(annotation.toData(fn.name));
                }
                // Message contexts
                else if (fn.hasAnnotation<MessageContext>()) {
                    if (!fn.isEvent<MessageContextInteractionEvent>()) return@fns;
                    val annotation = fn.findAnnotation<MessageContext>()!!;
                    commands.add(annotation.toData(fn.name));
                }
                // Slash command self
                else if (fn.hasAnnotation<SlashCommand>()) {
                    if (!fn.isSlashCommand()) return@fns;
                    val annotation = fn.findAnnotation<SlashCommand>()!!;
                    val parameters = fn.parameters.toOptions();

                    commands.add(annotation.toData(fn.name).addOptions(parameters));
                }
                // Sub-slash command
                else if (fn.hasAnnotation<Subcommand>() && clName != null) {
                    if (!fn.isSlashCommand()) return@fns;
                    val annotation = fn.findAnnotation<Subcommand>()!!;
                    val parameters = fn.parameters.toOptions();
                    val command = annotation.toData(fn.name).addOptions(parameters);

                    if (!subcommands.containsKey(annotation.subcommandGroup))
                        subcommands[annotation.subcommandGroup] = mutableListOf(command);
                    else
                        subcommands[annotation.subcommandGroup]?.add(command);
                }
            };

            // add subslashes
            if (clName == null) return@cls;
            commands.add(clAnnotation!!.toData(clName)
                .apply {
                    subcommands.forEach { (group, commands) ->
                        if (group == "")
                            addSubcommands(commands);
                        else
                            addSubcommandGroups(SubcommandGroupData(group, "No description").addSubcommands(commands));
                    };
                }
            );
        };
        // add the commands
        commands.addAll(customCommands);
        bot.bot.updateCommands().addCommands(commands).complete();
    }
    // extensions
    fun JDABuilder.enableInteractionEventSystem(): JDABuilder {
        addEventListeners(InteractionEventSystem);
        return this;
    }
    fun addInteractionListener(vararg _listeners: InteractionListener) {
        registerListeners(*_listeners);
    }
    // private helpers
    private inline fun <reified T: Annotation, reified E: GenericEvent> handleEvent(id: String, event: E, hasComparator: Boolean = true) {
        listeners.forEach { cl ->
            cl::class.declaredFunctions.forEach fns@{ fn ->
                if (!fn.isEvent<E>()) return@fns;
                val annotation = fn.findAnnotation<T>() ?: return@fns;
                val name = if (annotation.getName() == "") fn.name else annotation.getName();
                if (hasComparator && !annotation.getComparator().compare(id, name)) return@fns;
                if (!hasComparator && name != id) return@fns;
                fn.call(cl, event);
            };
        };
    }
    // components
    override fun onButtonInteraction(event: ButtonInteractionEvent) =
        handleEvent<OnButtonPress, ButtonInteractionEvent>(event.componentId, event);

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) =
        handleEvent<OnStringSelect, StringSelectInteractionEvent>(event.componentId, event);

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) =
        handleEvent<OnEntitySelect, EntitySelectInteractionEvent>(event.componentId, event);

    override fun onModalInteraction(event: ModalInteractionEvent) =
        handleEvent<OnModalSubmit, ModalInteractionEvent>(event.modalId, event);

    // contexts
    override fun onUserContextInteraction(event: UserContextInteractionEvent) =
        handleEvent<UserContext, UserContextInteractionEvent>(event.name, event, false);

    override fun onMessageContextInteraction(event: MessageContextInteractionEvent) =
        handleEvent<MessageContext, MessageContextInteractionEvent>(event.name, event, false);

    // commands
    override fun onCommandAutoCompleteInteraction(event: CommandAutoCompleteInteractionEvent) {
        listeners.forEach { cl ->
            val clAnnotation = cl::class.findAnnotation<SlashCommand>();
            val clName = if (clAnnotation?.name == "") cl::class.simpleName!!.firstWord else clAnnotation?.name;

            cl::class.declaredFunctions.forEach fns@{ fn ->
                if (!fn.isEvent<CommandAutoCompleteInteractionEvent>()) return@fns;
                val annotation = fn.findAnnotation<OnCommandAutocomplete>() ?: return@fns;

                val name = if (annotation.name == "") fn.name else annotation.name;

                if (clName == null) {
                    if (name != event.name) return@fns;
                }
                else {
                    if (clName != event.name) return@fns;
                    if (name != event.subcommandName) return@fns;
                    if (annotation.subcommandGroup != event.subcommandGroup) return@fns;
                }

                fn.call(cl, event);
            };
        };
    }
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        listeners.forEach { cl ->
            val clAnnotation = cl::class.findAnnotation<SlashCommand>();
            val clName = if (clAnnotation?.name == "") cl::class.simpleName!!.firstWord else clAnnotation?.name;

            cl::class.declaredFunctions.forEach fns@{ fn ->
                val cmdAnnotation = fn.findAnnotation<SlashCommand>();
                val subAnnotation = fn.findAnnotation<Subcommand>();


                if (cmdAnnotation != null) {
                    val name = if (cmdAnnotation.name == "") fn.name else cmdAnnotation.name;
                    if (name != event.name) return@fns;
                } else if (subAnnotation != null) {
                    if (clName != event.name) return@fns;
                    if (fn.name != event.subcommandName) return@fns;
                    if (subAnnotation.subcommandGroup != (event.subcommandGroup ?: "")) return@fns;
                } else return@fns;

                val params = fn.parameters
                    .filter { it.hasAnnotation<Option>(); }
                    .map { p ->
                        val pAnnotation = p.findAnnotation<Option>()!!;
                        val pName = if (pAnnotation.name == "") p.name!! else pAnnotation.name;
                        event.getOption(pName)?.asType(p.type);
                    }.toTypedArray();

                fn.call(cl, *params, event);
            };
        };
    }
}