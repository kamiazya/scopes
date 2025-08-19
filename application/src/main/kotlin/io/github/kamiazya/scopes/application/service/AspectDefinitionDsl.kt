package io.github.kamiazya.scopes.application.service

import arrow.core.getOrElse
import io.github.kamiazya.scopes.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectRule
import io.github.kamiazya.scopes.domain.valueobject.AspectCondition
import io.github.kamiazya.scopes.domain.valueobject.AspectValue

/**
 * DSL for creating aspect definitions with improved readability and conciseness.
 *
 * Example usage:
 * ```
 * val definitions = aspectDefinitions {
 *     ordered("priority") {
 *         description = "Task priority level"
 *         values("low", "medium", "high", "critical")
 *         rules {
 *             required("Priority is required for critical tasks") {
 *                 aspectEquals("status", "critical")
 *             }
 *         }
 *     }
 *
 *     numeric("effort") {
 *         description = "Estimated effort in hours"
 *         rules {
 *             range(min = 0.5, max = 100.0, "Effort must be between 0.5 and 100 hours")
 *             required("Effort is required when status is in progress") {
 *                 aspectEquals("status", "in_progress")
 *             }
 *         }
 *     }
 *
 *     text("comment") {
 *         description = "Additional comments"
 *         rules {
 *             pattern("^[A-Za-z0-9\\s,.!?-]+$", "Comments must contain only alphanumeric characters and basic punctuation")
 *         }
 *     }
 * }
 * ```
 */

/**
 * Main DSL entry point for creating aspect definitions.
 */
fun aspectDefinitions(init: AspectDefinitionsBuilder.() -> Unit): List<AspectDefinition> {
    val builder = AspectDefinitionsBuilder()
    builder.init()
    return builder.build()
}

/**
 * DSL entry point for creating aspect definitions with rules.
 */
fun aspectDefinitionsWithRules(init: AspectDefinitionsWithRulesBuilder.() -> Unit): List<AspectDefinitionWithRules> {
    val builder = AspectDefinitionsWithRulesBuilder()
    builder.init()
    return builder.build()
}

/**
 * Builder for collecting multiple aspect definitions.
 */
class AspectDefinitionsBuilder {
    private val definitions = mutableListOf<AspectDefinition>()

    /**
     * Create an ordered aspect definition with allowed values.
     */
    fun ordered(key: String, init: OrderedAspectBuilder.() -> Unit) {
        val builder = OrderedAspectBuilder(key)
        builder.init()
        definitions.add(builder.build())
    }

    /**
     * Create a numeric aspect definition.
     */
    fun numeric(key: String, init: NumericAspectBuilder.() -> Unit) {
        val builder = NumericAspectBuilder(key)
        builder.init()
        definitions.add(builder.build())
    }

    /**
     * Create a text aspect definition.
     */
    fun text(key: String, init: TextAspectBuilder.() -> Unit) {
        val builder = TextAspectBuilder(key)
        builder.init()
        definitions.add(builder.build())
    }

    /**
     * Create a boolean aspect definition.
     */
    fun boolean(key: String, init: BooleanAspectBuilder.() -> Unit) {
        val builder = BooleanAspectBuilder(key)
        builder.init()
        definitions.add(builder.build())
    }

    fun build(): List<AspectDefinition> = definitions.toList()
}

/**
 * Builder for collecting multiple aspect definitions with rules.
 */
class AspectDefinitionsWithRulesBuilder {
    private val definitions = mutableListOf<AspectDefinitionWithRules>()

    /**
     * Create an ordered aspect definition with allowed values.
     */
    fun ordered(key: String, init: OrderedAspectBuilder.() -> Unit) {
        val builder = OrderedAspectBuilder(key)
        builder.init()
        definitions.add(builder.buildWithRules())
    }

    /**
     * Create a numeric aspect definition.
     */
    fun numeric(key: String, init: NumericAspectBuilder.() -> Unit) {
        val builder = NumericAspectBuilder(key)
        builder.init()
        definitions.add(builder.buildWithRules())
    }

    /**
     * Create a text aspect definition.
     */
    fun text(key: String, init: TextAspectBuilder.() -> Unit) {
        val builder = TextAspectBuilder(key)
        builder.init()
        definitions.add(builder.buildWithRules())
    }

    /**
     * Create a boolean aspect definition.
     */
    fun boolean(key: String, init: BooleanAspectBuilder.() -> Unit) {
        val builder = BooleanAspectBuilder(key)
        builder.init()
        definitions.add(builder.buildWithRules())
    }

    fun build(): List<AspectDefinitionWithRules> = definitions.toList()
}

/**
 * Builder for ordered aspect definitions.
 */
class OrderedAspectBuilder(private val key: String) {
    var description: String? = null
    var allowMultiple: Boolean = false
    private val allowedValues = mutableListOf<String>()
    private val rules = mutableListOf<AspectRule>()

    /**
     * Set allowed values for this ordered aspect.
     */
    fun values(vararg values: String) {
        allowedValues.addAll(values)
    }

    /**
     * Set allowed values from a list.
     */
    fun values(values: List<String>) {
        allowedValues.addAll(values)
    }

    /**
     * Add rules for this aspect.
     */
    fun rules(init: RuleBuilder.() -> Unit) {
        val builder = RuleBuilder()
        builder.init()
        rules.addAll(builder.build())
    }

