package codes.rorak.hamley.handlerfactories.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class OnCommandAutocomplete(val name: String = "", val subcommandGroup: String = "");
