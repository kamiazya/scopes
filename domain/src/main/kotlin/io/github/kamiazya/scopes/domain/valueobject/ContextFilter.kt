package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.NonEmptyList
import io.github.kamiazya.scopes.domain.entity.AspectDefinition

/**
 * Value object representing a filter for a context view.
 * Encapsulates the AspectCriteria that defines what scopes are visible in this context.
 *
 * Business rules:
 * - Filter must be valid AspectCriteria
 * - Filter is immutable once created
 * - Filter can be evaluated against scope aspects
 */
data class ContextFilter(
    val criteria: AspectCriteria?,
    val expression: String // Store original expression for display
) {

    /**
     * Evaluate this filter against a scope's aspects.
     * Uses the evaluateWithAspects method for proper multiple value support.
     * Returns true if criteria is null (match all).
     */
    fun matches(
        aspects: Aspects,
        definitions: Map<AspectKey, AspectDefinition> = emptyMap()
    ): Boolean {
        return criteria?.evaluateWithAspects(aspects, definitions) ?: true
    }

    /**
     * Evaluate this filter against a map of aspects.
     * For backward compatibility with existing code.
     * Returns true if criteria is null (match all).
     */
    fun matches(
        aspectsMap: Map<AspectKey, NonEmptyList<AspectValue>>,
        definitions: Map<AspectKey, AspectDefinition> = emptyMap()
    ): Boolean {
        return criteria?.evaluateWithMultipleValues(aspectsMap, definitions) ?: true
    }

    companion object {
        /**
         * Create a filter that matches everything (no filtering).
         */
        fun all(): ContextFilter = ContextFilter(
            criteria = null,
            expression = "*"
        )

        /**
         * Create a filter from a simple key-value equality check.
         * Returns null if the key or value is invalid.
         */
        fun simple(key: String, value: String): ContextFilter? {
            val aspectKey = AspectKey.create(key).getOrNull() ?: return null
            val aspectValue = AspectValue.create(value).getOrNull() ?: return null
            
            return ContextFilter(
                criteria = AspectCriteria.from(
                    AspectCriterion.equals(aspectKey, aspectValue)
                ),
                expression = "$key=$value"
            )
        }
    }

    override fun toString(): String = expression
}
