package io.github.kamiazya.scopes.apps.cli.integration

import arrow.core.getOrElse
import io.github.kamiazya.scopes.platform.infrastructure.transaction.NoOpTransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.LogLevel
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DefineAspectCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DeleteAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.UpdateAspectDefinitionCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DefineAspectHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.DeleteAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect.UpdateAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.GetAspectDefinition
import io.github.kamiazya.scopes.scopemanagement.application.query.dto.ListAspectDefinitions
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.GetAspectDefinitionHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.handler.aspect.ListAspectDefinitionsHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.validation.AspectUsageValidationService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.ValidateAspectValueUseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.validation.AspectValueValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
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
            lateinit var transactionManager: NoOpTransactionManager
            lateinit var logger: Logger
            lateinit var applicationErrorMapper: ApplicationErrorMapper

            // Handlers
            lateinit var defineAspectHandler: DefineAspectHandler
            lateinit var getAspectDefinitionHandler: GetAspectDefinitionHandler
            lateinit var updateAspectDefinitionHandler: UpdateAspectDefinitionHandler
            lateinit var deleteAspectDefinitionHandler: DeleteAspectDefinitionHandler
            lateinit var listAspectDefinitionsHandler: ListAspectDefinitionsHandler
            lateinit var validateAspectValueUseCase: ValidateAspectValueUseCase
            lateinit var validationService: AspectValueValidationService

            beforeEach {
                // Initialize repositories
                aspectDefinitionRepository = InMemoryAspectDefinitionRepository()
                scopeRepository = InMemoryScopeRepository()
                transactionManager = NoOpTransactionManager()
                logger = object : Logger {
                    override fun debug(message: String, context: Map<String, Any>) {}
                    override fun info(message: String, context: Map<String, Any>) {}
                    override fun warn(message: String, context: Map<String, Any>) {}
                    override fun error(message: String, context: Map<String, Any>, throwable: Throwable?) {}
                    override fun isEnabledFor(level: LogLevel): Boolean = true
                    override fun withContext(context: Map<String, Any>): Logger = this
                    override fun withName(name: String): Logger = this
                }
                applicationErrorMapper = ApplicationErrorMapper(logger)

                // Initialize handlers
                defineAspectHandler = DefineAspectHandler(aspectDefinitionRepository, transactionManager, applicationErrorMapper)
                getAspectDefinitionHandler = GetAspectDefinitionHandler(aspectDefinitionRepository, transactionManager, logger)
                updateAspectDefinitionHandler = UpdateAspectDefinitionHandler(aspectDefinitionRepository, transactionManager, applicationErrorMapper)
                deleteAspectDefinitionHandler =
                    DeleteAspectDefinitionHandler(
                        aspectDefinitionRepository,
                        AspectUsageValidationService(scopeRepository),
                        transactionManager,
                        applicationErrorMapper,
                    )
                listAspectDefinitionsHandler = ListAspectDefinitionsHandler(aspectDefinitionRepository, transactionManager, logger)
                validationService = AspectValueValidationService()
                validateAspectValueUseCase = ValidateAspectValueUseCase(aspectDefinitionRepository, validationService)
            }

            describe("Aspect Definition CRUD Operations") {
                it("should define a text aspect") {
                    runTest {
                        // Act
                        val command = DefineAspectCommand(
                            key = "description",
                            description = "Task description",
                            type = AspectType.Text,
                        )
                        val result = defineAspectHandler(command)

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrElse { error("Expected success but got $it") }
                        definition.key.value shouldBe "description"
                        definition.type shouldBe AspectType.Text
                        definition.description shouldBe "Task description"
                    }
                }

                it("should define a numeric aspect") {
                    runTest {
                        // Act
                        val command = DefineAspectCommand(
                            key = "estimatedHours",
                            description = "Estimated hours",
                            type = AspectType.Numeric,
                        )
                        val result = defineAspectHandler(command)

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrElse { error("Expected success but got $it") }
                        definition.key.value shouldBe "estimatedHours"
                        definition.type shouldBe AspectType.Numeric
                    }
                }

                it("should define a boolean aspect") {
                    runTest {
                        // Act
                        val command = DefineAspectCommand(
                            key = "isCompleted",
                            description = "Completion status",
                            type = AspectType.BooleanType,
                        )
                        val result = defineAspectHandler(command)

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrElse { error("Expected success but got $it") }
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
                        val command = DefineAspectCommand(
                            key = "priority",
                            description = "Task priority",
                            type = AspectType.Ordered(values),
                        )
                        val result = defineAspectHandler(command)

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrElse { error("Expected success but got $it") }
                        definition.key.value shouldBe "priority"
                        val orderedType = definition.type as AspectType.Ordered
                        orderedType.allowedValues shouldContainExactlyInAnyOrder values
                    }
                }

                it("should define a duration aspect") {
                    runTest {
                        // Act
                        val command = DefineAspectCommand(
                            key = "timeSpent",
                            description = "Time spent on task",
                            type = AspectType.Duration,
                        )
                        val result = defineAspectHandler(command)

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrElse { error("Expected success but got $it") }
                        definition.key.value shouldBe "timeSpent"
                        definition.type shouldBe AspectType.Duration
                    }
                }

                it("should prevent duplicate aspect definitions") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("status", "Task status", AspectType.Text))

                        // Act
                        val result = defineAspectHandler(DefineAspectCommand("status", "Another status", AspectType.Numeric))

                        // Assert
                        result.shouldBeLeft()
                    }
                }

                it("should retrieve an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("category", "Task category", AspectType.Text))

                        // Act
                        val query = GetAspectDefinition("category")
                        val result = getAspectDefinitionHandler(query)

                        // Assert
                        result.shouldBeRight()
                        val definition = result.getOrElse { error("Expected success but got $it") }
                        definition shouldNotBe null
                        definition?.key shouldBe "category"
                    }
                }

                it("should update an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("label", "Task label", AspectType.Text))

                        // Act
                        val command = UpdateAspectDefinitionCommand(
                            key = "label",
                            description = "Updated task label",
                        )
                        val result = updateAspectDefinitionHandler(command)

                        // Assert
                        result.shouldBeRight()
                        val updated = result.getOrElse { error("Expected success but got $it") }
                        updated.description shouldBe "Updated task label"
                    }
                }

                it("should delete an aspect definition") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("temp", "Temporary aspect", AspectType.Text))

                        // Act
                        val command = DeleteAspectDefinitionCommand("temp")
                        val result = deleteAspectDefinitionHandler(command)

                        // Assert
                        result.shouldBeRight()

                        // Verify deletion
                        val getResult = getAspectDefinitionHandler(GetAspectDefinition("temp"))
                        getResult.shouldBeRight()
                        val definition = getResult.getOrElse { error("Expected success but got $it") }
                        definition shouldBe null
                    }
                }

                it("should list all aspect definitions") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("aspect1", "First aspect", AspectType.Text))
                        defineAspectHandler(DefineAspectCommand("aspect2", "Second aspect", AspectType.Numeric))
                        defineAspectHandler(DefineAspectCommand("aspect3", "Third aspect", AspectType.BooleanType))

                        // Act
                        val query = ListAspectDefinitions()
                        val result = listAspectDefinitionsHandler(query)

                        // Assert
                        result.shouldBeRight()
                        val definitions = result.getOrElse { error("Expected success but got $it") }
                        definitions.size shouldBe 3
                        definitions.map { it.key } shouldContainExactlyInAnyOrder listOf("aspect1", "aspect2", "aspect3")
                    }
                }
            }

            describe("Aspect Value Validation") {
                it("should validate text values") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("note", "Task note", AspectType.Text))

                        // Act
                        val query = ValidateAspectValueUseCase.Query("note", "This is a valid note")
                        val result = validateAspectValueUseCase(query)

                        // Assert
                        result.shouldBeRight()
                        val validatedValue = result.getOrElse { error("Expected success but got $it") }
                        validatedValue.value shouldBe "This is a valid note"
                    }
                }

                it("should validate numeric values") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("score", "Task score", AspectType.Numeric))

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
                        defineAspectHandler(DefineAspectCommand("active", "Is active", AspectType.BooleanType))

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
                        defineAspectHandler(DefineAspectCommand("size", "Task size", AspectType.Ordered(sizes)))

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
                        defineAspectHandler(DefineAspectCommand("duration", "Task duration", AspectType.Duration))

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
                        val validated = result.getOrElse { error("Expected success but got $it") }
                        val tagsKeyForAssertion = AspectKey.create("tags").getOrElse { error("Failed to create tags key") }
                        validated[tagsKeyForAssertion]?.size shouldBe 3
                    }
                }

                it("should reject multiple values when not allowed") {
                    runTest {
                        // Arrange
                        defineAspectHandler(DefineAspectCommand("status", "Task status", AspectType.Text))

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
