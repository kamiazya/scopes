package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.application.port.TransactionContext
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.application.service.error.CrossAggregateValidationError
import io.github.kamiazya.scopes.application.test.MockLogger
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.domain.error.currentTimestamp
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.AliasType
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveMaxLength
import io.kotest.matchers.string.shouldHaveMinLength
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock

class CreateScopeHandlerPropertyTest : StringSpec({

    // Balanced iteration count for quality testing without memory issues
    val iterations = 100  // Enough to find issues, but not cause memory problems

    "valid scope creation requests should always succeed" {
        checkAll(iterations, validCreateScopeCommandArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks for successful creation
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns false.right()
            coEvery { scopeRepository.save(any()) } answers {
                firstArg<Scope>().right()
            }

            if (command.parentId != null) {
                coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns Unit.right()
                coEvery { hierarchyService.calculateHierarchyDepth(any(), any()) } returns 1.right()
                coEvery { hierarchyService.validateHierarchyDepth(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findByParentId(any()) } returns emptyList<Scope>().right()
                coEvery { hierarchyService.validateChildrenLimit(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findById(any()) } returns null.right()
            }

            if (command.customAlias != null) {
                val mockAlias = createMockScopeAlias(command.customAlias)
                coEvery { aliasService.assignCanonicalAlias(any(), any()) } returns mockAlias.right()
            } else if (command.generateAlias) {
                val mockAlias = createMockScopeAlias("generated-alias-${System.currentTimeMillis()}")
                coEvery { aliasService.generateCanonicalAlias(any()) } returns mockAlias.right()
            }

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeRight()
            result.getOrNull()?.let { createResult ->
                createResult.title shouldBe command.title.trim()
                if (command.description?.trim()?.isNotEmpty() == true) {
                    createResult.description shouldBe command.description.trim()
                }
                createResult.parentId shouldBe command.parentId
                createResult.id shouldNotBe null

                // Verify title constraints
                createResult.title.shouldHaveMinLength(1)
                createResult.title.shouldHaveMaxLength(200)
                createResult.title.shouldNotContain("\n")
                createResult.title.shouldNotContain("\r")
            }
        }
    }

    "scope creation should generate unique IDs" {
        checkAll(iterations, validCreateScopeCommandArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            val createdScopes = mutableListOf<Scope>()

            // Setup mocks
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns false.right()
            coEvery { scopeRepository.save(any()) } answers {
                val scope = firstArg<Scope>()
                createdScopes.add(scope)
                scope.right()
            }

            if (command.parentId != null) {
                coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns Unit.right()
                coEvery { hierarchyService.calculateHierarchyDepth(any(), any()) } returns 1.right()
                coEvery { hierarchyService.validateHierarchyDepth(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findByParentId(any()) } returns emptyList<Scope>().right()
                coEvery { hierarchyService.validateChildrenLimit(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findById(any()) } returns null.right()
            }

            if (command.customAlias != null) {
                val mockAlias = createMockScopeAlias(command.customAlias)
                coEvery { aliasService.assignCanonicalAlias(any(), any()) } returns mockAlias.right()
            } else if (command.generateAlias) {
                val mockAlias = createMockScopeAlias("generated-alias-${System.currentTimeMillis()}")
                coEvery { aliasService.generateCanonicalAlias(any()) } returns mockAlias.right()
            }

            // Act - Create two scopes
            val result1 = runBlocking { handler(command) }
            val result2 = runBlocking { handler(command) }

            // Assert
            if (result1.isRight() && result2.isRight()) {
                createdScopes.size shouldBe 2
                createdScopes[0].id shouldNotBe createdScopes[1].id
            }
        }
    }

    "duplicate titles at same level should fail" {
        checkAll(iterations, validCreateScopeCommandArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks for duplicate title
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns true.right()

            if (command.parentId != null) {
                coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns Unit.right()
                coEvery { hierarchyService.calculateHierarchyDepth(any(), any()) } returns 1.right()
                coEvery { hierarchyService.validateHierarchyDepth(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findByParentId(any()) } returns emptyList<Scope>().right()
                coEvery { hierarchyService.validateChildrenLimit(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findById(any()) } returns null.right()
            }

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeLeft()
            result.leftOrNull().shouldBeInstanceOf<ScopeUniquenessError.DuplicateTitle>()
        }
    }

    "invalid parent ID should fail" {
        checkAll(iterations,
            validTitleArb(),
            invalidScopeIdArb()
        ) { title, invalidParentId ->
            // Arrange
            val command = CreateScope(
                title = title,
                parentId = invalidParentId,
                metadata = emptyMap()
            )

            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeLeft()
            result.leftOrNull().shouldBeInstanceOf<ScopeHierarchyError.InvalidParentId>()
        }
    }

    "parent not found should fail" {
        checkAll(iterations, validCreateScopeCommandWithParentArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks for parent not found
            coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns
                CrossAggregateValidationError.CrossReferenceViolation(
                    sourceAggregate = "scope",
                    targetAggregate = command.parentId!!,
                    referenceType = "parentId",
                    violation = "Parent not found"
                ).left()

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeLeft()
            result.leftOrNull().shouldBeInstanceOf<ScopeHierarchyError.ParentNotFound>()
        }
    }

    "hierarchy depth validation should be enforced" {
        checkAll(iterations, validCreateScopeCommandWithParentArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks for hierarchy too deep
            coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns Unit.right()
            coEvery { hierarchyService.calculateHierarchyDepth(any(), any()) } returns 10.right()
            coEvery { hierarchyService.validateHierarchyDepth(any(), any()) } returns
                ScopeHierarchyError.MaxDepthExceeded(
                    currentTimestamp(),
                    ScopeId.create(command.parentId!!).getOrNull()!!,
                    10,
                    10
                ).left()

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeLeft()
            result.leftOrNull().shouldBeInstanceOf<ScopeHierarchyError.MaxDepthExceeded>()
        }
    }

    "children limit validation should be enforced" {
        checkAll(iterations, validCreateScopeCommandWithParentArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks for too many children
            coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns Unit.right()
            coEvery { hierarchyService.calculateHierarchyDepth(any(), any()) } returns 1.right()
            coEvery { hierarchyService.validateHierarchyDepth(any(), any()) } returns Unit.right()
            coEvery { scopeRepository.findByParentId(any()) } returns
                List(100) { mockk<Scope>() }.right()
            coEvery { hierarchyService.validateChildrenLimit(any(), any()) } returns
                ScopeHierarchyError.MaxChildrenExceeded(
                    currentTimestamp(),
                    ScopeId.create(command.parentId!!).getOrNull()!!,
                    100,
                    100
                ).left()

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeLeft()
            result.leftOrNull().shouldBeInstanceOf<ScopeHierarchyError.MaxChildrenExceeded>()
        }
    }

    "metadata should be converted to aspects correctly" {
        checkAll(iterations,
            validTitleArb(),
            validMetadataArb()
        ) { title, metadata ->
            // Arrange
            val command = CreateScope(
                title = title,
                metadata = metadata
            )

            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns false.right()

            val savedScopes = mutableListOf<Scope>()
            coEvery { scopeRepository.save(any()) } answers {
                val scope = firstArg<Scope>()
                savedScopes.add(scope)
                scope.right()
            }

            // Setup alias mocks if needed
            if (command.customAlias != null) {
                val mockAlias = createMockScopeAlias(command.customAlias)
                coEvery { aliasService.assignCanonicalAlias(any(), any()) } returns mockAlias.right()
            } else if (command.generateAlias) {
                val mockAlias = createMockScopeAlias("generated-alias-${System.currentTimeMillis()}")
                coEvery { aliasService.generateCanonicalAlias(any()) } returns mockAlias.right()
            }

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeRight()
            if (savedScopes.isNotEmpty()) {
                val savedScope = savedScopes.first()
                metadata.forEach { (key, value) ->
                    val aspectKey = AspectKey.create(key).getOrNull()
                    if (aspectKey != null) {
                        savedScope.hasAspect(aspectKey) shouldBe true
                        savedScope.getAspectValue(aspectKey)?.value shouldBe value
                    }
                }
            }
        }
    }

    "custom alias should be assigned when provided" {
        checkAll(iterations,
            validTitleArb(),
            validAliasNameArb()
        ) { title, customAlias ->
            // Arrange
            val command = CreateScope(
                title = title,
                customAlias = customAlias,
                generateAlias = false,
                metadata = emptyMap()
            )

            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns false.right()
            coEvery { scopeRepository.save(any()) } answers {
                firstArg<Scope>().right()
            }

            val mockAlias = createMockScopeAlias(customAlias)
            coEvery { aliasService.assignCanonicalAlias(any(), any()) } returns mockAlias.right()

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeRight()
            result.getOrNull()?.canonicalAlias shouldBe customAlias

            coVerify(exactly = 1) {
                aliasService.assignCanonicalAlias(any(), any())
            }
        }
    }

    "alias should be generated when generateAlias is true" {
        checkAll(iterations, validTitleArb()) { title ->
            // Arrange
            val command = CreateScope(
                title = title,
                generateAlias = true,
                customAlias = null,
                metadata = emptyMap()
            )

            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns false.right()
            coEvery { scopeRepository.save(any()) } answers {
                firstArg<Scope>().right()
            }

            val generatedAlias = "generated-alias-123"
            val mockAlias = createMockScopeAlias(generatedAlias)
            coEvery { aliasService.generateCanonicalAlias(any()) } returns mockAlias.right()

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeRight()
            result.getOrNull()?.canonicalAlias shouldBe generatedAlias

            coVerify(exactly = 1) {
                aliasService.generateCanonicalAlias(any())
            }
        }
    }

    "scope creation should set timestamps correctly" {
        checkAll(iterations, validCreateScopeCommandArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            val beforeCreation = Clock.System.now()

            // Setup mocks
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns false.right()

            val savedScopes = mutableListOf<Scope>()
            coEvery { scopeRepository.save(any()) } answers {
                val scope = firstArg<Scope>()
                savedScopes.add(scope)
                scope.right()
            }

            if (command.parentId != null) {
                coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns Unit.right()
                coEvery { hierarchyService.calculateHierarchyDepth(any(), any()) } returns 1.right()
                coEvery { hierarchyService.validateHierarchyDepth(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findByParentId(any()) } returns emptyList<Scope>().right()
                coEvery { hierarchyService.validateChildrenLimit(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findById(any()) } returns null.right()
            }

            // Setup alias mocks if needed
            if (command.customAlias != null) {
                val mockAlias = createMockScopeAlias(command.customAlias)
                coEvery { aliasService.assignCanonicalAlias(any(), any()) } returns mockAlias.right()
            } else if (command.generateAlias) {
                val mockAlias = createMockScopeAlias("generated-alias-${System.currentTimeMillis()}")
                coEvery { aliasService.generateCanonicalAlias(any()) } returns mockAlias.right()
            }

            // Act
            val result = runBlocking { handler(command) }
            val afterCreation = Clock.System.now()

            // Assert
            result.shouldBeRight()
            if (savedScopes.isNotEmpty()) {
                val savedScope = savedScopes.first()
                (savedScope.createdAt >= beforeCreation) shouldBe true
                (savedScope.createdAt <= afterCreation) shouldBe true
                savedScope.createdAt shouldBe savedScope.updatedAt
            }
        }
    }

    "repository errors should be propagated correctly" {
        checkAll(iterations, validCreateScopeCommandArb()) { command ->
            // Arrange
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = TestTransactionManager()
            val hierarchyService = mockk<ScopeHierarchyService>()
            val validationService = mockk<CrossAggregateValidationService>()
            val aliasService = mockk<ScopeAliasManagementService>()

            val handler = CreateScopeHandler(
                scopeRepository,
                transactionManager,
                hierarchyService,
                validationService,
                aliasService,
                MockLogger()
            )

            // Setup mocks for repository error
            coEvery { scopeRepository.existsByParentIdAndTitle(any(), any()) } returns false.right()
            coEvery { scopeRepository.save(any()) } returns
                PersistenceError.StorageUnavailable(
                    currentTimestamp(),
                    "save",
                    null
                ).left()

            if (command.parentId != null) {
                coEvery { validationService.validateHierarchyConsistency(any(), any()) } returns Unit.right()
                coEvery { hierarchyService.calculateHierarchyDepth(any(), any()) } returns 1.right()
                coEvery { hierarchyService.validateHierarchyDepth(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findByParentId(any()) } returns emptyList<Scope>().right()
                coEvery { hierarchyService.validateChildrenLimit(any(), any()) } returns Unit.right()
                coEvery { scopeRepository.findById(any()) } returns null.right()
            }

            // Act
            val result = runBlocking { handler(command) }

            // Assert
            result.shouldBeLeft()
            result.leftOrNull().shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
        }
    }
})

// Helper function to create a mock ScopeAlias with null-safe handling
private fun createMockScopeAlias(aliasName: String): ScopeAlias {
    // First validate the alias name
    val validatedAliasName = AliasName.create(aliasName).getOrElse {
        // Fallback to a valid default if the provided name is invalid
        AliasName.create("default-alias").getOrNull()!!
    }

    return mockk {
        every { this@mockk.aliasName } returns validatedAliasName
        every { scopeId } returns ScopeId.generate()
        every { aliasType } returns AliasType.CANONICAL
        every { createdAt } returns Clock.System.now()
        every { updatedAt } returns Clock.System.now()
    }
}

// Custom Arbitrary generators
private fun validTitleArb(): Arb<String> = Arb.string(1..100)
    .filter { title ->
        val trimmed = title.trim()
        trimmed.isNotEmpty() &&
        !trimmed.contains('\n') &&
        !trimmed.contains('\r') &&
        !trimmed.contains('\\') &&  // Exclude backslash to avoid issues
        trimmed.all { it.isLetterOrDigit() || it in " -_.,!?()[]{}@#$%&+=~" }  // Allow only safe characters
    }

private fun validDescriptionArb(): Arb<String?> = Arb.choice(
    Arb.constant(null),
    Arb.string(0..100).map { it.take(500) }  // More efficient, no filter
)

private fun validScopeIdArb(): Arb<String> = Arb.constant("").map {
    ScopeId.generate().value.toString()
}

private fun invalidScopeIdArb(): Arb<String> = Arb.choice(
    Arb.of("", " ", "invalid-id", "123", "not-a-ulid"),
    Arb.string(1..10).filter { !it.matches(Regex("[0-9A-HJKMNP-TV-Z]{26}")) }
)

private fun validAliasNameArb(): Arb<String> = Arb.string(2..50)
    .map { raw ->
        // Generate a valid alias name that follows all domain rules
        val chars = raw.lowercase()
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' }
            .ifEmpty { "alias" }

        // Ensure it starts with alphanumeric
        val validStart = if (chars.isNotEmpty() && chars.first().isLetterOrDigit()) {
            chars
        } else {
            "a$chars"
        }

        // Ensure it ends with alphanumeric
        val validEnd = if (validStart.isNotEmpty() && validStart.last().isLetterOrDigit()) {
            validStart
        } else {
            "${validStart}1"
        }

        // Remove consecutive special characters and ensure length constraints
        validEnd
            .replace(Regex("[-_]{2,}"), "-")
            .take(64)
            .let { if (it.length < 2) "a1" else it }
    }

private fun validAspectKeyArb(): Arb<String> = Arb.stringPattern("[a-z][a-z0-9_]{0,49}")

private fun validAspectValueArb(): Arb<String> = Arb.string(1..50)
    .map { it.trim().ifEmpty { "value" } }  // Always return a non-empty value

private fun validMetadataArb(): Arb<Map<String, String>> = Arb.map(
    keyArb = validAspectKeyArb(),
    valueArb = validAspectValueArb(),
    minSize = 0,
    maxSize = 2  // Reduced from 5 to avoid memory issues
)

private fun validCreateScopeCommandArb(): Arb<CreateScope> = Arb.bind(
    validTitleArb(),
    validDescriptionArb(),
    Arb.choice(
        Arb.constant(null),
        validScopeIdArb()
    ),
    validMetadataArb(),
    Arb.boolean(),
    Arb.choice(
        Arb.constant(null),
        validAliasNameArb()
    )
) { title, description, parentId, metadata, generateAlias, customAlias ->
    CreateScope(
        title = title,
        description = description,
        parentId = parentId,
        metadata = metadata,
        generateAlias = generateAlias && customAlias == null,
        customAlias = customAlias
    )
}

private fun validCreateScopeCommandWithParentArb(): Arb<CreateScope> = Arb.bind(
    validTitleArb(),
    validDescriptionArb(),
    validScopeIdArb(),
    validMetadataArb(),
    Arb.boolean(),
    Arb.choice(
        Arb.constant(null),
        validAliasNameArb()
    )
) { title, description, parentId, metadata, generateAlias, customAlias ->
    CreateScope(
        title = title,
        description = description,
        parentId = parentId,
        metadata = metadata,
        generateAlias = generateAlias && customAlias == null,
        customAlias = customAlias
    )
}

// Test implementation of TransactionManager
private class TestTransactionManager : TransactionManager {
    override suspend fun <E, T> inTransaction(
        block: suspend TransactionContext.() -> Either<E, T>
    ): Either<E, T> = TestTransactionContext().block()

    override suspend fun <E, T> inReadOnlyTransaction(
        block: suspend TransactionContext.() -> Either<E, T>
    ): Either<E, T> = TestTransactionContext().block()
}

private class TestTransactionContext : TransactionContext {
    private var markedForRollback = false

    override fun markForRollback() {
        markedForRollback = true
    }

    override fun isMarkedForRollback(): Boolean = markedForRollback

    override fun getTransactionId(): String = "test-transaction-${System.currentTimeMillis()}"
}

