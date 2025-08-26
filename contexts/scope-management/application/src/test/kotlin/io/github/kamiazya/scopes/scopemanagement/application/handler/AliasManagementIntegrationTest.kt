package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.getOrElse
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAlias
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
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

            val addAliasHandler = AddAliasHandler(aliasService, aliasRepository, transactionManager, logger)
            val removeAliasHandler = RemoveAliasHandler(aliasService, transactionManager, logger)
            val renameAliasHandler = RenameAliasHandler(aliasRepository, transactionManager, logger)

            describe("complete alias lifecycle") {
                it("should add, rename, and remove aliases successfully") {
                    // Given - setup initial scope with canonical alias
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val initialAlias = "initial-alias"
                    val initialAliasNameVO = AliasName.create(initialAlias).getOrNull()!!

                    // Create initial canonical alias
                    val initialCanonicalAlias = ScopeAlias.createCanonical(scopeIdVO, initialAliasNameVO)
                    aliasRepository.save(initialCanonicalAlias)

                    // Add new alias using existing alias
                    val newAlias = "my-project"
                    val addCommand = AddAlias(initialAlias, newAlias)
                    val addResult = addAliasHandler(addCommand)

                    // Then - add should succeed
                    addResult.shouldBeRight()

                    // Verify alias was added
                    val aliases = aliasRepository.findByScopeId(scopeIdVO)
                        .getOrElse { emptyList() }
                    aliases.size shouldBe 2
                    aliases.any { it.aliasName.value == newAlias } shouldBe true

                    // Rename the new alias
                    val renamedAlias = "my-renamed-project"
                    val renameCommand = RenameAlias(newAlias, renamedAlias)
                    val renameResult = renameAliasHandler(renameCommand)

                    // Then - rename should succeed
                    renameResult.shouldBeRight()

                    // Verify alias was renamed
                    val aliasesAfterRename = aliasRepository.findByScopeId(scopeIdVO)
                        .getOrElse { emptyList() }
                    aliasesAfterRename.size shouldBe 2
                    aliasesAfterRename.any { it.aliasName.value == newAlias } shouldBe false
                    aliasesAfterRename.any { it.aliasName.value == renamedAlias } shouldBe true

                    // Remove alias
                    val removeCommand = RemoveAlias(renamedAlias)
                    val removeResult = removeAliasHandler(removeCommand)

                    // Then - remove should succeed
                    removeResult.shouldBeRight()

                    // Verify alias was removed
                    val remainingAliases = aliasRepository.findByScopeId(scopeIdVO)
                        .getOrElse { emptyList() }
                    remainingAliases.size shouldBe 1
                    remainingAliases[0].aliasName.value shouldBe initialAlias
                }
            }

            describe("duplicate alias handling") {
                it("should prevent adding duplicate aliases") {
                    // Given - setup two scopes with aliases
                    val scopeId1 = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeId2 = "01HZQB5QKM0WDG7ZBHSPKT3N2Z"
                    val scopeIdVO1 = ScopeId.create(scopeId1).getOrNull()!!
                    val scopeIdVO2 = ScopeId.create(scopeId2).getOrNull()!!

                    val alias1 = "scope1-alias"
                    val alias2 = "scope2-alias"
                    val sharedAlias = "shared-alias"

                    // Create initial aliases for both scopes
                    aliasRepository.save(ScopeAlias.createCanonical(scopeIdVO1, AliasName.create(alias1).getOrNull()!!))
                    aliasRepository.save(ScopeAlias.createCanonical(scopeIdVO2, AliasName.create(alias2).getOrNull()!!))

                    // Add shared alias to first scope
                    val addCommand1 = AddAlias(alias1, sharedAlias)
                    val result1 = addAliasHandler(addCommand1)
                    result1.shouldBeRight()

                    // Try to add same alias to second scope
                    val addCommand2 = AddAlias(alias2, sharedAlias)
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

                it("should preserve canonical status when renaming") {
                    // Given - create a canonical alias
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val canonicalAliasName = AliasName.create("old-canonical").getOrNull()!!
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, canonicalAliasName)

                    // Save canonical alias to repository
                    aliasRepository.save(canonicalAlias)

                    // When - rename canonical alias
                    val renameCommand = RenameAlias("old-canonical", "new-canonical")
                    val result = renameAliasHandler(renameCommand)

                    // Then - should succeed
                    result.shouldBeRight()

                    // Verify new alias is still canonical
                    val renamedAlias = aliasRepository.findByAliasName(AliasName.create("new-canonical").getOrNull()!!)
                        .getOrElse { null }
                    renamedAlias?.isCanonical() shouldBe true
                }
            }

            describe("non-existent alias handling") {
                it("should handle operations on non-existent aliases gracefully") {
                    // Remove non-existent alias
                    val removeCommand = RemoveAlias("non-existent-alias")
                    val removeResult = removeAliasHandler(removeCommand)

                    removeResult.shouldBeLeft()
                    removeResult.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "non-existent-alias"
                    }

                    // Rename non-existent alias
                    val renameCommand = RenameAlias("non-existent", "new-name")
                    val renameResult = renameAliasHandler(renameCommand)

                    renameResult.shouldBeLeft()
                    renameResult.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe "non-existent"
                    }

                    // Add alias using non-existent alias
                    val addCommand = AddAlias("non-existent", "new-alias")
                    val addResult = addAliasHandler(addCommand)

                    addResult.shouldBeLeft()
                    addResult.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe "non-existent"
                    }
                }
            }

            describe("rename conflicts") {
                it("should prevent renaming to an existing alias") {
                    // Given - create two aliases
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val alias1 = ScopeAlias.createCustom(scopeId, AliasName.create("alias1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("alias2").getOrNull()!!)

                    aliasRepository.save(alias1)
                    aliasRepository.save(alias2)

                    // When - try to rename alias1 to alias2
                    val renameCommand = RenameAlias("alias1", "alias2")
                    val result = renameAliasHandler(renameCommand)

                    // Then - should fail
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "alias2"
                    }
                }
            }
        }
    })
