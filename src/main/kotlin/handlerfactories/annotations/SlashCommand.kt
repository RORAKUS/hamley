package codes.rorak.hamley.handlerfactories.annotations

import net.dv8tion.jda.api.Permission

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SlashCommand(
    val description: String = "",
    val guildOnly: Boolean = false,
    val defaultPermissions: Array<Permission> = [],
    val nsfw: Boolean = false,

    val name: String = ""
);