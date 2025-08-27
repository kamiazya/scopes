package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.getOrElse
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.SetCanonicalAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAliases
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeAliasRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk

class CanonicalAliasIntegrationTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("Canonical Alias Transitions") {
            lateinit var aliasRepository: ScopeAliasRepository
            lateinit var aliasService: ScopeAliasManagementService
            val aliasGenerationService = mockk<AliasGenerationService>()
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            lateinit var addAliasHandler: AddAliasHandler
            lateinit var setCanonicalHandler: SetCanonicalAliasHandler
            lateinit var listAliasesHandler: ListAliasesHandler

            beforeEach {
                // Create fresh repository for each test
                aliasRepository = InMemoryScopeAliasRepository()
                aliasService = ScopeAliasManagementService(aliasRepository, aliasGenerationService)

                // Configure transaction manager to execute the block directly
                coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                    val block = firstArg<suspend () -> Either<Any, Any>>()
                    block.invoke()
                }

                // Initialize handlers with fresh repository
                addAliasHandler = AddAliasHandler(aliasService, aliasRepository, transactionManager, logger)
                setCanonicalHandler = SetCanonicalAliasHandler(
                    aliasService,
                    aliasRepository,
                    transactionManager,
                    logger,
                )
                listAliasesHandler = ListAliasesHandler(aliasRepository, transactionManager, logger)
            }

            describe("complete canonical alias lifecycle") {
                it("should transition canonical status between aliases") {
                    // Given - create a scope with generated canonical alias
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val generatedAliasName = AliasName.create("generated-alias").getOrNull()!!

                    // Simulate the scope already having a generated canonical alias
                    val generatedAlias = ScopeAlias.createCanonical(scopeIdVO, generatedAliasName)
                    aliasRepository.save(generatedAlias)

                    // Add first custom alias using generated alias
                    val alias1 = "project-v1"
                    val addCommand1 = AddAlias("generated-alias", alias1)
                    val addResult1 = addAliasHandler(addCommand1)
                    addResult1.shouldBeRight()

                    // Add second custom alias using first alias
                    val alias2 = "project-v2"
                    val addCommand2 = AddAlias(alias1, alias2)
                    val addResult2 = addAliasHandler(addCommand2)
                    addResult2.shouldBeRight()

                    // Verify initial state - generated alias is canonical
                    val initialList = listAliasesHandler(ListAliases(scopeId))
                    initialList.shouldBeRight()
                    val initialAliases = initialList.getOrElse { throw AssertionError("Expected Right but got Left") }.aliases
                    initialAliases.size shouldBe 3
                    initialAliases.find { it.aliasName == "generated-alias" }?.isCanonical shouldBe true
                    initialAliases.find { it.aliasName == alias1 }?.isCanonical shouldBe false
                    initialAliases.find { it.aliasName == alias2 }?.isCanonical shouldBe false

                    // Set alias1 as canonical using generated-alias as current
                    val setCommand1 = SetCanonicalAlias("generated-alias", alias1)
                    val setResult1 = setCanonicalHandler(setCommand1)
                    setResult1.shouldBeRight()

                    // Verify state after first transition
                    val afterFirst = listAliasesHandler(ListAliases(scopeId))
                    afterFirst.shouldBeRight()
                    val aliasesAfterFirst = afterFirst.getOrElse { throw AssertionError("Expected Right but got Left") }.aliases
                    aliasesAfterFirst.find { it.aliasName == "generated-alias" }?.isCanonical shouldBe false
                    aliasesAfterFirst.find { it.aliasName == alias1 }?.isCanonical shouldBe true
                    aliasesAfterFirst.find { it.aliasName == alias2 }?.isCanonical shouldBe false

                    // Set alias2 as canonical using alias1 as current
                    val setCommand2 = SetCanonicalAlias(alias1, alias2)
                    val setResult2 = setCanonicalHandler(setCommand2)
                    setResult2.shouldBeRight()

                    // Verify final state
                    val finalList = listAliasesHandler(ListAliases(scopeId))
                    finalList.shouldBeRight()
                    val finalAliases = finalList.getOrElse { throw AssertionError("Expected Right but got Left") }.aliases
                    finalAliases.find { it.aliasName == "generated-alias" }?.isCanonical shouldBe false
                    finalAliases.find { it.aliasName == alias1 }?.isCanonical shouldBe false
                    finalAliases.find { it.aliasName == alias2 }?.isCanonical shouldBe true

                    // Verify all aliases still exist but only one is canonical
                    finalAliases.size shouldBe 3
                    finalAliases.count { it.isCanonical } shouldBe 1
                }
            }

            describe("idempotent canonical operations") {
                it("should handle setting same alias as canonical multiple times") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val aliasName = "my-project"

                    // Create initial canonical alias for the scope
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val initialCanonical = ScopeAlias.createCanonical(scopeIdVO, AliasName.create("initial-canonical").getOrNull()!!)
                    aliasRepository.save(initialCanonical)

                    // Add alias using the initial canonical
                    val addCommand = AddAlias("initial-canonical", aliasName)
                    addAliasHandler(addCommand).shouldBeRight()

                    // Set as canonical first time using the initial canonical
                    val setCommand = SetCanonicalAlias("initial-canonical", aliasName)
                    setCanonicalHandler(setCommand).shouldBeRight()

                    // Set as canonical second time (idempotent) - now aliasName is canonical
                    val setCommand2 = SetCanonicalAlias(aliasName, aliasName)
                    val result = setCanonicalHandler(setCommand2)
                    result.shouldBeRight()

                    // Verify state unchanged
                    val listResult = listAliasesHandler(ListAliases(scopeId))
                    listResult.shouldBeRight()
                    val aliases = listResult.getOrElse { throw AssertionError("Expected Right but got Left") }.aliases
                    aliases.size shouldBe 2 // initial-canonical and my-project
                    aliases.find { it.aliasName == aliasName }?.isCanonical shouldBe true
                    aliases.find { it.aliasName == "initial-canonical" }?.isCanonical shouldBe false
                }
            }

            describe("error scenarios") {
                it("should fail when current alias doesn't exist") {
                    // Given
                    val nonExistentAlias = "non-existent"
                    val targetAlias = "target"

                    // When
                    val command = SetCanonicalAlias(nonExistentAlias, targetAlias)
                    val result = setCanonicalHandler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe nonExistentAlias
                    }
                }

                it("should fail when new canonical alias doesn't exist") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val existingAlias = "existing"
                    val nonExistentAlias = "non-existent"

                    // Create initial canonical alias for the scope
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val initialCanonical = ScopeAlias.createCanonical(scopeIdVO, AliasName.create("initial-canonical").getOrNull()!!)
                    aliasRepository.save(initialCanonical)

                    // Add existing alias using initial canonical
                    addAliasHandler(AddAlias("initial-canonical", existingAlias)).shouldBeRight()

                    // When
                    val command = SetCanonicalAlias(existingAlias, nonExistentAlias)
                    val result = setCanonicalHandler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe nonExistentAlias
                    }
                }

                it("should fail when aliases belong to different scopes") {
                    // Given
                    val scopeId1 = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeId2 = "01HZQB5QKM0WDG7ZBHSPKT3N2Z"
                    val alias1 = "project-1"
                    val alias2 = "project-2"

                    // Create canonical aliases for both scopes
                    val scopeIdVO1 = ScopeId.create(scopeId1).getOrNull()!!
                    val scopeIdVO2 = ScopeId.create(scopeId2).getOrNull()!!
                    val canonical1 = ScopeAlias.createCanonical(scopeIdVO1, AliasName.create("canonical-1").getOrNull()!!)
                    val canonical2 = ScopeAlias.createCanonical(scopeIdVO2, AliasName.create("canonical-2").getOrNull()!!)
                    aliasRepository.save(canonical1)
                    aliasRepository.save(canonical2)

                    // Add alias to scope1
                    addAliasHandler(AddAlias("canonical-1", alias1)).shouldBeRight()
                    // Add alias to scope2
                    addAliasHandler(AddAlias("canonical-2", alias2)).shouldBeRight()

                    // Try to promote alias2 using alias1 (different scopes)
                    val setCommand = SetCanonicalAlias(alias1, alias2)
                    val result = setCanonicalHandler(setCommand)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe alias2
                    }
                }
            }

            describe("canonical alias sorting") {
                it("should always show canonical alias first in listings") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"

                    // Create initial canonical alias
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val initialAlias = ScopeAlias.createCanonical(scopeIdVO, AliasName.create("initial-alias").getOrNull()!!)
                    aliasRepository.save(initialAlias)

                    // Add multiple aliases
                    val aliases = listOf("zebra", "alpha", "beta", "gamma")
                    var previousAlias = "initial-alias"
                    aliases.forEach { alias ->
                        addAliasHandler(AddAlias(previousAlias, alias)).shouldBeRight()
                        previousAlias = alias
                    }

                    // Set "zebra" (last alphabetically) as canonical using "alpha" as current
                    setCanonicalHandler(SetCanonicalAlias("alpha", "zebra")).shouldBeRight()

                    // When listing
                    val result = listAliasesHandler(ListAliases(scopeId))

                    // Then - canonical should be first despite being last alphabetically
                    result.shouldBeRight()
                    val aliasList = result.getOrElse { throw AssertionError("Expected Right but got Left") }.aliases
                    aliasList[0].aliasName shouldBe "zebra"
                    aliasList[0].isCanonical shouldBe true
                }
            }
        }
    })
