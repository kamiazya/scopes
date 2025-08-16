package io.github.kamiazya.scopes.application.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldContain
import io.github.kamiazya.scopes.domain.valueobject.AspectType
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.domain.entity.Scope
import arrow.core.getOrElse
import arrow.core.nonEmptyListOf

class MultipleValueAspectTest : StringSpec({

    "AspectDefinition should support allowMultiple flag" {
        val key = AspectKey.create("tags").getOrElse { error("Invalid key") }
        val values = listOf("frontend", "backend", "mobile").map { 
            AspectValue.create(it).getOrElse { error("Invalid value") }
        }
        
        val singleValueDef = AspectDefinition.createOrdered(
            key = key,
            allowedValues = values,
            description = "Tags",
            allowMultiple = false
        ).getOrElse { error("Failed to create definition") }
        
        val multiValueDef = AspectDefinition.createOrdered(
            key = key,
            allowedValues = values,
            description = "Tags",
            allowMultiple = true
        ).getOrElse { error("Failed to create definition") }
        
        singleValueDef.allowMultiple shouldBe false
        multiValueDef.allowMultiple shouldBe true
    }

    "DSL should support allowMultiple flag" {
        val definitions = aspectDefinitions {
            ordered("tags") {
                description = "Categorization tags"
                allowMultiple = true
                values("frontend", "backend", "mobile")
            }
        }

        definitions shouldHaveSize 1
        val definition = definitions.first()
        
        definition.key.value shouldBe "tags"
        definition.allowMultiple shouldBe true
        definition.type.shouldBeInstanceOf<AspectType.Ordered>()
    }

    "Scope should handle multiple aspect values" {
        val tagKey = AspectKey.create("tags").getOrElse { error("Invalid key") }
        val frontendValue = AspectValue.create("frontend").getOrElse { error("Invalid value") }
        val backendValue = AspectValue.create("backend").getOrElse { error("Invalid value") }
        
        val scope = Scope.create(
            title = "Test Scope",
            aspectsData = mapOf(tagKey to nonEmptyListOf(frontendValue, backendValue))
        ).getOrElse { error("Failed to create scope") }
        
        val aspectsMap = scope.getAspects()
        aspectsMap.size shouldBe 1
        aspectsMap[tagKey] shouldNotBe null
        aspectsMap[tagKey]!! shouldHaveSize 2
        aspectsMap[tagKey]!! shouldContain frontendValue
        aspectsMap[tagKey]!! shouldContain backendValue
    }

    "Scope should provide convenience methods for single and multiple values" {
        val tagKey = AspectKey.create("tags").getOrElse { error("Invalid key") }
        val priorityKey = AspectKey.create("priority").getOrElse { error("Invalid key") }
        
        val frontendValue = AspectValue.create("frontend").getOrElse { error("Invalid value") }
        val backendValue = AspectValue.create("backend").getOrElse { error("Invalid value") }
        val highValue = AspectValue.create("high").getOrElse { error("Invalid value") }
        
        val scope = Scope.create(
            title = "Test Scope"
        ).getOrElse { error("Failed to create scope") }
            .setAspect(tagKey, nonEmptyListOf(frontendValue, backendValue))
            .setAspect(priorityKey, highValue) // Single value convenience method
        
        // Test multiple values
        val tagValues = scope.getAspectValues(tagKey)
        tagValues shouldNotBe null
        tagValues!! shouldHaveSize 2
        tagValues shouldContain frontendValue
        tagValues shouldContain backendValue
        
        // Test single value (first of multiple)
        val firstTag = scope.getAspectValue(tagKey)
        firstTag shouldBe frontendValue
        
        // Test single value
        val priority = scope.getAspectValue(priorityKey)
        priority shouldBe highValue
        
        val priorityValues = scope.getAspectValues(priorityKey)
        priorityValues shouldNotBe null
        priorityValues!! shouldHaveSize 1
        priorityValues.first() shouldBe highValue
    }

    "Default aspects should include multiple value support" {
        val defaults = AspectDefinitionDefaults.all()
        
        val tagsDef = defaults.find { it.key.value == "tags" }
        tagsDef shouldNotBe null
        tagsDef?.allowMultiple shouldBe true
        tagsDef?.type.shouldBeInstanceOf<AspectType.Ordered>()
        
        // Other definitions should be single value by default
        val priorityDef = defaults.find { it.key.value == "priority" }
        priorityDef shouldNotBe null
        priorityDef?.allowMultiple shouldBe false
    }

    "AspectDefinitionWithRules should validate multiple values correctly" {
        val definitions = aspectDefinitionsWithRules {
            ordered("skills") {
                description = "Required skills"
                allowMultiple = true
                values("kotlin", "react", "docker", "aws")
                rules {
                    required("Skills are required for development tasks") {
                        aspectEquals("type", "development")
                    }
                }
            }
        }

        val definitionWithRules = definitions.first()
        definitionWithRules.definition.allowMultiple shouldBe true
        
        val typeKey = AspectKey.create("type").getOrElse { error("Invalid key") }
        val developmentValue = AspectValue.create("development").getOrElse { error("Invalid value") }
        val kotlinValue = AspectValue.create("kotlin").getOrElse { error("Invalid value") }
        val reactValue = AspectValue.create("react").getOrElse { error("Invalid value") }
        
        // Test with development type - skills should be required
        val devAspects = mapOf(typeKey to nonEmptyListOf(developmentValue))
        val noSkillsResult = definitionWithRules.validateRules(emptyList(), devAspects)
        noSkillsResult.any { it is io.github.kamiazya.scopes.domain.valueobject.RuleValidationResult.Invalid } shouldBe true
        
        val withSkillsResult = definitionWithRules.validateRules(nonEmptyListOf(kotlinValue, reactValue), devAspects)
        withSkillsResult.all { it is io.github.kamiazya.scopes.domain.valueobject.RuleValidationResult.Valid } shouldBe true
    }
})