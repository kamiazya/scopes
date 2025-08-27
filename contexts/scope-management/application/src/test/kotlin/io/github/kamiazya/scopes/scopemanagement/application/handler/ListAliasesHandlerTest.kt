package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.ListAliases
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class ListAliasesHandlerTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("ListAliasesHandler") {
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

            val handler = ListAliasesHandler(aliasRepository, transactionManager, logger)

            describe("when listing aliases for a valid scope") {
                it("should return all aliases sorted correctly") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val query = ListAliases(scopeId)
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!

                    val now = Clock.System.now()
                    val customAlias1 = ScopeAlias.createCustom(
                        scopeIdVO,
                        AliasName.create("project-alpha").getOrNull()!!,
                    ).copy(createdAt = now - 2.hours)

                    val canonicalAlias = ScopeAlias.createCanonical(
                        scopeIdVO,
                        AliasName.create("main-project").getOrNull()!!,
                    ).copy(createdAt = now - 1.hours)

                    val customAlias2 = ScopeAlias.createCustom(
                        scopeIdVO,
                        AliasName.create("project-beta").getOrNull()!!,
                    ).copy(createdAt = now - 30.minutes)

                    val aliases = listOf(customAlias1, canonicalAlias, customAlias2)

                    coEvery { aliasRepository.findByScopeId(scopeIdVO) } returns aliases.right()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeRight()
                    val dto = result.getOrElse { throw AssertionError("Expected Right but got Left") }
                    dto.scopeId shouldBe scopeId
                    dto.totalCount shouldBe 3
                    dto.aliases.size shouldBe 3

                    // Verify sorting: canonical first
                    dto.aliases[0].aliasName shouldBe "main-project"
                    dto.aliases[0].isCanonical shouldBe true
                    dto.aliases[0].aliasType shouldBe "CANONICAL"

                    // Then by creation date
                    dto.aliases[1].aliasName shouldBe "project-alpha"
                    dto.aliases[1].isCanonical shouldBe false
                    dto.aliases[1].aliasType shouldBe "CUSTOM"

                    dto.aliases[2].aliasName shouldBe "project-beta"
                    dto.aliases[2].isCanonical shouldBe false
                    dto.aliases[2].aliasType shouldBe "CUSTOM"
                }
            }

            describe("when scope has no aliases") {
                it("should return empty list") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val query = ListAliases(scopeId)
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!

                    coEvery { aliasRepository.findByScopeId(scopeIdVO) } returns emptyList<ScopeAlias>().right()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeRight()
                    val dto = result.getOrElse { throw AssertionError("Expected Right but got Left") }
                    dto.scopeId shouldBe scopeId
                    dto.totalCount shouldBe 0
                    dto.aliases shouldBe emptyList()
                }
            }

            describe("when scope has only canonical alias") {
                it("should return single alias") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val query = ListAliases(scopeId)
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!

                    val canonicalAlias = ScopeAlias.createCanonical(
                        scopeIdVO,
                        AliasName.create("main-project").getOrNull()!!,
                    )

                    coEvery { aliasRepository.findByScopeId(scopeIdVO) } returns listOf(canonicalAlias).right()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeRight()
                    val dto = result.getOrElse { throw AssertionError("Expected Right but got Left") }
                    dto.totalCount shouldBe 1
                    dto.aliases.size shouldBe 1
                    dto.aliases[0].isCanonical shouldBe true
                }
            }

            describe("when scopeId is invalid") {
                it("should return IdInvalidFormat error for invalid format") {
                    // Given
                    val query = ListAliases("invalid-id")

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.IdInvalidFormat>().apply {
                        attemptedValue shouldBe "invalid-id"
                        expectedFormat shouldBe "ULID"
                    }
                    coVerify(exactly = 0) { aliasRepository.findByScopeId(any()) }
                }

                it("should return IdBlank error for blank id") {
                    // Given
                    val query = ListAliases("")

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.IdBlank>().apply {
                        attemptedValue shouldBe ""
                    }
                    coVerify(exactly = 0) { aliasRepository.findByScopeId(any()) }
                }
            }

            describe("when repository returns an error") {
                it("should map repository error to application error") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val query = ListAliases(scopeId)
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!
                    val error = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.NotFound(
                        Clock.System.now(),
                        "ScopeAlias",
                        scopeId,
                    )

                    coEvery { aliasRepository.findByScopeId(scopeIdVO) } returns error.left()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.NotFound>().apply {
                        entityType shouldBe "ScopeAlias"
                        entityId shouldBe scopeId
                    }
                }
            }

            describe("when all aliases are custom") {
                it("should sort by creation date only") {
                    // Given
                    val scopeId = "01HZQB5QKM0WDG7ZBHSPKT3N2Y"
                    val query = ListAliases(scopeId)
                    val scopeIdVO = ScopeId.create(scopeId).getOrNull()!!

                    val now = Clock.System.now()
                    val alias1 = ScopeAlias.createCustom(
                        scopeIdVO,
                        AliasName.create("newest").getOrNull()!!,
                    ).copy(createdAt = now)

                    val alias2 = ScopeAlias.createCustom(
                        scopeIdVO,
                        AliasName.create("middle").getOrNull()!!,
                    ).copy(createdAt = now - 1.hours)

                    val alias3 = ScopeAlias.createCustom(
                        scopeIdVO,
                        AliasName.create("oldest").getOrNull()!!,
                    ).copy(createdAt = now - 2.hours)

                    val aliases = listOf(alias1, alias2, alias3)

                    coEvery { aliasRepository.findByScopeId(scopeIdVO) } returns aliases.right()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeRight()
                    val dto = result.getOrElse { throw AssertionError("Expected Right but got Left") }
                    dto.aliases[0].aliasName shouldBe "oldest"
                    dto.aliases[1].aliasName shouldBe "middle"
                    dto.aliases[2].aliasName shouldBe "newest"
                }
            }
        }
    })
