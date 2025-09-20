package io.github.kamiazya.scopes.interfaces.cli.adapters

import io.github.kamiazya.scopes.interfaces.cli.helpers.TestPortImplementations
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import kotlinx.coroutines.runBlocking

/**
 * Integration tests for AspectCommandAdapter.
 * Tests aspect definition operations (create, update, delete) using real implementations.
 */
class AspectCommandAdapterIntegrationTest :
    DescribeSpec({

        describe("AspectCommandAdapter Integration Tests") {

            lateinit var adapter: AspectCommandAdapter

            beforeEach {
                val testEnvironment = TestPortImplementations.createTestEnvironment()
                adapter = AspectCommandAdapter(
                    aspectCommandPort = testEnvironment.aspectCommandPort,
                )
            }

            describe("Defining Aspects") {

                context("successful aspect definition") {

                    it("should create a text aspect definition") {
                        runBlocking {
                            // Act - define a text aspect
                            val result = adapter.defineAspect(
                                key = "priority",
                                description = "Task priority level",
                                type = "text",
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should create a boolean aspect definition") {
                        runBlocking {
                            // Act - define a boolean aspect
                            val result = adapter.defineAspect(
                                key = "completed",
                                description = "Whether task is completed",
                                type = "boolean",
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should create a numeric aspect definition") {
                        runBlocking {
                            // Act - define a numeric aspect
                            val result = adapter.defineAspect(
                                key = "estimate",
                                description = "Estimated effort in hours",
                                type = "numeric",
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should create a duration aspect definition") {
                        runBlocking {
                            // Act - define a duration aspect
                            val result = adapter.defineAspect(
                                key = "duration",
                                description = "Task duration",
                                type = "duration",
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should create multiple different aspects") {
                        runBlocking {
                            // Act - define multiple aspects
                            val result1 = adapter.defineAspect(
                                key = "assignee",
                                description = "Person assigned to task",
                                type = "text",
                            )
                            val result2 = adapter.defineAspect(
                                key = "deadline",
                                description = "Task deadline",
                                type = "duration",
                            )
                            val result3 = adapter.defineAspect(
                                key = "category",
                                description = "Task category",
                                type = "text",
                            )

                            // Assert
                            result1.shouldBeRight()
                            result2.shouldBeRight()
                            result3.shouldBeRight()
                        }
                    }
                }

                context("error cases") {

                    it("should fail when aspect key already exists") {
                        runBlocking {
                            // Arrange - create an aspect
                            adapter.defineAspect(
                                key = "existing-aspect",
                                description = "An existing aspect",
                                type = "text",
                            ).shouldBeRight()

                            // Act - try to create aspect with same key
                            val result = adapter.defineAspect(
                                key = "existing-aspect",
                                description = "Another aspect with same key",
                                type = "boolean",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid aspect key format") {
                        runBlocking {
                            // Act - try to create aspect with invalid key
                            val result = adapter.defineAspect(
                                key = "invalid key with spaces",
                                description = "Invalid key format",
                                type = "text",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with empty aspect key") {
                        runBlocking {
                            // Act - try to create aspect with empty key
                            val result = adapter.defineAspect(
                                key = "",
                                description = "Empty key test",
                                type = "text",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid aspect type") {
                        runBlocking {
                            // Act - try to create aspect with invalid type
                            val result = adapter.defineAspect(
                                key = "test-aspect",
                                description = "Test aspect",
                                type = "invalid-type",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }
            }

            describe("Updating Aspect Definitions") {

                context("successful aspect update") {

                    it("should update aspect description") {
                        runBlocking {
                            // Arrange - create an aspect
                            adapter.defineAspect(
                                key = "update-test",
                                description = "Original description",
                                type = "text",
                            ).shouldBeRight()

                            // Act - update the description
                            val result = adapter.updateAspectDefinition(
                                key = "update-test",
                                description = "Updated description",
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should update aspect description to null") {
                        runBlocking {
                            // Arrange - create an aspect
                            adapter.defineAspect(
                                key = "nullable-test",
                                description = "Original description",
                                type = "text",
                            ).shouldBeRight()

                            // Act - update description to null
                            val result = adapter.updateAspectDefinition(
                                key = "nullable-test",
                                description = null,
                            )

                            // Assert
                            result.shouldBeRight()
                        }
                    }

                    it("should update multiple aspects") {
                        runBlocking {
                            // Arrange - create multiple aspects
                            adapter.defineAspect("multi-1", "Description 1", "text").shouldBeRight()
                            adapter.defineAspect("multi-2", "Description 2", "boolean").shouldBeRight()
                            adapter.defineAspect("multi-3", "Description 3", "numeric").shouldBeRight()

                            // Act - update all descriptions
                            val result1 = adapter.updateAspectDefinition("multi-1", "Updated 1")
                            val result2 = adapter.updateAspectDefinition("multi-2", "Updated 2")
                            val result3 = adapter.updateAspectDefinition("multi-3", "Updated 3")

                            // Assert
                            result1.shouldBeRight()
                            result2.shouldBeRight()
                            result3.shouldBeRight()
                        }
                    }
                }

                context("error cases") {

                    it("should fail when aspect not found") {
                        runBlocking {
                            // Act - try to update non-existent aspect
                            val result = adapter.updateAspectDefinition(
                                key = "non-existent",
                                description = "New description",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid aspect key format") {
                        runBlocking {
                            // Act - try to update with invalid key
                            val result = adapter.updateAspectDefinition(
                                key = "invalid key format",
                                description = "New description",
                            )

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }
            }

            describe("Deleting Aspect Definitions") {

                context("successful aspect deletion") {

                    it("should delete an aspect definition") {
                        runBlocking {
                            // Arrange - create an aspect
                            adapter.defineAspect(
                                key = "delete-test",
                                description = "Test aspect for deletion",
                                type = "text",
                            ).shouldBeRight()

                            // Act - delete the aspect
                            val result = adapter.deleteAspectDefinition("delete-test")

                            // Assert
                            result.shouldBeRight()

                            // Verify deletion by trying to update (should fail)
                            val verifyResult = adapter.updateAspectDefinition(
                                key = "delete-test",
                                description = "Should fail",
                            )
                            verifyResult.shouldBeLeft()
                        }
                    }

                    it("should delete multiple aspects") {
                        runBlocking {
                            // Arrange - create multiple aspects
                            adapter.defineAspect("del-1", "Delete me 1", "text").shouldBeRight()
                            adapter.defineAspect("del-2", "Delete me 2", "boolean").shouldBeRight()
                            adapter.defineAspect("del-3", "Delete me 3", "numeric").shouldBeRight()

                            // Act - delete all aspects
                            val result1 = adapter.deleteAspectDefinition("del-1")
                            val result2 = adapter.deleteAspectDefinition("del-2")
                            val result3 = adapter.deleteAspectDefinition("del-3")

                            // Assert
                            result1.shouldBeRight()
                            result2.shouldBeRight()
                            result3.shouldBeRight()
                        }
                    }
                }

                context("error cases") {

                    it("should fail when aspect not found") {
                        runBlocking {
                            // Act - try to delete non-existent aspect
                            val result = adapter.deleteAspectDefinition("non-existent")

                            // Assert
                            result.shouldBeLeft()
                        }
                    }

                    it("should fail with invalid aspect key format") {
                        runBlocking {
                            // Act - try to delete with invalid key
                            val result = adapter.deleteAspectDefinition("invalid key format")

                            // Assert
                            result.shouldBeLeft()
                        }
                    }
                }
            }

            describe("Aspect Lifecycle Operations") {

                it("should handle complete aspect lifecycle") {
                    runBlocking {
                        val aspectKey = "lifecycle-test"

                        // Create
                        val createResult = adapter.defineAspect(
                            key = aspectKey,
                            description = "Test aspect lifecycle",
                            type = "text",
                        )
                        createResult.shouldBeRight()

                        // Update
                        val updateResult = adapter.updateAspectDefinition(
                            key = aspectKey,
                            description = "Updated lifecycle description",
                        )
                        updateResult.shouldBeRight()

                        // Delete
                        val deleteResult = adapter.deleteAspectDefinition(aspectKey)
                        deleteResult.shouldBeRight()

                        // Verify deletion
                        val verifyResult = adapter.updateAspectDefinition(
                            key = aspectKey,
                            description = "Should fail",
                        )
                        verifyResult.shouldBeLeft()
                    }
                }

                it("should allow recreating deleted aspects") {
                    runBlocking {
                        val aspectKey = "recreate-test"

                        // Create first time
                        adapter.defineAspect(aspectKey, "First time", "text").shouldBeRight()

                        // Delete
                        adapter.deleteAspectDefinition(aspectKey).shouldBeRight()

                        // Recreate with different type
                        val recreateResult = adapter.defineAspect(
                            key = aspectKey,
                            description = "Recreated",
                            type = "boolean",
                        )
                        recreateResult.shouldBeRight()
                    }
                }
            }

            describe("Aspect Usage Scenarios") {

                it("should support typical project management aspects") {
                    runBlocking {
                        // Create common project management aspects
                        val aspects = mapOf(
                            "priority" to Pair("Task priority level", "text"),
                            "status" to Pair("Task completion status", "text"),
                            "assignee" to Pair("Person responsible for task", "text"),
                            "estimate" to Pair("Estimated effort in hours", "numeric"),
                            "completed" to Pair("Whether task is done", "boolean"),
                            "blocked" to Pair("Whether task is blocked", "boolean"),
                            "category" to Pair("Task category or type", "text"),
                            "milestone" to Pair("Associated milestone", "text"),
                        )

                        // Create all aspects
                        for ((key, descAndType) in aspects) {
                            val result = adapter.defineAspect(
                                key = key,
                                description = descAndType.first,
                                type = descAndType.second,
                            )
                            result.shouldBeRight()
                        }
                    }
                }
            }
        }
    })
