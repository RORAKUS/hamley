package codes.rorak.hamley.handlerfactories.annotations

import codes.rorak.hamley.handlerfactories.Comparator

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class OnButtonPress(val name: String = "", val comparator: Comparator = Comparator.EQUALS);
