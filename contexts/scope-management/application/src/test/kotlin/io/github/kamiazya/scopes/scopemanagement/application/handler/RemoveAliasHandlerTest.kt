package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.RemoveAlias
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
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError as AppScopeAliasError

class RemoveAliasHandlerTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("RemoveAliasHandler") {
            val scopeAliasService = mockk<ScopeAliasManagementService>()
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            beforeEach {
                // Configure transaction manager to execute the block directly
                coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                    val block = firstArg<suspend () -> Either<Any, Any>>()
                    block.invoke()
                }
            }

            val handler = RemoveAliasHandler(scopeAliasService, transactionManager, logger)

            describe("when removing a valid alias") {
                it("should successfully remove the alias") {
                    // Given
                    val command = RemoveAlias("my-project")
                    val aliasName = AliasName.create("my-project").getOrNull()!!
                    val alias = ScopeAlias.createCustom(
                        ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!,
                        aliasName,
                    )

                    coEvery { scopeAliasService.removeAlias(aliasName) } returns alias.right()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeRight()
                    coVerify(exactly = 1) { scopeAliasService.removeAlias(aliasName) }
                }
            }

            describe("when alias name is invalid") {
                it("should return InvalidAlias error for invalid format") {
                    // Given
                    val command = RemoveAlias("invalid alias!")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasInvalidFormat>().apply {
                        attemptedValue shouldBe "invalid alias!"
                    }
                    coVerify(exactly = 0) { scopeAliasService.removeAlias(any()) }
                }

                it("should return InvalidAlias error for empty alias") {
                    // Given
                    val command = RemoveAlias("")

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasEmpty>().apply {
                        attemptedValue shouldBe ""
                    }
                    coVerify(exactly = 0) { scopeAliasService.removeAlias(any()) }
                }
            }

            describe("when service returns an error") {
                it("should map AliasNotFound error to InvalidAlias") {
                    // Given
                    val command = RemoveAlias("non-existent")
                    val aliasName = AliasName.create("non-existent").getOrNull()!!
                    val error = ScopeAliasError.AliasNotFound(Clock.System.now(), "non-existent")

                    coEvery { scopeAliasService.removeAlias(aliasName) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        aliasName shouldBe "non-existent"
                    }
                }

                it("should map CannotRemoveCanonicalAlias error to InvalidAlias") {
                    // Given
                    val command = RemoveAlias("canonical-alias")
                    val aliasName = AliasName.create("canonical-alias").getOrNull()!!
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val error = ScopeAliasError.CannotRemoveCanonicalAlias(
                        Clock.System.now(),
                        scopeId,
                        "canonical-alias",
                    )

                    coEvery { scopeAliasService.removeAlias(aliasName) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<AppScopeAliasError.CannotRemoveCanonicalAlias>().apply {
                        aliasName shouldBe "canonical-alias"
                    }
                }

                it("should handle attempting to remove a canonical alias") {
                    // Given
                    val command = RemoveAlias("generated-alias")
                    val aliasName = AliasName.create("generated-alias").getOrNull()!!
                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val error = ScopeAliasError.CannotRemoveCanonicalAlias(
                        Clock.System.now(),
                        scopeId,
                        "generated-alias",
                    )

                    coEvery { scopeAliasService.removeAlias(aliasName) } returns error.left()

                    // When
                    val result = handler(command)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<AppScopeAliasError.CannotRemoveCanonicalAlias>().apply {
                        aliasName shouldBe "generated-alias"
                    }
                    coVerify(exactly = 1) { scopeAliasService.removeAlias(aliasName) }
                }
            }
        }
    })
