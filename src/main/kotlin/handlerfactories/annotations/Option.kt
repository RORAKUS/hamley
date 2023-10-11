package codes.rorak.hamley.handlerfactories.annotations

import net.dv8tion.jda.api.entities.channel.ChannelType

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Option(
    val description: String = "No description",
    val required: Boolean = false,
    val autocomplete: Boolean = false,
    val name: String = "",

    val channelTypes: Array<ChannelType> = [],
    val maxLength: Int = -1,
    val minLength: Int = -1,

    val maxDoubleValue: Double = -1.0,
    val minDoubleValue: Double = -1.0,
    val maxLongValue: Long = -1,
    val minLongValue: Long = -1,

    val choices: Array<String> = []
);
