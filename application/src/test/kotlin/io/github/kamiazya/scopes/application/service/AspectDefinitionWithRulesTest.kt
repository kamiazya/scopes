package io.github.kamiazya.scopes.application.service

import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectType
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AspectDefinitionWithRulesTest :
    StringSpec({

        "AspectDefinitionWithRules should validate basic value compatibility" {
            val definitions = aspectDefinitionsWithRules {
                numeric("effort") {
                    description = "Estimated effort in hours"
                }
            }

            definitions shouldHaveSize 1
            val definitionWithRules = definitions.first()

            val validValue = AspectValue.create("10.5").getOrElse { error("Invalid value") }
            val invalidValue = AspectValue.create("not_a_number").getOrElse { error("Invalid value") }

            definitionWithRules.isValidValue(validValue) shouldBe true
            definitionWithRules.isValidValue(invalidValue) shouldBe false
        }

        "AspectDefinitionWithRules should validate range rules" {
            val definitions = aspectDefinitionsWithRules {
                numeric("effort") {
                    description = "Estimated effort in hours"
                    rules {
                        range(min = 0.5, max = 100.0, "Effort must be between 0.5 and 100 hours")
                    }
                }
            }

            val definitionWithRules = definitions.first()
            val validValue = AspectValue.create("10.5").getOrElse { error("Invalid value") }
            val tooSmallValue = AspectValue.create("0.1").getOrElse { error("Invalid value") }
            val tooLargeValue = AspectValue.create("150.0").getOrElse { error("Invalid value") }

            val allAspects = emptyMap<AspectKey, NonEmptyList<AspectValue>>()

            definitionWithRules.isValidWithRules(validValue, allAspects) shouldBe true
            definitionWithRules.isValidWithRules(tooSmallValue, allAspects) shouldBe false
            definitionWithRules.isValidWithRules(tooLargeValue, allAspects) shouldBe false

            val errorMessages = definitionWithRules.getValidationMessages(tooSmallValue, allAspects)
            errorMessages shouldHaveSize 1
            errorMessages.first() shouldBe "Effort must be between 0.5 and 100 hours"
        }

        "AspectDefinitionWithRules should validate conditional required rules" {
            val definitions = aspectDefinitionsWithRules {
                numeric("effort") {
                    description = "Estimated effort in hours"
                    rules {
                        required("Effort is required when status is in progress") {
                            aspectEquals("status", "in_progress")
                        }
                    }
                }
            }

            val definitionWithRules = definitions.first()
            val statusKey = AspectKey.create("status").getOrElse { error("Invalid key") }
            val inProgressValue = AspectValue.create("in_progress").getOrElse { error("Invalid value") }
            val todoValue = AspectValue.create("todo").getOrElse { error("Invalid value") }
            val effortValue = AspectValue.create("5.0").getOrElse { error("Invalid value") }

            // When status is in_progress, effort is required
            val aspectsInProgress = mapOf(statusKey to nonEmptyListOf(inProgressValue))
            definitionWithRules.isValidWithRules(null, aspectsInProgress) shouldBe false
            definitionWithRules.isValidWithRules(effortValue, aspectsInProgress) shouldBe true

            // When status is todo, effort is not required
            val aspectsTodo = mapOf(statusKey to nonEmptyListOf(todoValue))
            definitionWithRules.isValidWithRules(null, aspectsTodo) shouldBe true
            definitionWithRules.isValidWithRules(effortValue, aspectsTodo) shouldBe true
        }

        "AspectDefinitionWithRules should validate pattern rules" {
            val definitions = aspectDefinitionsWithRules {
                text("comment") {
                    description = "Additional comments"
                    rules {
                        pattern(
                            "^[A-Za-z0-9\\s,.!?-]+$",
                            "Comments must contain only alphanumeric characters and basic punctuation",
                        )
                    }
                }
            }

            val definitionWithRules = definitions.first()
            val validComment = AspectValue.create("This is a valid comment!").getOrElse { error("Invalid value") }
            val invalidComment = AspectValue.create("Invalid @#$% symbols").getOrElse { error("Invalid value") }

            val allAspects = emptyMap<AspectKey, NonEmptyList<AspectValue>>()

            definitionWithRules.isValidWithRules(validComment, allAspects) shouldBe true
            definitionWithRules.isValidWithRules(invalidComment, allAspects) shouldBe false

            val errorMessages = definitionWithRules.getValidationMessages(invalidComment, allAspects)
            errorMessages shouldHaveSize 1
            errorMessages.first() shouldBe "Comments must contain only alphanumeric characters and basic punctuation"
        }

        "AspectDefinitionDefaultsWithRules should provide defaults with rules" {
            val defaults = AspectDefinitionDefaultsWithRules.all()

            defaults.isNotEmpty() shouldBe true

            val effortDef = defaults.find { it.definition.key.value == "effort" }
            effortDef shouldNotBe null
            effortDef?.definition?.type shouldBe AspectType.Numeric
            effortDef?.rules?.isNotEmpty() shouldBe true

            // Test the range rule on effort
            val effortValue = AspectValue.create("10.0").getOrElse { error("Invalid value") }
            val tooLargeValue = AspectValue.create("150.0").getOrElse { error("Invalid value") }
            val allAspects = emptyMap<AspectKey, NonEmptyList<AspectValue>>()

            effortDef?.isValidWithRules(effortValue, allAspects) shouldBe true
            effortDef?.isValidWithRules(tooLargeValue, allAspects) shouldBe false
        }
    })
