package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.getOrElse
import io.github.kamiazya.scopes.interfaces.cli.helpers.TestPortImplementations
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for AspectQueryAdapter.
 * Tests aspect query operations (get, list, validate) using real implementations.
 */
class AspectQueryAdapterIntegrationTest :
    DescribeSpec({

        describe("AspectQueryAdapter Integration Tests") {

            lateinit var queryAdapter: AspectQueryAdapter
            lateinit var commandAdapter: AspectCommandAdapter

            beforeEach {
                val testEnvironment = TestPortImplementations.createTestEnvironment()
                queryAdapter = AspectQueryAdapter(
                    aspectQueryPort = testEnvironment.aspectQueryPort,
                )
                commandAdapter = AspectCommandAdapter(
                    aspectCommandPort = testEnvironment.aspectCommandPort,
                )
            }

            describe("Getting Aspect Definitions") {

                context("successful aspect retrieval") {

                    it("should get an existing aspect definition") {
                        runBlocking {
                            // Arrange - create an aspect
                            commandAdapter.defineAspect(
                                key = "priority",
                                description = "Task priority level",
                                type = "text",
                            ).shouldBeRight()

                            // Act - get the aspect definition
                            val result = queryAdapter.getAspectDefinition("priority")

                            // Assert
                            result.shouldBeRight()
                            val aspectDefinition = result.getOrElse { error("Test failed to get result") }
                            aspectDefinition shouldNotBe null
                            aspectDefinition?.key shouldBe "priority"
                            aspectDefinition?.description shouldBe "Task priority level"
                            aspectDefinition?.type shouldBe "text"
                            aspectDefinition?.createdAt shouldNotBe null
                            aspectDefinition?.updatedAt shouldNotBe null
                        }
                    }

                    it("should get multiple different aspect definitions") {
                        runBlocking {
                            // Arrange - create multiple aspects
                            commandAdapter.defineAspect("status", "Task status", "text").shouldBeRight()
                            commandAdapter.defineAspect("completed", "Is completed", "boolean").shouldBeRight()
                            commandAdapter.defineAspect("estimate", "Effort estimate", "numeric").shouldBeRight()

                            // Act - get each aspect
                            val statusResult = queryAdapter.getAspectDefinition("status")
                            val completedResult = queryAdapter.getAspectDefinition("completed")
                            val estimateResult = queryAdapter.getAspectDefinition("estimate")

                            // Assert
                            statusResult.shouldBeRight()
                            completedResult.shouldBeRight()
                            estimateResult.shouldBeRight()

                            val statusAspect = statusResult.getOrElse { error("Failed") }!!
                            val completedAspect = completedResult.getOrElse { error("Failed") }!!
                            val estimateAspect = estimateResult.getOrElse { error("Failed") }!!

                            statusAspect.key shouldBe "status"
                            statusAspect.type shouldBe "text"

                            completedAspect.key shouldBe "completed"
                            completedAspect.type shouldBe "boolean"

                            estimateAspect.key shouldBe "estimate"
                            estimateAspect.type shouldBe "numeric"
                        }
                    }

                    it("should reflect updated aspect definitions") {
                        runBlocking {
                            // Arrange - create and update aspect
                            commandAdapter.defineAspect("update-test", "Original", "text").shouldBeRight()
                            commandAdapter.updateAspectDefinition("update-test", "Updated description").shouldBeRight()

                            // Act - get the updated aspect
                            val result = queryAdapter.getAspectDefinition("update-test")

                            // Assert
                            result.shouldBeRight()
                            val aspect = result.getOrElse { error("Failed") }!!
                            aspect.description shouldBe "Updated description"
                            aspect.updatedAt shouldNotBe aspect.createdAt
                        }
                    }
                }

                context("error cases") {

                    it("should return null for non-existent aspect") {
                        runBlocking {
                            // Act - try to get non-existent aspect
                            val result = queryAdapter.getAspectDefinition("non-existent")

                            // Assert
                            result.shouldBeRight()
                            val aspect = result.getOrElse { error("Failed") }
                            aspect shouldBe null
                        }
                    }

                    it("should fail with invalid aspect key format") {
                        runBlocking {
                            // Act - try to get with invalid key
                            val result = queryAdapter.getAspectDefinition("invalid key format")

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should return null for deleted aspect") {
                        runBlocking {
                            // Arrange - create and delete aspect
                            commandAdapter.defineAspect("delete-test", "To be deleted", "text").shouldBeRight()
                            commandAdapter.deleteAspectDefinition("delete-test").shouldBeRight()

                            // Act - try to get deleted aspect
                            val result = queryAdapter.getAspectDefinition("delete-test")

                            // Assert
                            result.shouldBeRight()
                            val aspect = result.getOrElse { error("Failed") }
                            aspect shouldBe null
                        }
                    }
                }
            }

            describe("Listing Aspect Definitions") {

                context("successful aspect listing") {

                    it("should list no aspects when none exist") {
                        runBlocking {
                            // Act - list aspects when none exist
                            val result = queryAdapter.listAspectDefinitions()

                            // Assert
                            result.shouldBeRight()
                            val aspects = result.getOrElse { error("Failed") }
                            aspects shouldHaveSize 0
                        }
                    }

                    it("should list single aspect definition") {
                        runBlocking {
                            // Arrange - create one aspect
                            commandAdapter.defineAspect("single", "Single aspect", "text").shouldBeRight()

                            // Act - list aspects
                            val result = queryAdapter.listAspectDefinitions()

                            // Assert
                            result.shouldBeRight()
                            val aspects = result.getOrElse { error("Failed") }
                            aspects shouldHaveSize 1
                            aspects.first().key shouldBe "single"
                            aspects.first().description shouldBe "Single aspect"
                            aspects.first().type shouldBe "text"
                        }
                    }

                    it("should list multiple aspect definitions") {
                        runBlocking {
                            // Arrange - create multiple aspects
                            val aspectsToCreate = mapOf(
                                "priority" to Pair("Task priority", "text"),
                                "assignee" to Pair("Person assigned", "text"),
                                "completed" to Pair("Is completed", "boolean"),
                                "estimate" to Pair("Time estimate", "numeric"),
                            )

                            for ((key, descAndType) in aspectsToCreate) {
                                commandAdapter.defineAspect(key, descAndType.first, descAndType.second).shouldBeRight()
                            }

                            // Act - list all aspects
                            val result = queryAdapter.listAspectDefinitions()

                            // Assert
                            result.shouldBeRight()
                            val aspects = result.getOrElse { error("Failed") }
                            aspects shouldHaveSize 4

                            val aspectKeys = aspects.map { it.key }
                            aspectKeys shouldContainExactlyInAnyOrder listOf("priority", "assignee", "completed", "estimate")

                            // Check specific aspects
                            val priorityAspect = aspects.find { it.key == "priority" }!!
                            priorityAspect.type shouldBe "text"
                            priorityAspect.description shouldBe "Task priority"

                            val assigneeAspect = aspects.find { it.key == "assignee" }!!
                            assigneeAspect.type shouldBe "text"
                            assigneeAspect.description shouldBe "Person assigned"
                        }
                    }

                    it("should list aspects after updates") {
                        runBlocking {
                            // Arrange - create and update aspects
                            commandAdapter.defineAspect("update-1", "Original 1", "text").shouldBeRight()
                            commandAdapter.defineAspect("update-2", "Original 2", "boolean").shouldBeRight()
                            commandAdapter.updateAspectDefinition("update-1", "Updated 1").shouldBeRight()

                            // Act - list aspects
                            val result = queryAdapter.listAspectDefinitions()

                            // Assert
                            result.shouldBeRight()
                            val aspects = result.getOrElse { error("Failed") }
                            aspects shouldHaveSize 2

                            val updated1 = aspects.find { it.key == "update-1" }!!
                            updated1.description shouldBe "Updated 1"

                            val unchanged2 = aspects.find { it.key == "update-2" }!!
                            unchanged2.description shouldBe "Original 2"
                        }
                    }

                    it("should list remaining aspects after deletion") {
                        runBlocking {
                            // Arrange - create multiple aspects, then delete some
                            commandAdapter.defineAspect("keep-1", "Keep this", "text").shouldBeRight()
                            commandAdapter.defineAspect("delete-me", "Delete this", "boolean").shouldBeRight()
                            commandAdapter.defineAspect("keep-2", "Keep this too", "numeric").shouldBeRight()

                            commandAdapter.deleteAspectDefinition("delete-me").shouldBeRight()

                            // Act - list remaining aspects
                            val result = queryAdapter.listAspectDefinitions()

                            // Assert
                            result.shouldBeRight()
                            val aspects = result.getOrElse { error("Failed") }
                            aspects shouldHaveSize 2

                            val aspectKeys = aspects.map { it.key }
                            aspectKeys shouldContainExactlyInAnyOrder listOf("keep-1", "keep-2")

                            // Verify deleted aspect is not in list
                            aspects.none { it.key == "delete-me" } shouldBe true
                        }
                    }
                }
            }

            describe("Validating Aspect Values") {

                context("successful aspect value validation") {

                    it("should validate single text value") {
                        runBlocking {
                            // Arrange - create text aspect
                            commandAdapter.defineAspect("text-aspect", "Text type", "text").shouldBeRight()

                            // Act - validate text value
                            val result = queryAdapter.validateAspectValue("text-aspect", listOf("some-text"))

                            // Assert
                            result.shouldBeRight()
                            val validatedValues = result.getOrElse { error("Failed") }
                            validatedValues shouldHaveSize 1
                            validatedValues.first() shouldBe "some-text"
                        }
                    }

                    it("should fail with multiple text values when allowMultiple is false") {
                        runBlocking {
                            // Arrange - create text aspect (allowMultiple defaults to false)
                            commandAdapter.defineAspect("multi-text", "Multiple text values", "text").shouldBeRight()

                            // Act - try to validate multiple values
                            val result = queryAdapter.validateAspectValue("multi-text", listOf("value1", "value2", "value3"))

                            // Assert - should fail because multiple values not allowed
                            result.shouldBeLeft()
                        }
                    }

                    it("should validate boolean values") {
                        runBlocking {
                            // Arrange - create boolean aspect
                            commandAdapter.defineAspect("bool-aspect", "Boolean type", "boolean").shouldBeRight()

                            // Act - validate boolean values
                            val trueResult = queryAdapter.validateAspectValue("bool-aspect", listOf("true"))
                            val falseResult = queryAdapter.validateAspectValue("bool-aspect", listOf("false"))

                            // Assert
                            trueResult.shouldBeRight()
                            falseResult.shouldBeRight()

                            val trueValues = trueResult.getOrElse { error("Failed") }
                            val falseValues = falseResult.getOrElse { error("Failed") }

                            trueValues.first() shouldBe "true"
                            falseValues.first() shouldBe "false"
                        }
                    }

                    it("should fail with multiple numeric values when allowMultiple is false") {
                        runBlocking {
                            // Arrange - create numeric aspect (allowMultiple defaults to false)
                            commandAdapter.defineAspect("numeric-aspect", "Numeric type", "numeric").shouldBeRight()

                            // Act - try to validate multiple values
                            val result = queryAdapter.validateAspectValue("numeric-aspect", listOf("42", "3.14", "0"))

                            // Assert - should fail because multiple values not allowed
                            result.shouldBeLeft()
                        }
                    }
                }

                context("validation error cases") {

                    it("should fail when aspect not found") {
                        runBlocking {
                            // Act - try to validate with non-existent aspect
                            val result = queryAdapter.validateAspectValue("non-existent", listOf("value"))

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid boolean values") {
                        runBlocking {
                            // Arrange - create boolean aspect
                            commandAdapter.defineAspect("bool-strict", "Boolean type", "boolean").shouldBeRight()

                            // Act - try to validate invalid boolean
                            val result = queryAdapter.validateAspectValue("bool-strict", listOf("maybe"))

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid numeric values") {
                        runBlocking {
                            // Arrange - create numeric aspect
                            commandAdapter.defineAspect("num-strict", "Numeric type", "numeric").shouldBeRight()

                            // Act - try to validate invalid numeric
                            val result = queryAdapter.validateAspectValue("num-strict", listOf("not-a-number"))

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with empty values list") {
                        runBlocking {
                            // Arrange - create aspect
                            commandAdapter.defineAspect("empty-test", "Test aspect", "text").shouldBeRight()

                            // Act - try to validate empty list
                            val result = queryAdapter.validateAspectValue("empty-test", emptyList())

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }
            }

            describe("Complete Query Workflow") {

                it("should support end-to-end aspect management workflow") {
                    runBlocking {
                        // Create multiple aspects for a project management scenario
                        val aspects = listOf(
                            Triple("priority", "Task priority level", "text"),
                            Triple("status", "Task status", "text"),
                            Triple("assignee", "Person assigned", "text"),
                            Triple("estimate", "Time estimate", "numeric"),
                            Triple("completed", "Is completed", "boolean"),
                        )

                        // Create all aspects
                        for ((key, description, type) in aspects) {
                            commandAdapter.defineAspect(key, description, type).shouldBeRight()
                        }

                        // List all aspects
                        val listResult = queryAdapter.listAspectDefinitions()
                        listResult.shouldBeRight()
                        val allAspects = listResult.getOrElse { error("Failed") }
                        allAspects shouldHaveSize 5

                        // Get specific aspects
                        val priorityResult = queryAdapter.getAspectDefinition("priority")
                        priorityResult.shouldBeRight()
                        val priority = priorityResult.getOrElse { error("Failed") }!!
                        priority.type shouldBe "text"

                        // Validate single value (since allowMultiple defaults to false)
                        val validationResult = queryAdapter.validateAspectValue("assignee", listOf("alice"))
                        validationResult.shouldBeRight()
                        val validatedAssignees = validationResult.getOrElse { error("Failed") }
                        validatedAssignees shouldHaveSize 1
                        validatedAssignees.first() shouldBe "alice"

                        // Update and verify
                        commandAdapter.updateAspectDefinition("priority", "Updated priority description").shouldBeRight()
                        val updatedResult = queryAdapter.getAspectDefinition("priority")
                        updatedResult.shouldBeRight()
                        val updated = updatedResult.getOrElse { error("Failed") }!!
                        updated.description shouldBe "Updated priority description"

                        // Delete and verify removal
                        commandAdapter.deleteAspectDefinition("completed").shouldBeRight()
                        val finalListResult = queryAdapter.listAspectDefinitions()
                        finalListResult.shouldBeRight()
                        val finalAspects = finalListResult.getOrElse { error("Failed") }
                        finalAspects shouldHaveSize 4
                        finalAspects.none { it.key == "completed" } shouldBe true
                    }
                }
            }

            describe("Edge Cases") {

                it("should handle aspect listing order consistency") {
                    runBlocking {
                        // Create aspects in specific order
                        commandAdapter.defineAspect("z-last", "Last", "text").shouldBeRight()
                        commandAdapter.defineAspect("a-first", "First", "text").shouldBeRight()
                        commandAdapter.defineAspect("m-middle", "Middle", "text").shouldBeRight()

                        // List multiple times
                        val result1 = queryAdapter.listAspectDefinitions()
                        val result2 = queryAdapter.listAspectDefinitions()

                        // Assert both successful and consistent
                        result1.shouldBeRight()
                        result2.shouldBeRight()

                        val aspects1 = result1.getOrElse { error("Failed") }
                        val aspects2 = result2.getOrElse { error("Failed") }

                        aspects1.map { it.key } shouldBe aspects2.map { it.key }
                    }
                }
            }
        }
    })
