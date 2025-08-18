package io.github.kamiazya.scopes.domain.valueobject


/**
 * Represents validation rules for aspect values.
 * These rules can be applied when setting or updating aspect values.
 */
sealed class AspectRule {
    abstract val message: String

    /**
     * Range rule for numeric values.
     */
    data class Range(
        val min: Double? = null,
        val max: Double? = null,
        override val message: String = "Value must be within allowed range"
    ) : AspectRule()

    /**
     * Pattern rule for text values using regex.
     */
    data class Pattern(
        val regex: String,
        override val message: String = "Value does not match required pattern"
    ) : AspectRule()

    /**
     * Required rule - value must be present when condition is met.
     */
    data class Required(
        val condition: AspectCondition,
        override val message: String = "This field is required"
    ) : AspectRule()

    /**
     * Forbidden rule - value must not be present when condition is met.
     */
    data class Forbidden(
        val condition: AspectCondition,
        override val message: String = "This field is not allowed"
    ) : AspectRule()

    /**
     * Custom rule with validation function.
     */
    data class Custom(
        val validator: (AspectValue?, Map<AspectKey, AspectValue>) -> Boolean,
        override val message: String = "Value does not meet custom requirements"
    ) : AspectRule()
}

/**
 * Conditions for rule application.
 */
sealed class AspectCondition {
    /**
     * Check if another aspect has a specific value.
     */
    data class AspectEquals(
        val key: String,
        val value: String
    ) : AspectCondition()

    /**
     * Check if another aspect is present.
     */
    data class AspectExists(
        val key: String
    ) : AspectCondition()

    /**
     * Logical AND of multiple conditions.
     */
    data class And(
        val conditions: List<AspectCondition>
    ) : AspectCondition()

    /**
     * Logical OR of multiple conditions.
     */
    data class Or(
        val conditions: List<AspectCondition>
    ) : AspectCondition()

    /**
     * Logical NOT of a condition.
     */
    data class Not(
        val condition: AspectCondition
    ) : AspectCondition()
}

/**
 * Result of rule validation.
 */
sealed class RuleValidationResult {
    data object Valid : RuleValidationResult()
    data class Invalid(val rule: AspectRule, val actualValue: AspectValue?) : RuleValidationResult()
}

/**
 * Extension functions for rule evaluation.
 */
fun AspectRule.validate(
    value: AspectValue?,
    allAspects: Map<AspectKey, AspectValue>
): RuleValidationResult {
    val isValid = when (this) {
        is AspectRule.Range -> validateRange(value)
        is AspectRule.Pattern -> validatePattern(value)
        is AspectRule.Required -> validateRequired(value, allAspects)
        is AspectRule.Forbidden -> validateForbidden(value, allAspects)
        is AspectRule.Custom -> validator(value, allAspects)
    }

    return if (isValid) {
        RuleValidationResult.Valid
    } else {
        RuleValidationResult.Invalid(this, value)
    }
}

private fun AspectRule.Range.validateRange(value: AspectValue?): Boolean {
    if (value == null) return true // Range validation only applies if value exists

    val numericValue = value.value.toDoubleOrNull() ?: return false

    return (min == null || numericValue >= min) && (max == null || numericValue <= max)
}

private fun AspectRule.Pattern.validatePattern(value: AspectValue?): Boolean {
    if (value == null) return true // Pattern validation only applies if value exists

    return Regex(regex).matches(value.value)
}

private fun AspectRule.Required.validateRequired(
    value: AspectValue?,
    allAspects: Map<AspectKey, AspectValue>
): Boolean {
    val conditionMet = condition.evaluate(allAspects)
    return !conditionMet || value != null
}

private fun AspectRule.Forbidden.validateForbidden(
    value: AspectValue?,
    allAspects: Map<AspectKey, AspectValue>
): Boolean {
    val conditionMet = condition.evaluate(allAspects)
    return !conditionMet || value == null
}

fun AspectCondition.evaluate(aspects: Map<AspectKey, AspectValue>): Boolean {
    return when (this) {
        is AspectCondition.AspectEquals -> {
            val aspectKey = aspects.keys.find { it.value == key }
            aspectKey?.let { aspects[it]?.value == value } ?: false
        }
        is AspectCondition.AspectExists -> {
            aspects.keys.any { it.value == key }
        }
        is AspectCondition.And -> {
            conditions.all { it.evaluate(aspects) }
        }
        is AspectCondition.Or -> {
            conditions.any { it.evaluate(aspects) }
        }
        is AspectCondition.Not -> {
            !condition.evaluate(aspects)
        }
    }
}

