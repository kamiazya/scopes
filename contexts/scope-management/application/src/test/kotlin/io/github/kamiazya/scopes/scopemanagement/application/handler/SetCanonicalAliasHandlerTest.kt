package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.SetCanonicalAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
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
import kotlinx.datetime.Clock

class SetCanonicalAliasHandlerTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("SetCanonicalAliasHandler") {
            val scopeAliasService = mockk<ScopeAliasManagementService>()
            val aliasRepository = mockk<ScopeAliasRepository>()
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            // Default transaction behavior - just execute the block
            coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                val block = firstArg<suspend () -> Either<Any, Any>>()
                block()
            }

            val handler = SetCanonicalAliasHandler(
                scopeAliasService,
                aliasRepository,
                transactionManager,
                logger,
            )

            describe("when setting a valid alias as canonical") {
                it("should successfully set the alias as canonical") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val aliasName = "my-project"
                    val command = SetCanonicalAlias(scopeId, aliasName)

                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val existingAlias = ScopeAlias.createCustom(scopeIdVO, aliasNameVO)

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns existingAlias.right()
                    coEvery {
                        scopeAliasService.assignCanonicalAlias(
                            scopeIdVO,
                            aliasNameVO,
                        )
                    } returns existingAlias.copy(aliasType = AliasType.CANONICAL).right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 1) {
                        scopeAliasService.assignCanonicalAlias(
                            scopeIdVO,
                            aliasNameVO,
                        )
                    }
                }
            }

            describe("when alias is already canonical") {
                it("should return success without calling service (idempotent)") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val aliasName = "my-project"
                    val command = SetCanonicalAlias(scopeId, aliasName)

                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val canonicalAlias = ScopeAlias.createCanonical(scopeIdVO, aliasNameVO)

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns canonicalAlias.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 0) {
                        scopeAliasService.assignCanonicalAlias(any(), any())
                    }
                }
            }

            describe("when scopeId is invalid") {
                it("should return IdInvalidFormat error for invalid format") {
                    // Given
                    val command = SetCanonicalAlias("invalid-id", "my-project")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.IdInvalidFormat>().apply {
                        attemptedValue shouldBe "invalid-id"
                        expectedFormat shouldBe "ULID"
                    }
                    coVerify(exactly = 0) { aliasRepository.findByAliasName(any()) }
                }

                it("should return IdBlank error for blank id") {
                    // Given
                    val command = SetCanonicalAlias("", "my-project")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.IdBlank>().apply {
                        attemptedValue shouldBe ""
                    }
                    coVerify(exactly = 0) { aliasRepository.findByAliasName(any()) }
                }
            }

            describe("when alias name is invalid") {
                it("should return InvalidAlias error for invalid format") {
                    // Given
                    val command = SetCanonicalAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "invalid alias!")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                    coVerify(exactly = 0) { aliasRepository.findByAliasName(any()) }
                }
            }

            describe("when alias does not exist") {
                it("should return AliasNotFound error") {
                    // Given
                    val command = SetCanonicalAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "non-existent")
                    val aliasNameVO = AliasName.create("non-existent").getOrNull()!!

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns null.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe "non-existent"
                    }
                }
            }

            describe("when alias belongs to different scope") {
                it("should return InvalidAlias error") {
                    // Given
                    val requestedScopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val actualScopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Z"
                    val aliasName = "my-project"
                    val command = SetCanonicalAlias(requestedScopeId, aliasName)

                    val actualScopeIdVO = ScopeId.create(actualScopeId).getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val alias = ScopeAlias.createCustom(actualScopeIdVO, aliasNameVO)

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns alias.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe aliasName
                    }
                    coVerify(exactly = 0) {
                        scopeAliasService.assignCanonicalAlias(any(), any())
                    }
                }
            }

            describe("when repository returns an error") {
                it("should map repository error to application error") {
                    // Given
                    val command = SetCanonicalAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "my-project")
                    val aliasNameVO = AliasName.create("my-project").getOrNull()!!
                    val error = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.NotFound(
                        Clock.System.now(),
                        "ScopeAlias",
                        "my-project",
                    )

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.NotFound>().apply {
                        entityType shouldBe "ScopeAlias"
                        entityId shouldBe "my-project"
                    }
                }
            }

            describe("when service returns an error") {
                it("should map service error to application error") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val aliasName = "my-project"
                    val command = SetCanonicalAlias(scopeId, aliasName)

                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val existingAlias = ScopeAlias.createCustom(scopeIdVO, aliasNameVO)
                    val error = ScopeAliasError.DuplicateAlias(
                        Clock.System.now(),
                        aliasName,
                        scopeIdVO,
                        scopeIdVO,
                    )

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns existingAlias.right()
                    coEvery {
                        scopeAliasService.assignCanonicalAlias(
                            scopeIdVO,
                            aliasNameVO,
                        )
                    } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>()
                }
            }
        }
    })
