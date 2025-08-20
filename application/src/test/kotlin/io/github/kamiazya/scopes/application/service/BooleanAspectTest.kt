package io.github.kamiazya.scopes.application.service

import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectType
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BooleanAspectTest :
    StringSpec({

        "Boolean AspectValue should validate correctly" {
            val trueValue = AspectValue.create("true").getOrElse { error("Invalid value") }
            val falseValue = AspectValue.create("false").getOrElse { error("Invalid value") }
            val invalidValue = AspectValue.create("maybe").getOrElse { error("Invalid value") }

            trueValue.isBoolean() shouldBe true
            trueValue.toBooleanValue() shouldBe true

            falseValue.isBoolean() shouldBe true
            falseValue.toBooleanValue() shouldBe false

            invalidValue.isBoolean() shouldBe false
            invalidValue.toBooleanValue() shouldBe null
        }

        "Boolean AspectValue should handle case insensitive values" {
            val trueCaps = AspectValue.create("TRUE").getOrElse { error("Invalid value") }
            val falseMixed = AspectValue.create("False").getOrElse { error("Invalid value") }

            trueCaps.isBoolean() shouldBe true
            trueCaps.toBooleanValue() shouldBe true

            falseMixed.isBoolean() shouldBe true
            falseMixed.toBooleanValue() shouldBe false
        }

        "Boolean AspectDefinition should validate values correctly" {
            val key = AspectKey.create("is_active").getOrElse { error("Invalid key") }
            val definition = AspectDefinition.createBoolean(key, "Whether the item is active")

            definition.type shouldBe AspectType.BooleanType
            definition.description shouldBe "Whether the item is active"

            val trueValue = AspectValue.create("true").getOrElse { error("Invalid value") }
            val falseValue = AspectValue.create("false").getOrElse { error("Invalid value") }
            val invalidValue = AspectValue.create("maybe").getOrElse { error("Invalid value") }

            definition.isValidValue(trueValue) shouldBe true
            definition.isValidValue(falseValue) shouldBe true
            definition.isValidValue(invalidValue) shouldBe false
        }

        "Boolean AspectDefinition should compare values correctly" {
            val key = AspectKey.create("is_active").getOrElse { error("Invalid key") }
            val definition = AspectDefinition.createBoolean(key, "Whether the item is active")

            val trueValue = AspectValue.create("true").getOrElse { error("Invalid value") }
            val falseValue = AspectValue.create("false").getOrElse { error("Invalid value") }

            // false < true in Kotlin Boolean comparison
            definition.compareValues(falseValue, trueValue) shouldBe -1
            definition.compareValues(trueValue, falseValue) shouldBe 1
            definition.compareValues(trueValue, trueValue) shouldBe 0
            definition.compareValues(falseValue, falseValue) shouldBe 0
        }

        "Boolean DSL should create correct definitions" {
            val definitions = aspectDefinitions {
                boolean("blocked") {
                    description = "Whether the item is blocked"
                }
            }

            definitions shouldHaveSize 1
            val definition = definitions.first()

            definition.key.value shouldBe "blocked"
            definition.description shouldBe "Whether the item is blocked"
            definition.type shouldBe AspectType.BooleanType
        }

        "Boolean DSL with rules should work correctly" {
            val definitions = aspectDefinitionsWithRules {
                boolean("is_urgent") {
                    description = "Whether the item is urgent"
                    rules {
                        required("Urgency must be specified for high priority items") {
                            aspectEquals("priority", "high")
                        }
                    }
                }
            }

            definitions shouldHaveSize 1
            val definitionWithRules = definitions.first()

            definitionWithRules.definition.key.value shouldBe "is_urgent"
            definitionWithRules.definition.type shouldBe AspectType.BooleanType
            definitionWithRules.rules shouldHaveSize 1

            // Test conditional requirement
            val priorityKey = AspectKey.create("priority").getOrElse { error("Invalid key") }
            val highPriorityValue = AspectValue.create("high").getOrElse { error("Invalid value") }
            val mediumPriorityValue = AspectValue.create("medium").getOrElse { error("Invalid value") }
            val trueValue = AspectValue.create("true").getOrElse { error("Invalid value") }

            // When priority is high, is_urgent is required
            val aspectsHighPriority = mapOf(priorityKey to nonEmptyListOf(highPriorityValue))
            definitionWithRules.isValidWithRules(null, aspectsHighPriority) shouldBe false
            definitionWithRules.isValidWithRules(trueValue, aspectsHighPriority) shouldBe true

            // When priority is medium, is_urgent is not required
            val aspectsMediumPriority = mapOf(priorityKey to nonEmptyListOf(mediumPriorityValue))
            definitionWithRules.isValidWithRules(null, aspectsMediumPriority) shouldBe true
            definitionWithRules.isValidWithRules(trueValue, aspectsMediumPriority) shouldBe true
        }

        "AspectDefinitionDefaults should use Boolean type for blocked" {
            val defaults = AspectDefinitionDefaults.all()

            val blockedDef = defaults.find { it.key.value == "blocked" }
            blockedDef shouldNotBe null
            blockedDef?.type shouldBe AspectType.BooleanType
            blockedDef?.description shouldBe "Whether the item is blocked"
        }
    })
