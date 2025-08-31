package io.github.kamiazya.scopes.apps.cli.integration

import io.github.kamiazya.scopes.scopemanagement.application.command.DefineAspectUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.DeleteAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.GetAspectDefinitionUseCase
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAspectDefinitionsUseCase
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
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
                        val command = DefineAspectUseCase.Command(
                            key = "description",
                            description = "Task description",
                            type = AspectType.Text,
                        )
                        val result = defineAspectUseCase(command)

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
                        val command = DefineAspectUseCase.Command(
                            key = "estimatedHours",
                            description = "Estimated hours",
                            type = AspectType.Numeric,
                        )
                        val result = defineAspectUseCase(command)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { definition ->
                            definition.key.value shouldBe "estimatedHours"
                            definition.type shouldBe AspectType.Numeric
                        }
                    }
                }

                it("should define a boolean aspect") {
                    runTest {
                        // Act
                        val command = DefineAspectUseCase.Command(
                            key = "isCompleted",
                            description = "Completion status",
                            type = AspectType.BooleanType,
                        )
                        val result = defineAspectUseCase(command)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { definition ->
                            definition.key.value shouldBe "isCompleted"
                            definition.type shouldBe AspectType.BooleanType
                        }
                    }
                }

                it("should define an ordered aspect") {
                    runTest {
                        // Arrange
                        val values = listOf("low", "medium", "high").map {
                            AspectValue.create(it).getOrNull()!!
                        }

                        // Act
                        val command = DefineAspectUseCase.Command(
                            key = "priority",
                            description = "Task priority",
                            type = AspectType.Ordered(values),
                        )
                        val result = defineAspectUseCase(command)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { definition ->
                            definition.key.value shouldBe "priority"
                            val orderedType = definition.type as AspectType.Ordered
                            orderedType.allowedValues shouldContainExactlyInAnyOrder values
                        }
                    }
                }

                it("should define a duration aspect") {
                    runTest {
                        // Act
                        val command = DefineAspectUseCase.Command(
                            key = "timeSpent",
                            description = "Time spent on task",
                            type = AspectType.Duration,
                        )
                        val result = defineAspectUseCase(command)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { definition ->
                            definition.key.value shouldBe "timeSpent"
                            definition.type shouldBe AspectType.Duration
                        }
                    }
                }

                it("should prevent duplicate aspect definitions") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("status", "Task status", AspectType.Text))

                        // Act
                        val result = defineAspectUseCase(DefineAspectUseCase.Command("status", "Another status", AspectType.Numeric))

                        // Assert
                        result.shouldBeLeft()
                    }
                }

                it("should retrieve an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("category", "Task category", AspectType.Text))

                        // Act
                        val query = GetAspectDefinitionUseCase.Query("category")
                        val result = getAspectDefinitionUseCase(query)

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
                        defineAspectUseCase(DefineAspectUseCase.Command("label", "Task label", AspectType.Text))

                        // Act
                        val command = UpdateAspectDefinitionUseCase.Command(
                            key = "label",
                            description = "Updated task label",
                        )
                        val result = updateAspectDefinitionUseCase(command)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { updated ->
                            updated.description shouldBe "Updated task label"
                        }
                    }
                }

                it("should delete an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("temp", "Temporary aspect", AspectType.Text))

                        // Act
                        val command = DeleteAspectDefinitionUseCase.Command("temp")
                        val result = deleteAspectDefinitionUseCase(command)

                        // Assert
                        result.shouldBeRight()

                        // Verify deletion
                        val getResult = getAspectDefinitionUseCase(GetAspectDefinitionUseCase.Query("temp"))
                        getResult.shouldBeRight()
                        getResult.getOrNull() shouldBe null
                    }
                }

                it("should list all aspect definitions") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("aspect1", "First aspect", AspectType.Text))
                        defineAspectUseCase(DefineAspectUseCase.Command("aspect2", "Second aspect", AspectType.Numeric))
                        defineAspectUseCase(DefineAspectUseCase.Command("aspect3", "Third aspect", AspectType.BooleanType))

                        // Act
                        val query = ListAspectDefinitionsUseCase.Query()
                        val result = listAspectDefinitionsUseCase(query)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { definitions ->
                            definitions.size shouldBe 3
                            definitions.map { it.key.value } shouldContainExactlyInAnyOrder listOf("aspect1", "aspect2", "aspect3")
                        }
                    }
                }
            }

            describe("Aspect Value Validation") {
                it("should validate text values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("note", "Task note", AspectType.Text))

                        // Act
                        val query = ValidateAspectValueUseCase.Query("note", "This is a valid note")
                        val result = validateAspectValueUseCase(query)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.value shouldBe "This is a valid note"
                    }
                }

                it("should validate numeric values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("score", "Task score", AspectType.Numeric))

                        // Act
                        val validResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("score", "42.5"))
                        val invalidResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("score", "not a number"))

                        // Assert
                        validResult.shouldBeRight()
                        invalidResult.shouldBeLeft()
                    }
                }

                it("should validate boolean values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("active", "Is active", AspectType.BooleanType))

                        // Act
                        val trueResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("active", "true"))
                        val falseResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("active", "false"))
                        val yesResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("active", "yes"))
                        val invalidResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("active", "maybe"))

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
                        defineAspectUseCase(DefineAspectUseCase.Command("size", "Task size", AspectType.Ordered(sizes)))

                        // Act
                        val validResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("size", "medium"))
                        val invalidResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("size", "extra-large"))

                        // Assert
                        validResult.shouldBeRight()
                        invalidResult.shouldBeLeft()
                    }
                }

                it("should validate duration values") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("duration", "Task duration", AspectType.Duration))

                        // Act
                        val validResults = listOf(
                            validateAspectValueUseCase(ValidateAspectValueUseCase.Query("duration", "P1D")),
                            validateAspectValueUseCase(ValidateAspectValueUseCase.Query("duration", "PT2H30M")),
                            validateAspectValueUseCase(ValidateAspectValueUseCase.Query("duration", "P1W")),
                            validateAspectValueUseCase(ValidateAspectValueUseCase.Query("duration", "P2DT3H4M")),
                        )
                        val invalidResult = validateAspectValueUseCase(ValidateAspectValueUseCase.Query("duration", "2 hours"))

                        // Assert
                        validResults.forEach { it.shouldBeRight() }
                        invalidResult.shouldBeLeft()
                    }
                }

                it("should validate multiple values when allowed") {
                    runTest {
                        // Arrange
                        val tagsKey = AspectKey.create("tags").getOrNull() ?: error("Failed to create aspect key")
                        val tags = AspectDefinition.createText(
                            key = tagsKey,
                            description = "Task tags",
                            allowMultiple = true,
                        )
                        aspectDefinitionRepository.save(tags)

                        // Act
                        val query = ValidateAspectValueUseCase.MultipleQuery(
                            mapOf("tags" to listOf("frontend", "bug", "urgent")),
                        )
                        val result = validateAspectValueUseCase(query)

                        // Assert
                        result.shouldBeRight()
                        result.getOrNull()?.let { validated ->
                            val tagsKey = AspectKey.create("tags").getOrNull()!!
                            validated[tagsKey]?.size shouldBe 3
                        }
                    }
                }

                it("should reject multiple values when not allowed") {
                    runTest {
                        // Arrange
                        defineAspectUseCase(DefineAspectUseCase.Command("status", "Task status", AspectType.Text))

                        // Act
                        val query = ValidateAspectValueUseCase.MultipleQuery(
                            mapOf("status" to listOf("open", "closed")),
                        )
                        val result = validateAspectValueUseCase(query)

                        // Assert
                        result.shouldBeLeft()
                    }
                }
            }
        }
    })
