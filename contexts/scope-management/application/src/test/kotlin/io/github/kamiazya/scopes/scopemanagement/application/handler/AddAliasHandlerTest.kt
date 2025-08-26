package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
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
import kotlinx.datetime.Clock

class AddAliasHandlerTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("AddAliasHandler") {
            val scopeAliasService = mockk<ScopeAliasManagementService>()
            val aliasRepository = mockk<ScopeAliasRepository>()
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            // Default transaction behavior - just execute the block
            coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                val block = firstArg<suspend () -> Either<Any, Any>>()
                block()
            }

            val handler = AddAliasHandler(scopeAliasService, aliasRepository, transactionManager, logger)

            describe("when adding a valid alias") {
                it("should successfully add the alias") {
                    // Given
                    val existingAlias = "project-main"
                    val newAlias = "project-v2"
                    val command = AddAlias(existingAlias, newAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val existingAliasNameVO = AliasName.create(existingAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAlias).getOrNull()!!

                    val existingAliasEntity = ScopeAlias.createCanonical(scopeIdVO, existingAliasNameVO)
                    val newAliasEntity = ScopeAlias.createCustom(scopeIdVO, newAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(existingAliasNameVO) } returns existingAliasEntity.right()
                    coEvery { scopeAliasService.assignCustomAlias(scopeIdVO, newAliasNameVO) } returns newAliasEntity.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 1) { scopeAliasService.assignCustomAlias(scopeIdVO, newAliasNameVO) }
                }
            }

            describe("when existing alias is invalid") {
                it("should return InvalidAlias error for invalid format") {
                    // Given
                    val command = AddAlias("invalid alias!", "new-alias")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                    coVerify(exactly = 0) { aliasRepository.findByAliasName(any()) }
                }

                it("should return AliasNotFound error when existing alias doesn't exist") {
                    // Given
                    val command = AddAlias("non-existent", "new-alias")
                    val existingAliasNameVO = AliasName.create("non-existent").getOrNull()!!

                    coEvery { aliasRepository.findByAliasName(existingAliasNameVO) } returns null.right()

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
                    val existingAlias = "project-main"
                    val command = AddAlias(existingAlias, "invalid alias!")

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val existingAliasNameVO = AliasName.create(existingAlias).getOrNull()!!
                    val existingAliasEntity = ScopeAlias.createCanonical(scopeIdVO, existingAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(existingAliasNameVO) } returns existingAliasEntity.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                }
            }

            describe("when service returns DuplicateAlias error") {
                it("should return InvalidAlias error") {
                    // Given
                    val existingAlias = "project-main"
                    val newAlias = "duplicate-alias"
                    val command = AddAlias(existingAlias, newAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val existingAliasNameVO = AliasName.create(existingAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAlias).getOrNull()!!

                    val existingAliasEntity = ScopeAlias.createCanonical(scopeIdVO, existingAliasNameVO)
                    val error = ScopeAliasError.DuplicateAlias(
                        Clock.System.now(),
                        newAlias,
                        scopeIdVO,
                        scopeIdVO,
                    )

                    coEvery { aliasRepository.findByAliasName(existingAliasNameVO) } returns existingAliasEntity.right()
                    coEvery { scopeAliasService.assignCustomAlias(scopeIdVO, newAliasNameVO) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>()
                }
            }

            describe("when repository returns an error") {
                it("should map repository error to application error") {
                    // Given
                    val command = AddAlias("project-main", "new-alias")
                    val existingAliasNameVO = AliasName.create("project-main").getOrNull()!!
                    val error = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.NotFound(
                        Clock.System.now(),
                        "ScopeAlias",
                        "project-main",
                    )

                    coEvery { aliasRepository.findByAliasName(existingAliasNameVO) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.NotFound>().apply {
                        entityType shouldBe "ScopeAlias"
                        entityId shouldBe "project-main"
                    }
                }
            }

            describe("when adding alias to scope with multiple existing aliases") {
                it("should successfully add new alias") {
                    // Given
                    val existingAlias = "project-secondary"
                    val newAlias = "project-v3"
                    val command = AddAlias(existingAlias, newAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val existingAliasNameVO = AliasName.create(existingAlias).getOrNull()!!
                    val newAliasNameVO = AliasName.create(newAlias).getOrNull()!!

                    // Existing alias is a custom alias (not canonical)
                    val existingAliasEntity = ScopeAlias.createCustom(scopeIdVO, existingAliasNameVO)
                    val newAliasEntity = ScopeAlias.createCustom(scopeIdVO, newAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(existingAliasNameVO) } returns existingAliasEntity.right()
                    coEvery { scopeAliasService.assignCustomAlias(scopeIdVO, newAliasNameVO) } returns newAliasEntity.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 1) { scopeAliasService.assignCustomAlias(scopeIdVO, newAliasNameVO) }
                }
            }
        }
    })
