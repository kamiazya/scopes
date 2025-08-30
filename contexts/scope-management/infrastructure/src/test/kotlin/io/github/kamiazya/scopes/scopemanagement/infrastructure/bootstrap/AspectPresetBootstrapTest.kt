package io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap

import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class AspectPresetBootstrapTest :
    DescribeSpec({
        describe("AspectPresetBootstrap") {
            val mockRepository = mockk<AspectDefinitionRepository>()
            val mockLogger = mockk<Logger>(relaxed = true)
            val bootstrap = AspectPresetBootstrap(mockRepository, mockLogger)

            describe("initialize") {
                context("when no presets exist") {
                    it("should create all standard presets") {
                        // Arrange
                        coEvery { mockRepository.findByKey(any()) } returns null.right()
                        val savedDefinitions = mutableListOf<AspectDefinition>()
                        coEvery { mockRepository.save(capture(savedDefinitions)) } answers {
                            savedDefinitions.last().right()
                        }

                        // Act
                        val result = bootstrap.initialize()

                        // Assert
                        result.isRight() shouldBe true
                        savedDefinitions.size shouldBe 3

                        // Verify priority preset
                        val priorityDef = savedDefinitions.find { it.key.value == "priority" }!!
                        val expectedValues = listOf(
                            AspectValue.create("low").getOrNull()!!,
                            AspectValue.create("medium").getOrNull()!!,
                            AspectValue.create("high").getOrNull()!!,
                        )
                        priorityDef.type shouldBe AspectType.Ordered(expectedValues)
                        priorityDef.description shouldBe "Task priority level"
                        priorityDef.allowMultiple shouldBe false

                        // Verify status preset
                        val statusDef = savedDefinitions.find { it.key.value == "status" }!!
                        statusDef.type shouldBe AspectType.Text
                        statusDef.description shouldBe "Task status"
                        statusDef.allowMultiple shouldBe false

                        // Verify type preset
                        val typeDef = savedDefinitions.find { it.key.value == "type" }!!
                        typeDef.type shouldBe AspectType.Text
                        typeDef.description shouldBe "Task type classification"
                        typeDef.allowMultiple shouldBe false

                        // Verify logging
                        coVerify { mockLogger.info("Initializing standard aspect presets") }
                        coVerify { mockLogger.info("Created aspect preset: priority") }
                        coVerify { mockLogger.info("Created aspect preset: status") }
                        coVerify { mockLogger.info("Created aspect preset: type") }
                        coVerify { mockLogger.info("Aspect preset initialization completed") }
                    }
                }

                context("when some presets already exist") {
                    it("should only create missing presets") {
                        // Arrange
                        val existingPriority = AspectDefinition.createText(
                            key = AspectKey.create("priority").getOrNull()!!,
                            description = "Existing priority",
                            allowMultiple = false,
                        )

                        coEvery { mockRepository.findByKey(AspectKey.create("priority").getOrNull()!!) } returns existingPriority.right()
                        coEvery { mockRepository.findByKey(AspectKey.create("status").getOrNull()!!) } returns null.right()
                        coEvery { mockRepository.findByKey(AspectKey.create("type").getOrNull()!!) } returns null.right()

                        val savedDefinitions = mutableListOf<AspectDefinition>()
                        coEvery { mockRepository.save(capture(savedDefinitions)) } answers {
                            savedDefinitions.last().right()
                        }

                        // Act
                        val result = bootstrap.initialize()

                        // Assert
                        result.isRight() shouldBe true
                        savedDefinitions.size shouldBe 2 // Only status and type should be saved
                        savedDefinitions.none { it.key.value == "priority" } shouldBe true
                        savedDefinitions.any { it.key.value == "status" } shouldBe true
                        savedDefinitions.any { it.key.value == "type" } shouldBe true

                        // Verify logging
                        coVerify { mockLogger.debug("Aspect preset already exists: priority") }
                        coVerify { mockLogger.info("Created aspect preset: status") }
                        coVerify { mockLogger.info("Created aspect preset: type") }
                    }
                }
            }

            describe("companion object") {
                it("should provide standard preset values") {
                    AspectPresetBootstrap.PRIORITY_VALUES shouldBe listOf("low", "medium", "high")
                    AspectPresetBootstrap.STATUS_VALUES shouldBe listOf("todo", "ready", "in-progress", "blocked", "done")
                    AspectPresetBootstrap.TYPE_VALUES shouldBe listOf("feature", "bug", "chore", "doc")
                }
            }
        }
    })
