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
            val aliasRepository: ScopeAliasRepository = InMemoryScopeAliasRepository()
            val aliasGenerationService = mockk<AliasGenerationService>()
            val aliasService = ScopeAliasManagementService(aliasRepository, aliasGenerationService)
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            // Default transaction behavior - just execute the block
            coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                val block = firstArg<suspend () -> Either<Any, Any>>()
                block()
            }

            val addAliasHandler = AddAliasHandler(aliasService, transactionManager, logger)
            val setCanonicalHandler = SetCanonicalAliasHandler(
                aliasService,
                aliasRepository,
                transactionManager,
                logger,
            )
            val listAliasesHandler = ListAliasesHandler(aliasRepository, transactionManager, logger)

            describe("complete canonical alias lifecycle") {
                it("should transition canonical status between aliases") {
                    // Given - create a scope with generated canonical alias
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val generatedAliasName = AliasName.create("generated-alias").getOrNull()!!

                    // Simulate the scope already having a generated canonical alias
                    val generatedAlias = ScopeAlias.createCanonical(scopeIdVO, generatedAliasName)
                    aliasRepository.save(generatedAlias)

                    // Add first custom alias
                    val alias1 = "project-v1"
                    val addCommand1 = AddAlias(scopeId, alias1)
                    val addResult1 = addAliasHandler(addCommand1)
                    addResult1.shouldBeRight()

                    // Add second custom alias
                    val alias2 = "project-v2"
                    val addCommand2 = AddAlias(scopeId, alias2)
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

                    // Set alias1 as canonical
                    val setCommand1 = SetCanonicalAlias(scopeId, alias1)
                    val setResult1 = setCanonicalHandler(setCommand1)
                    setResult1.shouldBeRight()

                    // Verify state after first transition
                    val afterFirst = listAliasesHandler(ListAliases(scopeId))
                    afterFirst.shouldBeRight()
                    val aliasesAfterFirst = afterFirst.getOrElse { throw AssertionError("Expected Right but got Left") }.aliases
                    aliasesAfterFirst.find { it.aliasName == "generated-alias" }?.isCanonical shouldBe false
                    aliasesAfterFirst.find { it.aliasName == alias1 }?.isCanonical shouldBe true
                    aliasesAfterFirst.find { it.aliasName == alias2 }?.isCanonical shouldBe false

                    // Set alias2 as canonical
                    val setCommand2 = SetCanonicalAlias(scopeId, alias2)
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

                    // Add alias
                    val addCommand = AddAlias(scopeId, aliasName)
                    addAliasHandler(addCommand).shouldBeRight()

                    // Set as canonical first time
                    val setCommand = SetCanonicalAlias(scopeId, aliasName)
                    setCanonicalHandler(setCommand).shouldBeRight()

                    // Set as canonical second time (idempotent)
                    val result = setCanonicalHandler(setCommand)
                    result.shouldBeRight()

                    // Verify state unchanged
                    val listResult = listAliasesHandler(ListAliases(scopeId))
                    listResult.shouldBeRight()
                    val aliases = listResult.getOrElse { throw AssertionError("Expected Right but got Left") }.aliases
                    aliases.size shouldBe 1
                    aliases[0].isCanonical shouldBe true
                }
            }

            describe("error scenarios") {
                it("should fail to set non-existent alias as canonical") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val nonExistentAlias = "non-existent"

                    // When
                    val command = SetCanonicalAlias(scopeId, nonExistentAlias)
                    val result = setCanonicalHandler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe nonExistentAlias
                    }
                }

                it("should fail to set alias from different scope as canonical") {
                    // Given
                    val scopeId1 = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeId2 = "01HZQB5QKM0WDG7ZBHSPKT3N2Z"
                    val aliasName = "my-project"

                    // Add alias to scope1
                    val addCommand = AddAlias(scopeId1, aliasName)
                    addAliasHandler(addCommand).shouldBeRight()

                    // Try to set it as canonical for scope2
                    val setCommand = SetCanonicalAlias(scopeId2, aliasName)
                    val result = setCanonicalHandler(setCommand)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe aliasName
                    }
                }
            }

            describe("canonical alias sorting") {
                it("should always show canonical alias first in listings") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"

                    // Add multiple aliases
                    val aliases = listOf("zebra", "alpha", "beta", "gamma")
                    aliases.forEach { alias ->
                        addAliasHandler(AddAlias(scopeId, alias)).shouldBeRight()
                    }

                    // Set "zebra" (last alphabetically) as canonical
                    setCanonicalHandler(SetCanonicalAlias(scopeId, "zebra")).shouldBeRight()

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
