package io.github.kamiazya.scopes.application.service

import arrow.core.NonEmptyList
import io.github.kamiazya.scopes.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectRule
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.valueobject.RuleValidationResult
import io.github.kamiazya.scopes.domain.valueobject.validate

/**
 * Application layer wrapper that combines AspectDefinition with validation rules.
 * Keeps the domain entity serializable while providing rule validation capabilities.
 */
data class AspectDefinitionWithRules(val definition: AspectDefinition, val rules: List<AspectRule> = emptyList()) {
    /**
     * Validate if a value is compatible with this aspect definition and its rules.
     */
    fun isValidValue(value: AspectValue): Boolean = definition.isValidValue(value)

    /**
     * Validate a single value against all rules in the context of other aspects.
     */
    fun validateRules(
        value: AspectValue?,
        allAspects: Map<AspectKey, NonEmptyList<AspectValue>>,
    ): List<RuleValidationResult> {
        val flatAspects = allAspects.mapValues { (_, values) ->
            values.head
        }
        return rules.map { rule -> rule.validate(value, flatAspects) }
    }

    /**
     * Validate multiple values against all rules in the context of other aspects.
     */
    fun validateRules(
        values: List<AspectValue>,
        allAspects: Map<AspectKey, NonEmptyList<AspectValue>>,
    ): List<RuleValidationResult> {
        if (!definition.allowMultiple && values.size > 1) {
            return listOf(
                RuleValidationResult.Invalid(
                    // This is a simple rule that could be better formalized
                    rules.firstOrNull() ?: return emptyList(),
                    values.firstOrNull(),
                ),
            )
        } else {
            val flatAspects = allAspects.mapNotNull { (key, valueList) ->
                valueList.firstOrNull()?.let { key to it }
            }.toMap()

            // If values is empty, we still need to check required rules
            if (values.isEmpty()) {
                return rules.map { rule -> rule.validate(null, flatAspects) }
            }

            return values.flatMap { value ->
                rules.map { rule -> rule.validate(value, flatAspects) }
            }
        }
    }

    /**
     * Check if all rules are satisfied (single value).
     */
    fun isValidWithRules(value: AspectValue?, allAspects: Map<AspectKey, NonEmptyList<AspectValue>>): Boolean = validateRules(value, allAspects).all { it is RuleValidationResult.Valid }

    /**
     * Check if all rules are satisfied (multiple values).
     */
    fun isValidWithRules(values: List<AspectValue>, allAspects: Map<AspectKey, NonEmptyList<AspectValue>>): Boolean = validateRules(values, allAspects).all { it is RuleValidationResult.Valid }

    /**
     * Get validation error messages for failed rules (single value).
     */
    fun getValidationMessages(
        value: AspectValue?,
        allAspects: Map<AspectKey, NonEmptyList<AspectValue>>,
    ): List<String> = validateRules(value, allAspects)
        .filterIsInstance<RuleValidationResult.Invalid>()
        .map { it.rule.message }

    /**
     * Get validation error messages for failed rules (multiple values).
     */
    fun getValidationMessages(
        values: List<AspectValue>,
        allAspects: Map<AspectKey, NonEmptyList<AspectValue>>,
    ): List<String> = validateRules(values, allAspects)
        .filterIsInstance<RuleValidationResult.Invalid>()
        .map { it.rule.message }

    // Delegate other operations to the domain entity
    fun getValueOrder(value: AspectValue): Int? = definition.getValueOrder(value)

    fun compareValues(first: AspectValue, second: AspectValue): Int? = definition.compareValues(first, second)
}
