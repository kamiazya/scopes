package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.getOrElse
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
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

class AliasManagementIntegrationTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("Alias Management Integration") {
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
            val removeAliasHandler = RemoveAliasHandler(aliasService, transactionManager, logger)

            describe("complete alias lifecycle") {
                it("should add and remove aliases successfully") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val aliasName = "my-project"

                    // Add alias
                    val addCommand = AddAlias(scopeId, aliasName)
                    val addResult = addAliasHandler(addCommand)

                    // Then - add should succeed
                    addResult.shouldBeRight()

                    // Verify alias was added
                    val aliases = aliasRepository.findByScopeId(ScopeId.create(scopeId).getOrNull()!!)
                        .getOrElse { emptyList() }
                    aliases.size shouldBe 1
                    aliases[0].aliasName.value shouldBe aliasName

                    // Remove alias
                    val removeCommand = RemoveAlias(aliasName)
                    val removeResult = removeAliasHandler(removeCommand)

                    // Then - remove should succeed
                    removeResult.shouldBeRight()

                    // Verify alias was removed
                    val remainingAliases = aliasRepository.findByScopeId(ScopeId.create(scopeId).getOrNull()!!)
                        .getOrElse { emptyList() }
                    remainingAliases.size shouldBe 0
                }
            }

            describe("duplicate alias handling") {
                it("should prevent adding duplicate aliases") {
                    // Given
                    val scopeId1 = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeId2 = "01HZQB5QKM0WDG7ZBHSPKT3N2Z"
                    val sharedAlias = "shared-alias"

                    // Add alias to first scope
                    val addCommand1 = AddAlias(scopeId1, sharedAlias)
                    val result1 = addAliasHandler(addCommand1)
                    result1.shouldBeRight()

                    // Try to add same alias to second scope
                    val addCommand2 = AddAlias(scopeId2, sharedAlias)
                    val result2 = addAliasHandler(addCommand2)

                    // Then - should fail with duplicate alias error
                    result2.shouldBeLeft()
                    result2.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe sharedAlias
                    }
                }
            }

            describe("canonical alias protection") {
                it("should prevent removing canonical aliases") {
                    // Given - create a canonical alias
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val canonicalAliasName = AliasName.create("canonical-alias").getOrNull()!!
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, canonicalAliasName)

                    // Save canonical alias to repository
                    aliasRepository.save(canonicalAlias)

                    // When - try to remove canonical alias
                    val removeCommand = RemoveAlias("canonical-alias")
                    val result = removeAliasHandler(removeCommand)

                    // Then - should fail
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>()
                }
            }

            describe("non-existent alias removal") {
                it("should handle removing non-existent aliases gracefully") {
                    // Given
                    val removeCommand = RemoveAlias("non-existent-alias")

                    // When
                    val result = removeAliasHandler(removeCommand)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "non-existent-alias"
                    }
                }
            }
        }
    })
