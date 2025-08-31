package io.github.kamiazya.scopes.apps.cli.integration

import io.github.kamiazya.scopes.scopemanagement.application.command.DefineAspectUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.DeleteAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.GetAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAspectDefinitionsUseCase
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AspectValueValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction.NoopTransactionManager
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest

class AspectManagementIntegrationTest :
    DescribeSpec({
        describe("Aspect Management Integration") {
            lateinit var aspectDefinitionRepository: AspectDefinitionRepository
            lateinit var scopeRepository: ScopeRepository
            lateinit var transactionManager: NoopTransactionManager

            // Use cases
            lateinit var defineAspectUseCase: DefineAspectUseCase
            lateinit var getAspectDefinitionUseCase: GetAspectDefinitionUseCase
            lateinit var updateAspectDefinitionUseCase: UpdateAspectDefinitionUseCase
            lateinit var deleteAspectDefinitionUseCase: DeleteAspectDefinitionUseCase
            lateinit var listAspectDefinitionsUseCase: ListAspectDefinitionsUseCase
            lateinit var validateAspectValueUseCase: ValidateAspectValueUseCase
            lateinit var validationService: AspectValueValidationService

            beforeEach {
                // Initialize repositories
                aspectDefinitionRepository = InMemoryAspectDefinitionRepository()
                scopeRepository = InMemoryScopeRepository()
                transactionManager = NoopTransactionManager()

                // Initialize use cases
                defineAspectUseCase = DefineAspectUseCase(aspectDefinitionRepository, transactionManager)
                getAspectDefinitionUseCase = GetAspectDefinitionUseCase(aspectDefinitionRepository)
                updateAspectDefinitionUseCase = UpdateAspectDefinitionUseCase(aspectDefinitionRepository, transactionManager)
                deleteAspectDefinitionUseCase = DeleteAspectDefinitionUseCase(aspectDefinitionRepository, transactionManager)
                listAspectDefinitionsUseCase = ListAspectDefinitionsUseCase(aspectDefinitionRepository)
                validationService = AspectValueValidationService()
                validateAspectValueUseCase = ValidateAspectValueUseCase(aspectDefinitionRepository, validationService)
            }

            describe("Aspect Definition CRUD Operations") {
                it("should define a text aspect") {
                    runTest {
                        // Act
                        val result = defineAspectUseCase.execute(
                            key = "description",
                            description = "Task description",
                            type = AspectType.Text,
                        )

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { definition ->
                            definition.key.value shouldBe "description"
                            definition.type shouldBe AspectType.Text
                            definition.description shouldBe "Task description"
                        }
                    }
                }

                it("should define a numeric aspect") {
                    runTest {
                        // Act
                        val result = defineAspectUseCase.execute(
                            key = "estimatedHours",
                            description = "Estimated hours",
                            type = AspectType.Numeric,
                        )

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrNull()!!
                        definition.key.value shouldBe "estimatedHours"
                        definition.type shouldBe AspectType.Numeric
                    }
                }

                it("should define a boolean aspect") {
                    runTest {
                        // Act
                        val result = defineAspectUseCase.execute(
                            key = "isCompleted",
                            description = "Completion status",
                            type = AspectType.BooleanType,
                        )

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrNull()!!
                        definition.key.value shouldBe "isCompleted"
                        definition.type shouldBe AspectType.BooleanType
                    }
                }

                it("should define an ordered aspect") {
                    runTest {
                        // Arrange
                        val values = listOf("low", "medium", "high").map {
                            AspectValue.create(it).getOrNull()!!
                        }

                        // Act
                        val result = defineAspectUseCase.execute(
                            key = "priority",
                            description = "Task priority",
                            type = AspectType.Ordered(values),
                        )

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrNull()!!
                        definition.key.value shouldBe "priority"
                        val orderedType = definition.type as AspectType.Ordered
                        orderedType.allowedValues shouldContainExactlyInAnyOrder values
                    }
                }

                it("should define a duration aspect") {
                    runTest {
                        // Act
                        val result = defineAspectUseCase.execute(
                            key = "timeSpent",
                            description = "Time spent on task",
                            type = AspectType.Duration,
                        )

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrNull()!!
                        definition.key.value shouldBe "timeSpent"
                        definition.type shouldBe AspectType.Duration
                    }
                }

                it("should prevent duplicate aspect definitions") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("status", "Task status", AspectType.Text)

                        // Act
                        val result = defineAspectUseCase.execute("status", "Another status", AspectType.Numeric)

                        // Assert
                        result.shouldBeLeft()
                    }
                }

                it("should retrieve an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("category", "Task category", AspectType.Text)

                        // Act
                        val result = getAspectDefinitionUseCase.execute("category")

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrNull()
                        definition shouldNotBe null
                        definition?.key?.value shouldBe "category"
                    }
                }

                it("should update an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("label", "Task label", AspectType.Text)

                        // Act
                        val result = updateAspectDefinitionUseCase.execute(
                            key = "label",
                            description = "Updated task label",
                        )

                        // Assert
                        result.shouldBeRight()
                        val updated = result.getOrNull()!!
                        updated.description shouldBe "Updated task label"
                    }
                }

                it("should delete an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("temp", "Temporary aspect", AspectType.Text)

                        // Act
                        val result = deleteAspectDefinitionUseCase.execute("temp")

                        // Assert
                        result.shouldBeRight()

                        // Verify deletion
                        val getResult = getAspectDefinitionUseCase.execute("temp")
                        getResult.shouldBeRight()
                        getResult.getOrNull() shouldBe null
                    }
                }

                it("should list all aspect definitions") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("aspect1", "First aspect", AspectType.Text)
                        defineAspectUseCase.execute("aspect2", "Second aspect", AspectType.Numeric)
                        defineAspectUseCase.execute("aspect3", "Third aspect", AspectType.BooleanType)

                        // Act
                        val result = listAspectDefinitionsUseCase.execute()

                        // Assert
                        result.shouldBeRight()
                        val definitions = result.getOrNull()!!
                        definitions.size shouldBe 3
                        definitions.map { it.key.value } shouldContainExactlyInAnyOrder listOf("aspect1", "aspect2", "aspect3")
                    }
                }
            }

            describe("Aspect Value Validation") {
                it("should validate text values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("note", "Task note", AspectType.Text)

                        // Act
                        val result = validateAspectValueUseCase.execute("note", "This is a valid note")

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.value shouldBe "This is a valid note"
                    }
                }

                it("should validate numeric values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("score", "Task score", AspectType.Numeric)

                        // Act
                        val validResult = validateAspectValueUseCase.execute("score", "42.5")
                        val invalidResult = validateAspectValueUseCase.execute("score", "not a number")

                        // Assert
                        validResult.shouldBeRight()
                        invalidResult.shouldBeLeft()
                    }
                }

                it("should validate boolean values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("active", "Is active", AspectType.BooleanType)

                        // Act
                        val trueResult = validateAspectValueUseCase.execute("active", "true")
                        val falseResult = validateAspectValueUseCase.execute("active", "false")
                        val yesResult = validateAspectValueUseCase.execute("active", "yes")
                        val invalidResult = validateAspectValueUseCase.execute("active", "maybe")

                        // Assert
                        trueResult.shouldBeRight()
                        falseResult.shouldBeRight()
                        yesResult.shouldBeRight()
                        invalidResult.shouldBeLeft()
                    }
                }

                it("should validate ordered values") {
                    runTest {
                        // Arrange
                        val sizes = listOf("small", "medium", "large").map {
                            AspectValue.create(it).getOrNull()!!
                        }
                        defineAspectUseCase.execute("size", "Task size", AspectType.Ordered(sizes))

                        // Act
                        val validResult = validateAspectValueUseCase.execute("size", "medium")
                        val invalidResult = validateAspectValueUseCase.execute("size", "extra-large")

                        // Assert
                        validResult.shouldBeRight()
                        invalidResult.shouldBeLeft()
                    }
                }

                it("should validate duration values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("duration", "Task duration", AspectType.Duration)

                        // Act
                        val validResults = listOf(
                            validateAspectValueUseCase.execute("duration", "P1D"),
                            validateAspectValueUseCase.execute("duration", "PT2H30M"),
                            validateAspectValueUseCase.execute("duration", "P1W"),
                            validateAspectValueUseCase.execute("duration", "P2DT3H4M"),
                        )
                        val invalidResult = validateAspectValueUseCase.execute("duration", "2 hours")

                        // Assert
                        validResults.forEach { it.shouldBeRight() }
                        invalidResult.shouldBeLeft()
                    }
                }

                it("should validate multiple values when allowed") {
                    runTest {
                        // Arrange
                        val tags = AspectDefinition.createText(
                            key = AspectKey.create("tags").getOrNull()!!,
                            description = "Task tags",
                            allowMultiple = true,
                        )
                        aspectDefinitionRepository.save(tags)

                        // Act
                        val result = validateAspectValueUseCase.executeMultiple(
                            mapOf("tags" to listOf("frontend", "bug", "urgent")),
                        )

                        // Assert
                        result.shouldBeRight()
                        val validated = result.getOrNull()!!
                        validated["tags"]?.size shouldBe 3
                    }
                }

                it("should reject multiple values when not allowed") {
                    runTest {
                        // Arrange
                        defineAspectUseCase.execute("status", "Task status", AspectType.Text)

                        // Act
                        val result = validateAspectValueUseCase.executeMultiple(
                            mapOf("status" to listOf("open", "closed")),
                        )

                        // Assert
                        result.shouldBeLeft()
                    }
                }
            }
        }
    })
