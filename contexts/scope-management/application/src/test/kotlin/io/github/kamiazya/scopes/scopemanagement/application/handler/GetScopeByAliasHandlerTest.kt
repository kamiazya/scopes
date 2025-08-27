package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeByAliasQuery
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
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

class GetScopeByAliasHandlerTest :
    DescribeSpec({

        beforeEach {
            clearAllMocks()
        }

        describe("GetScopeByAliasHandler") {
            val aliasRepository = mockk<ScopeAliasRepository>()
            val scopeRepository = mockk<ScopeRepository>()
            val transactionManager = mockk<TransactionManager>()
            val logger = mockk<Logger>(relaxed = true)

            beforeEach {
                // Configure transaction manager to execute the block directly
                coEvery { transactionManager.inTransaction<Any, Any>(any()) } coAnswers {
                    val block = firstArg<suspend () -> Either<Any, Any>>()
                    block.invoke()
                }
            }

            val handler = GetScopeByAliasHandler(
                aliasRepository,
                scopeRepository,
                transactionManager,
                logger,
            )

            describe("when alias exists and scope exists") {
                it("should return the scope") {
                    // Given
                    val aliasName = "project-name"
                    val query = GetScopeByAliasQuery(aliasName)

                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasNameVO)
                    val scope = Scope.create(
                        title = "Test Project",
                        description = "Test Description",
                        parentId = null,
                    ).getOrNull()!!.copy(id = scopeId)

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns alias.right()
                    coEvery { scopeRepository.findById(scopeId) } returns scope.right()
                    coEvery { aliasRepository.findByScopeId(scopeId) } returns listOf(alias).right()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeRight()
                    val dto = result.getOrNull()!!
                    dto.shouldBeInstanceOf<ScopeDto>()
                    dto.id shouldBe scopeId.value
                    dto.title shouldBe "Test Project"
                    dto.aliases.size shouldBe 1
                    dto.aliases[0].aliasName shouldBe aliasName
                    dto.aliases[0].isCanonical shouldBe true

                    coVerify(exactly = 1) { aliasRepository.findByAliasName(aliasNameVO) }
                    coVerify(exactly = 1) { scopeRepository.findById(scopeId) }
                    coVerify(exactly = 1) { aliasRepository.findByScopeId(scopeId) }
                }
            }

            describe("when alias name is invalid") {
                it("should return ScopeInputError.AliasInvalidFormat") {
                    // Given
                    val invalidAlias = "invalid alias!"
                    val query = GetScopeByAliasQuery(invalidAlias)

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasInvalidFormat>().apply {
                        attemptedValue shouldBe invalidAlias
                    }
                    coVerify(exactly = 0) { aliasRepository.findByAliasName(any()) }
                }
            }

            describe("when alias does not exist") {
                it("should return AliasNotFound error") {
                    // Given
                    val aliasName = "non-existent"
                    val query = GetScopeByAliasQuery(aliasName)
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns null.right()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe aliasName
                    }
                    coVerify(exactly = 1) { aliasRepository.findByAliasName(aliasNameVO) }
                    coVerify(exactly = 0) { scopeRepository.findById(any()) }
                }
            }

            describe("when alias exists but scope does not exist") {
                it("should return AliasNotFound error") {
                    // Given
                    val aliasName = "project-name"
                    val query = GetScopeByAliasQuery(aliasName)

                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasNameVO)

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns alias.right()
                    coEvery { scopeRepository.findById(scopeId) } returns null.right()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<ScopeInputError.AliasNotFound>().apply {
                        attemptedValue shouldBe aliasName
                    }
                    coVerify(exactly = 1) { aliasRepository.findByAliasName(aliasNameVO) }
                    coVerify(exactly = 1) { scopeRepository.findById(scopeId) }
                }
            }

            describe("when repository returns error") {
                it("should propagate repository error when finding alias") {
                    // Given
                    val aliasName = "project-name"
                    val query = GetScopeByAliasQuery(aliasName)
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val error = PersistenceError.StorageUnavailable(
                        Clock.System.now(),
                        "find",
                        null,
                    )

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns error.left()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.StorageUnavailable>()
                }

                it("should propagate repository error when finding scope") {
                    // Given
                    val aliasName = "project-name"
                    val query = GetScopeByAliasQuery(aliasName)

                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasNameVO)
                    val error = PersistenceError.StorageUnavailable(
                        Clock.System.now(),
                        "find",
                        null,
                    )

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns alias.right()
                    coEvery { scopeRepository.findById(scopeId) } returns error.left()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeLeft()
                    result.leftOrNull()!!.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.application.error.PersistenceError.StorageUnavailable>()
                }
            }

            describe("when fetching aliases for scope fails") {
                it("should continue with empty aliases list") {
                    // Given
                    val aliasName = "project-name"
                    val query = GetScopeByAliasQuery(aliasName)

                    val scopeId = ScopeId.create("01HZQB5QKM0WDG7ZBHSPKT3N2Y").getOrNull()!!
                    val aliasNameVO = AliasName.create(aliasName).getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasNameVO)
                    val scope = Scope.create(
                        title = "Test Project",
                        description = "Test Description",
                        parentId = null,
                    ).getOrNull()!!.copy(id = scopeId)

                    val error = PersistenceError.StorageUnavailable(
                        Clock.System.now(),
                        "find",
                        null,
                    )

                    coEvery { aliasRepository.findByAliasName(aliasNameVO) } returns alias.right()
                    coEvery { scopeRepository.findById(scopeId) } returns scope.right()
                    coEvery { aliasRepository.findByScopeId(scopeId) } returns error.left()

                    // When
                    val result = handler(query)

                    // Then
                    result.shouldBeRight()
                    val dto = result.getOrNull()!!
                    dto.shouldBeInstanceOf<ScopeDto>()
                    dto.id shouldBe scopeId.value
                    dto.aliases.isEmpty() shouldBe true
                }
            }
        }
    })
