package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for AspectRule and related classes.
 *
 * Business rules:
 * - AspectRule represents validation rules for aspect values
 * - Rules can be Range, Pattern, Required, Forbidden, or Custom
 * - AspectCondition defines when rules apply (AspectEquals, AspectExists, And, Or, Not)
 * - RuleValidationResult shows validation success or failure
 */
class AspectRuleTest :
    StringSpec({

        "should create Range rule with min and max" {
            val rule = AspectRule.Range(min = 0.0, max = 100.0, message = "Value must be between 0 and 100")

            rule.min shouldBe 0.0
            rule.max shouldBe 100.0
            rule.message shouldBe "Value must be between 0 and 100"
        }

        "should create Range rule with only min" {
            val rule = AspectRule.Range(min = 5.0, message = "Value must be at least 5")

            rule.min shouldBe 5.0
            rule.max shouldBe null
            rule.message shouldBe "Value must be at least 5"
        }

        "should create Range rule with only max" {
            val rule = AspectRule.Range(max = 10.0, message = "Value must be at most 10")

            rule.min shouldBe null
            rule.max shouldBe 10.0
            rule.message shouldBe "Value must be at most 10"
        }

        "should create Range rule with default message" {
            val rule = AspectRule.Range(min = 1.0, max = 5.0)

            rule.message shouldBe "Value must be within allowed range"
        }

        "should create Pattern rule with regex" {
            val rule = AspectRule.Pattern(regex = "^[A-Z]+$", message = "Must be uppercase letters only")

            rule.regex shouldBe "^[A-Z]+$"
            rule.message shouldBe "Must be uppercase letters only"
        }

        "should create Pattern rule with default message" {
            val rule = AspectRule.Pattern(regex = "\\d+")

            rule.message shouldBe "Value does not match required pattern"
        }

        "should create Required rule with condition" {
            val condition = AspectCondition.AspectEquals("status", "active")
            val rule = AspectRule.Required(condition = condition, message = "Priority is required when active")

            rule.condition shouldBe condition
            rule.message shouldBe "Priority is required when active"
        }

        "should create Required rule with default message" {
            val condition = AspectCondition.AspectExists("type")
            val rule = AspectRule.Required(condition = condition)

            rule.message shouldBe "This field is required"
        }

        "should create Forbidden rule with condition" {
            val condition = AspectCondition.AspectEquals("type", "readonly")
            val rule = AspectRule.Forbidden(condition = condition, message = "Cannot set priority for readonly items")

            rule.condition shouldBe condition
            rule.message shouldBe "Cannot set priority for readonly items"
        }

        "should create Forbidden rule with default message" {
            val condition = AspectCondition.Not(AspectCondition.AspectExists("enabled"))
            val rule = AspectRule.Forbidden(condition = condition)

            rule.message shouldBe "This field is not allowed"
        }

        "should create Custom rule with validator function" {
            val validator: (AspectValue?, Map<AspectKey, AspectValue>) -> Boolean = { value, _ ->
                value?.value?.length ?: 0 > 5
            }
            val rule = AspectRule.Custom(validator = validator, message = "Must be longer than 5 characters")

            rule.validator shouldBe validator
            rule.message shouldBe "Must be longer than 5 characters"
        }

        "should create Custom rule with default message" {
            val validator: (AspectValue?, Map<AspectKey, AspectValue>) -> Boolean = { _, _ -> true }
            val rule = AspectRule.Custom(validator = validator)

            rule.message shouldBe "Value does not meet custom requirements"
        }

        "should create AspectEquals condition" {
            val condition = AspectCondition.AspectEquals("status", "complete")

            condition.key shouldBe "status"
            condition.value shouldBe "complete"
        }

        "should create AspectExists condition" {
            val condition = AspectCondition.AspectExists("priority")

            condition.key shouldBe "priority"
        }

        "should create And condition with multiple sub-conditions" {
            val condition1 = AspectCondition.AspectEquals("status", "active")
            val condition2 = AspectCondition.AspectExists("priority")
            val andCondition = AspectCondition.And(listOf(condition1, condition2))

            andCondition.conditions shouldBe listOf(condition1, condition2)
        }

        "should create Or condition with multiple sub-conditions" {
            val condition1 = AspectCondition.AspectEquals("type", "bug")
            val condition2 = AspectCondition.AspectEquals("type", "feature")
            val orCondition = AspectCondition.Or(listOf(condition1, condition2))

            orCondition.conditions shouldBe listOf(condition1, condition2)
        }

        "should create Not condition with negated sub-condition" {
            val innerCondition = AspectCondition.AspectExists("deprecated")
            val notCondition = AspectCondition.Not(innerCondition)

            notCondition.condition shouldBe innerCondition
        }

        "should validate Range rule successfully for valid numeric value" {
            val rule = AspectRule.Range(min = 0.0, max = 10.0)
            val value = AspectValue.create("5").getOrNull()!!
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should validate Range rule successfully for null value" {
            val rule = AspectRule.Range(min = 0.0, max = 10.0)
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(null, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should fail Range rule validation for value below minimum" {
            val rule = AspectRule.Range(min = 5.0, max = 10.0)
            val value = AspectValue.create("3").getOrNull()!!
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Invalid>()
            result.rule shouldBe rule
            result.actualValue shouldBe value
        }

        "should fail Range rule validation for value above maximum" {
            val rule = AspectRule.Range(min = 0.0, max = 5.0)
            val value = AspectValue.create("8").getOrNull()!!
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Invalid>()
            result.rule shouldBe rule
            result.actualValue shouldBe value
        }

        "should fail Range rule validation for non-numeric value" {
            val rule = AspectRule.Range(min = 0.0, max = 10.0)
            val value = AspectValue.create("not-a-number").getOrNull()!!
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Invalid>()
        }

        "should validate Pattern rule successfully for matching value" {
            val rule = AspectRule.Pattern(regex = "^[A-Z]+$")
            val value = AspectValue.create("HELLO").getOrNull()!!
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should validate Pattern rule successfully for null value" {
            val rule = AspectRule.Pattern(regex = "^[A-Z]+$")
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(null, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should fail Pattern rule validation for non-matching value" {
            val rule = AspectRule.Pattern(regex = "^[A-Z]+$")
            val value = AspectValue.create("hello").getOrNull()!!
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Invalid>()
            result.rule shouldBe rule
            result.actualValue shouldBe value
        }

        "should validate Required rule successfully when condition is not met" {
            val condition = AspectCondition.AspectEquals("status", "active")
            val rule = AspectRule.Required(condition = condition)
            val value = null
            val statusKey = AspectKey.create("status").getOrNull()!!
            val aspects = mapOf(statusKey to AspectValue.create("inactive").getOrNull()!!)

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should validate Required rule successfully when condition is met and value exists" {
            val condition = AspectCondition.AspectEquals("status", "active")
            val rule = AspectRule.Required(condition = condition)
            val value = AspectValue.create("high").getOrNull()!!
            val statusKey = AspectKey.create("status").getOrNull()!!
            val aspects = mapOf(statusKey to AspectValue.create("active").getOrNull()!!)

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should fail Required rule validation when condition is met but value is missing" {
            val condition = AspectCondition.AspectEquals("status", "active")
            val rule = AspectRule.Required(condition = condition)
            val value = null
            val statusKey = AspectKey.create("status").getOrNull()!!
            val aspects = mapOf(statusKey to AspectValue.create("active").getOrNull()!!)

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Invalid>()
            result.rule shouldBe rule
            result.actualValue shouldBe value
        }

        "should validate Forbidden rule successfully when condition is not met" {
            val condition = AspectCondition.AspectEquals("type", "readonly")
            val rule = AspectRule.Forbidden(condition = condition)
            val value = AspectValue.create("high").getOrNull()!!
            val typeKey = AspectKey.create("type").getOrNull()!!
            val aspects = mapOf(typeKey to AspectValue.create("editable").getOrNull()!!)

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should validate Forbidden rule successfully when condition is met and value is absent" {
            val condition = AspectCondition.AspectEquals("type", "readonly")
            val rule = AspectRule.Forbidden(condition = condition)
            val value = null
            val typeKey = AspectKey.create("type").getOrNull()!!
            val aspects = mapOf(typeKey to AspectValue.create("readonly").getOrNull()!!)

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should fail Forbidden rule validation when condition is met and value is present" {
            val condition = AspectCondition.AspectEquals("type", "readonly")
            val rule = AspectRule.Forbidden(condition = condition)
            val value = AspectValue.create("high").getOrNull()!!
            val typeKey = AspectKey.create("type").getOrNull()!!
            val aspects = mapOf(typeKey to AspectValue.create("readonly").getOrNull()!!)

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Invalid>()
            result.rule shouldBe rule
            result.actualValue shouldBe value
        }

        "should validate Custom rule with custom logic" {
            val validator: (AspectValue?, Map<AspectKey, AspectValue>) -> Boolean = { value, aspects ->
                val length = value?.value?.length ?: 0
                val statusKey = aspects.keys.find { it.value == "status" }
                val status = statusKey?.let { aspects[it]?.value }

                if (status == "important") length >= 10 else length >= 5
            }
            val rule = AspectRule.Custom(validator = validator)
            val value = AspectValue.create("short").getOrNull()!!
            val statusKey = AspectKey.create("status").getOrNull()!!
            val aspects = mapOf(statusKey to AspectValue.create("normal").getOrNull()!!)

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Valid>()
        }

        "should fail Custom rule with custom logic" {
            val validator: (AspectValue?, Map<AspectKey, AspectValue>) -> Boolean = { value, _ ->
                value?.value?.startsWith("prefix-") ?: false
            }
            val rule = AspectRule.Custom(validator = validator)
            val value = AspectValue.create("no-prefix").getOrNull()!!
            val aspects = emptyMap<AspectKey, AspectValue>()

            val result = rule.validate(value, aspects)

            result.shouldBeInstanceOf<RuleValidationResult.Invalid>()
        }

        "should evaluate AspectEquals condition correctly" {
            val condition = AspectCondition.AspectEquals("status", "active")
            val statusKey = AspectKey.create("status").getOrNull()!!
            val aspects = mapOf(statusKey to AspectValue.create("active").getOrNull()!!)

            condition.evaluate(aspects) shouldBe true

            val otherAspects = mapOf(statusKey to AspectValue.create("inactive").getOrNull()!!)
            condition.evaluate(otherAspects) shouldBe false

            condition.evaluate(emptyMap()) shouldBe false
        }

        "should evaluate AspectExists condition correctly" {
            val condition = AspectCondition.AspectExists("priority")
            val priorityKey = AspectKey.create("priority").getOrNull()!!
            val aspects = mapOf(priorityKey to AspectValue.create("high").getOrNull()!!)

            condition.evaluate(aspects) shouldBe true

            condition.evaluate(emptyMap()) shouldBe false
        }

        "should evaluate And condition correctly" {
            val condition1 = AspectCondition.AspectEquals("status", "active")
            val condition2 = AspectCondition.AspectExists("priority")
            val andCondition = AspectCondition.And(listOf(condition1, condition2))

            val statusKey = AspectKey.create("status").getOrNull()!!
            val priorityKey = AspectKey.create("priority").getOrNull()!!

            val bothAspects = mapOf(
                statusKey to AspectValue.create("active").getOrNull()!!,
                priorityKey to AspectValue.create("high").getOrNull()!!,
            )
            andCondition.evaluate(bothAspects) shouldBe true

            val onlyStatus = mapOf(statusKey to AspectValue.create("active").getOrNull()!!)
            andCondition.evaluate(onlyStatus) shouldBe false

            val onlyPriority = mapOf(priorityKey to AspectValue.create("high").getOrNull()!!)
            andCondition.evaluate(onlyPriority) shouldBe false

            andCondition.evaluate(emptyMap()) shouldBe false
        }

        "should evaluate Or condition correctly" {
            val condition1 = AspectCondition.AspectEquals("type", "bug")
            val condition2 = AspectCondition.AspectEquals("type", "feature")
            val orCondition = AspectCondition.Or(listOf(condition1, condition2))

            val typeKey = AspectKey.create("type").getOrNull()!!

            val bugAspects = mapOf(typeKey to AspectValue.create("bug").getOrNull()!!)
            orCondition.evaluate(bugAspects) shouldBe true

            val featureAspects = mapOf(typeKey to AspectValue.create("feature").getOrNull()!!)
            orCondition.evaluate(featureAspects) shouldBe true

            val taskAspects = mapOf(typeKey to AspectValue.create("task").getOrNull()!!)
            orCondition.evaluate(taskAspects) shouldBe false

            orCondition.evaluate(emptyMap()) shouldBe false
        }

        "should evaluate Not condition correctly" {
            val innerCondition = AspectCondition.AspectExists("deprecated")
            val notCondition = AspectCondition.Not(innerCondition)

            val deprecatedKey = AspectKey.create("deprecated").getOrNull()!!
            val aspectsWithDeprecated = mapOf(deprecatedKey to AspectValue.create("true").getOrNull()!!)

            notCondition.evaluate(aspectsWithDeprecated) shouldBe false
            notCondition.evaluate(emptyMap()) shouldBe true
        }

        "should evaluate complex nested conditions correctly" {
            // (status = active AND priority exists) OR (type = urgent)
            val statusCondition = AspectCondition.AspectEquals("status", "active")
            val priorityCondition = AspectCondition.AspectExists("priority")
            val typeCondition = AspectCondition.AspectEquals("type", "urgent")

            val leftSide = AspectCondition.And(listOf(statusCondition, priorityCondition))
            val complexCondition = AspectCondition.Or(listOf(leftSide, typeCondition))

            val statusKey = AspectKey.create("status").getOrNull()!!
            val priorityKey = AspectKey.create("priority").getOrNull()!!
            val typeKey = AspectKey.create("type").getOrNull()!!

            // Matches left side (status = active AND priority exists)
            val leftMatchAspects = mapOf(
                statusKey to AspectValue.create("active").getOrNull()!!,
                priorityKey to AspectValue.create("high").getOrNull()!!,
            )
            complexCondition.evaluate(leftMatchAspects) shouldBe true

            // Matches right side (type = urgent)
            val rightMatchAspects = mapOf(typeKey to AspectValue.create("urgent").getOrNull()!!)
            complexCondition.evaluate(rightMatchAspects) shouldBe true

            // Matches neither side
            val noMatchAspects = mapOf(statusKey to AspectValue.create("inactive").getOrNull()!!)
            complexCondition.evaluate(noMatchAspects) shouldBe false
        }

        "should handle empty condition lists correctly" {
            val emptyAndCondition = AspectCondition.And(emptyList())
            val emptyOrCondition = AspectCondition.Or(emptyList())
            val aspects = mapOf(
                AspectKey.create("test").getOrNull()!! to AspectValue.create("value").getOrNull()!!,
            )

            emptyAndCondition.evaluate(aspects) shouldBe true // All empty conditions are true
            emptyOrCondition.evaluate(aspects) shouldBe false // No conditions to match
        }
    })
