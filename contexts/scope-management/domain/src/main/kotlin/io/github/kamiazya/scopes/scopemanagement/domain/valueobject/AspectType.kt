package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

/**
 * Represents the type of an aspect definition.
 */
sealed class AspectType {
    /**
     * Free-form text values.
     */
    data object Text : AspectType()

    /**
     * Numeric values that can be compared.
     */
    data object Numeric : AspectType()

    /**
     * Boolean values (true/false).
     */
    data object BooleanType : AspectType()

    /**
     * Ordered values from a predefined list.
     */
    data class Ordered(val allowedValues: List<AspectValue>) : AspectType()

    /**
     * Duration values in ISO 8601 format (e.g., "P1D", "PT2H30M", "P1W").
     * Supports comparisons for time-based queries.
     */
    data object Duration : AspectType()
}
