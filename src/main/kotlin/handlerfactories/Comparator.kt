package codes.rorak.hamley.handlerfactories

import java.util.function.BiPredicate

enum class Comparator(private val compareFunction: BiPredicate<String, String>) {
    EQUALS(String::equals),
    STARTS_WITH(String::startsWith),
    ENDS_WITH(String::endsWith),
    CONTAINS(String::contains);

    fun compare(one: String, two: String) = compareFunction.test(one, two);
}