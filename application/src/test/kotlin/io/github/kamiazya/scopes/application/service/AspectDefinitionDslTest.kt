package io.github.kamiazya.scopes.application.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldHaveSize
import io.github.kamiazya.scopes.domain.valueobject.AspectType

class AspectDefinitionDslTest : StringSpec({

    "DSL should create ordered aspect definition" {
        val definitions = aspectDefinitions {
            ordered("priority") {
                description = "Task priority level"
                values("low", "medium", "high", "critical")
            }
        }

        definitions shouldHaveSize 1
        val definition = definitions.first()

        definition.key.value shouldBe "priority"
        definition.description shouldBe "Task priority level"
        (definition.type is AspectType.Ordered) shouldBe true

        val orderedType = definition.type as AspectType.Ordered
        orderedType.allowedValues shouldHaveSize 4
        orderedType.allowedValues.map { it.value } shouldBe listOf("low", "medium", "high", "critical")
    }

    "DSL should create numeric aspect definition" {
        val definitions = aspectDefinitions {
            numeric("effort") {
                description = "Estimated effort in hours"
            }
        }

        definitions shouldHaveSize 1
        val definition = definitions.first()

        definition.key.value shouldBe "effort"
        definition.description shouldBe "Estimated effort in hours"
        definition.type shouldBe AspectType.Numeric
    }

    "DSL should create text aspect definition" {
        val definitions = aspectDefinitions {
            text("comment") {
                description = "Additional comments"
            }
        }

        definitions shouldHaveSize 1
        val definition = definitions.first()

        definition.key.value shouldBe "comment"
        definition.description shouldBe "Additional comments"
        definition.type shouldBe AspectType.Text
    }

    "DSL should create multiple aspect definitions" {
        val definitions = aspectDefinitions {
            ordered("priority") {
                description = "Task priority level"
                values("low", "medium", "high")
            }

            numeric("effort") {
                description = "Estimated effort"
            }

            text("comment") {
                description = "Comments"
            }
        }

        definitions shouldHaveSize 3
        definitions.map { it.key.value } shouldBe listOf("priority", "effort", "comment")
    }

    "AspectDefinitionDefaults should work with DSL" {
        val defaults = AspectDefinitionDefaults.all()

        defaults shouldNotBe emptyList<Any>()
        defaults shouldHaveSize 8

        val priorityDef = defaults.find { it.key.value == "priority" }
        priorityDef shouldNotBe null
        priorityDef?.description shouldBe "Task priority level"

        val effortDef = defaults.find { it.key.value == "effort" }
        effortDef shouldNotBe null
        effortDef?.type shouldBe AspectType.Numeric
    }
})
