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
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            // Default transaction behavior - just execute the block
            coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                val block = firstArg<suspend () -> Either<Any, Any>>()
                block()
            }

            val handler = AddAliasHandler(scopeAliasService, transactionManager, logger)

            describe("when adding a valid alias") {
                it("should successfully add the alias") {
                    // Given
                    val command = AddAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "my-project")
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val aliasName = AliasName.create("my-project").getOrNull()!!
                    val alias = ScopeAlias.createCustom(scopeId, aliasName)

                    coEvery { scopeAliasService.assignCustomAlias(scopeId, aliasName) } returns alias.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 1) { scopeAliasService.assignCustomAlias(scopeId, aliasName) }
                }
            }

            describe("when scopeId is invalid") {
                it("should return IdInvalidFormat error for invalid format") {
                    // Given
                    val command = AddAlias("invalid-id", "my-project")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.IdInvalidFormat>().apply {
                        attemptedValue shouldBe "invalid-id"
                        expectedFormat shouldBe "ULID"
                    }
                    coVerify(exactly = 0) { scopeAliasService.assignCustomAlias(any(), any()) }
                }

                it("should return IdBlank error for blank id") {
                    // Given
                    val command = AddAlias("", "my-project")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.IdBlank>().apply {
                        attemptedValue shouldBe ""
                    }
                    coVerify(exactly = 0) { scopeAliasService.assignCustomAlias(any(), any()) }
                }
            }

            describe("when alias name is invalid") {
                it("should return InvalidAlias error for invalid format") {
                    // Given
                    val command = AddAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "invalid alias!")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                    coVerify(exactly = 0) { scopeAliasService.assignCustomAlias(any(), any()) }
                }

                it("should return InvalidAlias error for too short alias") {
                    // Given
                    val command = AddAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "a")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "a"
                    }
                    coVerify(exactly = 0) { scopeAliasService.assignCustomAlias(any(), any()) }
                }
            }

            describe("when service returns an error") {
                it("should map DuplicateAlias error to InvalidAlias") {
                    // Given
                    val command = AddAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "existing-alias")
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val aliasName = AliasName.create("existing-alias").getOrNull()!!
                    val error = ScopeAliasError.DuplicateAlias(
                        Clock.System.now(),
                        "existing-alias",
                        ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2X").getOrNull()!!,
                        scopeId,
                    )

                    coEvery { scopeAliasService.assignCustomAlias(scopeId, aliasName) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "existing-alias"
                    }
                }

                it("should map CannotRemoveCanonicalAlias error to InvalidAlias") {
                    // Given
                    val command = AddAlias("01HZQB5QKM0WDG7ZBHSPKT3N2Y", "canonical-alias")
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val aliasName = AliasName.create("canonical-alias").getOrNull()!!
                    val error = ScopeAliasError.CannotRemoveCanonicalAlias(
                        Clock.System.now(),
                        scopeId,
                        "canonical-alias",
                    )

                    coEvery { scopeAliasService.assignCustomAlias(scopeId, aliasName) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.InvalidAlias>().apply {
                        attemptedValue shouldBe "canonical-alias"
                    }
                }
            }
        }
    })