    fun build(): AspectDefinition {
        val aspectKey = AspectKey.create(key).getOrElse {
            error("Invalid aspect key: $key")
        }
        val aspectValues = allowedValues.map { value ->
            AspectValue.create(value).getOrElse {
                error("Invalid aspect value: $value for key: $key")
            }
        }

        return AspectDefinition.createOrdered(
            key = aspectKey,
            allowedValues = aspectValues,
            description = description,
            allowMultiple = allowMultiple
        ).getOrElse {
            error("Failed to create ordered aspect definition for key: $key")
        }
    }

    fun buildWithRules(): AspectDefinitionWithRules {
        return AspectDefinitionWithRules(build(), rules)
    }
}

/**
 * Builder for numeric aspect definitions.
 */
class NumericAspectBuilder(private val key: String) {
    var description: String? = null
    var allowMultiple: Boolean = false
    private val rules = mutableListOf<AspectRule>()

    /**
     * Add rules for this aspect.
     */
    fun rules(init: RuleBuilder.() -> Unit) {
        val builder = RuleBuilder()
        builder.init()
        rules.addAll(builder.build())
    }

    fun build(): AspectDefinition {
        val aspectKey = AspectKey.create(key).getOrElse {
            error("Invalid aspect key: $key")
        }

        return AspectDefinition.createNumeric(
            key = aspectKey,
            description = description,
            allowMultiple = allowMultiple
        )
    }

    fun buildWithRules(): AspectDefinitionWithRules {
        return AspectDefinitionWithRules(build(), rules)
    }
}

/**
 * Builder for text aspect definitions.
 */
class TextAspectBuilder(private val key: String) {
    var description: String? = null
    var allowMultiple: Boolean = false
    private val rules = mutableListOf<AspectRule>()

    /**
     * Add rules for this aspect.
     */
    fun rules(init: RuleBuilder.() -> Unit) {
        val builder = RuleBuilder()
        builder.init()
        rules.addAll(builder.build())
    }

    fun build(): AspectDefinition {
        val aspectKey = AspectKey.create(key).getOrElse {
            error("Invalid aspect key: $key")
        }

        return AspectDefinition.createText(
            key = aspectKey,
            description = description,
            allowMultiple = allowMultiple
        )
    }

    fun buildWithRules(): AspectDefinitionWithRules {
        return AspectDefinitionWithRules(build(), rules)
    }
}

/**
 * Builder for boolean aspect definitions.
 */
class BooleanAspectBuilder(private val key: String) {
    var description: String? = null
    var allowMultiple: Boolean = false
    private val rules = mutableListOf<AspectRule>()

    /**
     * Add rules for this aspect.
     */
    fun rules(init: RuleBuilder.() -> Unit) {
        val builder = RuleBuilder()
        builder.init()
        rules.addAll(builder.build())
    }

    fun build(): AspectDefinition {
        val aspectKey = AspectKey.create(key).getOrElse {
            error("Invalid aspect key: $key")
        }

        return AspectDefinition.createBoolean(
            key = aspectKey,
            description = description,
            allowMultiple = allowMultiple
        )
    }

    fun buildWithRules(): AspectDefinitionWithRules {
        return AspectDefinitionWithRules(build(), rules)
    }
}

/**
 * Builder for aspect rules.
 */
class RuleBuilder {
    private val rules = mutableListOf<AspectRule>()

    /**
     * Add a range rule for numeric values.
     */
    fun range(min: Double? = null, max: Double? = null, message: String = "Value must be within allowed range") {
        rules.add(AspectRule.Range(min, max, message))
    }

    /**
     * Add a pattern rule for text values.
     */
    fun pattern(regex: String, message: String = "Value does not match required pattern") {
        rules.add(AspectRule.Pattern(regex, message))
    }

    /**
     * Add a required rule with condition.
     */
    fun required(message: String = "This field is required", init: ConditionBuilder.() -> AspectCondition) {
        val condition = ConditionBuilder().init()
        rules.add(AspectRule.Required(condition, message))
    }

    /**
     * Add a forbidden rule with condition.
     */
    fun forbidden(message: String = "This field is not allowed", init: ConditionBuilder.() -> AspectCondition) {
        val condition = ConditionBuilder().init()
        rules.add(AspectRule.Forbidden(condition, message))
    }

    /**
     * Add a custom rule with validation function.
     */
    fun custom(message: String, validator: (AspectValue?, Map<AspectKey, AspectValue>) -> Boolean) {
        rules.add(AspectRule.Custom(validator, message))
    }

    fun build(): List<AspectRule> = rules.toList()
}

/**
 * Builder for aspect conditions.
 */
class ConditionBuilder {
    /**
     * Check if another aspect equals a specific value.
     */
    fun aspectEquals(key: String, value: String): AspectCondition =
        AspectCondition.AspectEquals(key, value)

    /**
     * Check if another aspect exists.
     */
    fun aspectExists(key: String): AspectCondition =
        AspectCondition.AspectExists(key)

    /**
     * Logical AND of conditions.
     */
    fun and(vararg conditions: AspectCondition): AspectCondition =
        AspectCondition.And(conditions.toList())

    /**
     * Logical OR of conditions.
     */
    fun or(vararg conditions: AspectCondition): AspectCondition =
        AspectCondition.Or(conditions.toList())

    /**
     * Logical NOT of condition.
     */
    fun not(condition: AspectCondition): AspectCondition =
        AspectCondition.Not(condition)
}

