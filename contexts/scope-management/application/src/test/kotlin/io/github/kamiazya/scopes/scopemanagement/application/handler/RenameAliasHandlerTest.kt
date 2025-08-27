package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RenameAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError as AppScopeAliasError

class RenameAliasHandlerTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("RenameAliasHandler") {
            val aliasRepository = mockk<ScopeAliasRepository>()
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            beforeEach {
                // Configure transaction manager to execute the block directly
                coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                    val block = firstArg<suspend () -> Either<Any, Any>>()
                    block.invoke()
                }
            }

            val handler = RenameAliasHandler(aliasRepository, transactionManager, logger)

            describe("when renaming a valid alias") {
                it("should successfully rename the alias preserving type and creation time") {
                    // Given
                    val currentAlias = "old-name"
                    val newAliasName = "new-name"
                    val command = RenameAlias(currentAlias, newAliasName)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAliasName).getOrNull()!!

                    val originalCreatedAt = Clock.System.now() - 7.days
                    val currentAliasEntity = ScopeAlias.createCanonical(scopeIdVO, currentAliasNameVO)
                        .copy(createdAt = originalCreatedAt)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newAliasNameVO) } returns null.right()
                    coEvery { aliasRepository.removeByAliasName(currentAliasNameVO) } returns true.right()

                    val savedAliasSlot = slot<ScopeAlias>()
                    coEvery { aliasRepository.save(capture(savedAliasSlot)) } answers {
                        Unit.right()
                    }

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    val savedAlias = savedAliasSlot.captured
                    savedAlias.aliasName shouldBe newAliasNameVO
                    savedAlias.aliasType shouldBe AliasType.CANONICAL
                    savedAlias.scopeId shouldBe scopeIdVO
                    savedAlias.createdAt shouldBe originalCreatedAt // Creation time preserved

                    coVerify { aliasRepository.removeByAliasName(currentAliasNameVO) }
                    coVerify { aliasRepository.save(any()) }
                }

                it("should preserve custom alias type when renaming") {
                    // Given
                    val currentAlias = "old-custom"
                    val newAliasName = "new-custom"
                    val command = RenameAlias(currentAlias, newAliasName)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAliasName).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newAliasNameVO) } returns null.right()
                    coEvery { aliasRepository.removeByAliasName(currentAliasNameVO) } returns true.right()

                    val savedAliasSlot = slot<ScopeAlias>()
                    coEvery { aliasRepository.save(capture(savedAliasSlot)) } answers {
                        Unit.right()
                    }

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    val savedAlias = savedAliasSlot.captured
                    savedAlias.aliasType shouldBe AliasType.CUSTOM
                }
            }

            describe("when current alias is invalid") {
                it("should return InvalidAlias error for invalid format") {
                    // Given
                    val command = RenameAlias("invalid alias!", "new-name")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasInvalidFormat>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                }

                it("should return AliasNotFound error when alias doesn't exist") {
                    // Given
                    val command = RenameAlias("non-existent", "new-name")
                    val currentAliasNameVO = AliasName.create("non-existent").getOrNull()!!

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns null.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe "non-existent"
                    }
                }
            }

            describe("when new alias name is invalid") {
                it("should return InvalidAlias error for invalid format") {
                    // Given
                    val currentAlias = "existing-alias"
                    val command = RenameAlias(currentAlias, "invalid alias!")

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasInvalidFormat>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                }
            }

            describe("when new alias name already exists") {
                it("should return AliasDuplicate error") {
                    // Given
                    val currentAlias = "existing-alias"
                    val newAliasName = "already-taken"
                    val command = RenameAlias(currentAlias, newAliasName)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val differentScopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Z"
                    val differentScopeIdVO = ScopeId.create(differentScopeId).getOrNull()!!

                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAliasName).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)
                    val existingAlias = ScopeAlias.createCustom(differentScopeIdVO, newAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newAliasNameVO) } returns existingAlias.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<AppScopeAliasError.AliasDuplicate>().apply {
                        aliasName shouldBe newAliasName
                    }
                    coVerify(exactly = 0) { aliasRepository.removeByAliasName(any()) }
                    coVerify(exactly = 0) { aliasRepository.save(any()) }
                }
            }

            describe("when repository operations fail") {
                it("should handle delete failure") {
                    // Given
                    val currentAlias = "existing-alias"
                    val newAliasName = "new-name"
                    val command = RenameAlias(currentAlias, newAliasName)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAliasName).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)
                    val error = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.StorageUnavailable(
                        Clock.System.now(),
                        "delete",
                        null,
                    )

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newAliasNameVO) } returns null.right()
                    coEvery { aliasRepository.removeByAliasName(currentAliasNameVO) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.StorageUnavailable>()
                    coVerify(exactly = 0) { aliasRepository.save(any()) }
                }

                it("should handle save failure") {
                    // Given
                    val currentAlias = "existing-alias"
                    val newAliasName = "new-name"
                    val command = RenameAlias(currentAlias, newAliasName)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAliasName).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)
                    val error = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.StorageUnavailable(
                        Clock.System.now(),
                        "save",
                        null,
                    )

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newAliasNameVO) } returns null.right()
                    coEvery { aliasRepository.removeByAliasName(currentAliasNameVO) } returns true.right()
                    coEvery { aliasRepository.save(any()) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.StorageUnavailable>()
                }
            }
        }
    })
