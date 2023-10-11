package codes.rorak.hamley.handlerfactories.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Subcommand(val description: String = "", val subcommandGroup: String = "");
