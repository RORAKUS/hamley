package codes.rorak.hamley.handlerfactories.annotations

import net.dv8tion.jda.api.Permission

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class MessageContext(
    val name: String = "",
    val defaultPermissions: Array<Permission> = [],
    val guildOnly: Boolean = false,
    val nsfw: Boolean = false
);