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
                    val currentAlias = "project-alpha"
                    val newCanonicalAlias = "project-main"
                    val command = SetCanonicalAlias(currentAlias, newCanonicalAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newCanonicalAliasNameVO = AliasName.create(newCanonicalAlias).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)
                    val newCanonicalAliasEntity = ScopeAlias.createCustom(scopeIdVO, newCanonicalAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newCanonicalAliasNameVO) } returns newCanonicalAliasEntity.right()
                    coEvery {
                        scopeAliasService.assignCanonicalAlias(
                            scopeIdVO,
                            newCanonicalAliasNameVO,
                        )
                    } returns newCanonicalAliasEntity.copy(aliasType = AliasType.CANONICAL).right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 1) {
                        scopeAliasService.assignCanonicalAlias(
                            scopeIdVO,
                            newCanonicalAliasNameVO,
                        )
                    }
                }
            }

            describe("when alias is already canonical") {
                it("should return success without calling service (idempotent)") {
                    // Given
                    val currentAlias = "project-alpha"
                    val newCanonicalAlias = "project-main"
                    val command = SetCanonicalAlias(currentAlias, newCanonicalAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newCanonicalAliasNameVO = AliasName.create(newCanonicalAlias).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)
                    val canonicalAlias = ScopeAlias.createCanonical(scopeIdVO, newCanonicalAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newCanonicalAliasNameVO) } returns canonicalAlias.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 0) {
                        scopeAliasService.assignCanonicalAlias(any(), any())
                    }
                }
            }

            describe("when currentAlias is invalid") {
                it("should return InvalidAlias error for invalid alias name") {
                    // Given
                    val command = SetCanonicalAlias("invalid alias!", "project-main")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                    coVerify(exactly = 0) { aliasRepository.findByAliasName(any()) }
                }

                it("should return AliasNotFound error when current alias doesn't exist") {
                    // Given
                    val command = SetCanonicalAlias("non-existent", "project-main")
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

            describe("when newCanonicalAlias is invalid") {
                it("should return InvalidAlias error") {
                    // Given
                    val currentAlias = "project-alpha"
                    val command = SetCanonicalAlias(currentAlias, "invalid alias!") // Contains special char

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                }
            }

            describe("when new canonical alias does not exist") {
                it("should return AliasNotFound error") {
                    // Given
                    val currentAlias = "project-alpha"
                    val newCanonicalAlias = "non-existent"
                    val command = SetCanonicalAlias(currentAlias, newCanonicalAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newCanonicalAliasNameVO = AliasName.create(newCanonicalAlias).getOrNull()!!
                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newCanonicalAliasNameVO) } returns null.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe newCanonicalAlias
                    }
                }
            }

            describe("when aliases belong to different scopes") {
                it("should return InvalidAlias error") {
                    // Given
                    val currentAlias = "project-alpha"
                    val newCanonicalAlias = "project-main"
                    val command = SetCanonicalAlias(currentAlias, newCanonicalAlias)

                    val scopeId1 = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeId2 = "01HZQB5QKM0WDG7ZBHSPKT3N2Z" // Different scope
                    val scopeIdVO1 = ScopeId.create(scopeId1).getOrNull()!!
                    val scopeIdVO2 = ScopeId.create(scopeId2).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newCanonicalAliasNameVO = AliasName.create(newCanonicalAlias).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO1, currentAliasNameVO)
                    val newCanonicalAliasEntity = ScopeAlias.createCustom(scopeIdVO2, newCanonicalAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newCanonicalAliasNameVO) } returns newCanonicalAliasEntity.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe newCanonicalAlias
                    }
                    coVerify(exactly = 0) { scopeAliasService.assignCanonicalAlias(any(), any()) }
                }
            }

            describe("when canonical reassignment occurs") {
                it("should successfully reassign canonical alias") {
                    // Given
                    val currentAlias = "project-alpha"
                    val newCanonicalAlias = "project-beta"
                    val command = SetCanonicalAlias(currentAlias, newCanonicalAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newCanonicalAliasNameVO = AliasName.create(newCanonicalAlias).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCanonical(scopeIdVO, currentAliasNameVO)
                    val newCanonicalAliasEntity = ScopeAlias.createCustom(scopeIdVO, newCanonicalAliasNameVO)

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newCanonicalAliasNameVO) } returns newCanonicalAliasEntity.right()
                    coEvery { scopeAliasService.assignCanonicalAlias(scopeIdVO, newCanonicalAliasNameVO) } returns
                        newCanonicalAliasEntity.copy(aliasType = AliasType.CANONICAL).right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify { scopeAliasService.assignCanonicalAlias(scopeIdVO, newCanonicalAliasNameVO) }
                }
            }

            describe("when repository returns an error") {
                it("should map repository error to application error") {
                    // Given
                    val currentAlias = "project-alpha"
                    val newCanonicalAlias = "project-main"
                    val command = SetCanonicalAlias(currentAlias, newCanonicalAlias)

                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!

                    val error = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.NotFound(
                        Clock.System.now(),
                        "ScopeAlias",
                        currentAlias,
                    )

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.NotFound>().apply {
                        entityType shouldBe "ScopeAlias"
                        entityId shouldBe currentAlias
                    }
                }
            }

            describe("when service returns an error") {
                it("should map service error to application error") {
                    // Given
                    val currentAlias = "project-alpha"
                    val newCanonicalAlias = "project-main"
                    val command = SetCanonicalAlias(currentAlias, newCanonicalAlias)

                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val currentAliasNameVO = AliasName.create(currentAlias).getOrNull()!!
                    val newCanonicalAliasNameVO = AliasName.create(newCanonicalAlias).getOrNull()!!

                    val currentAliasEntity = ScopeAlias.createCustom(scopeIdVO, currentAliasNameVO)
                    val newCanonicalAliasEntity = ScopeAlias.createCustom(scopeIdVO, newCanonicalAliasNameVO)
                    val error = ScopeAliasError.DuplicateAlias(
                        Clock.System.now(),
                        newCanonicalAlias,
                        scopeIdVO,
                        scopeIdVO,
                    )

                    coEvery { aliasRepository.findByAliasName(currentAliasNameVO) } returns currentAliasEntity.right()
                    coEvery { aliasRepository.findByAliasName(newCanonicalAliasNameVO) } returns newCanonicalAliasEntity.right()
                    coEvery {
                        scopeAliasService.assignCanonicalAlias(
                            scopeIdVO,
                            newCanonicalAliasNameVO,
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
