package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.fail
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ScopeAliasManagementServiceTest :
    DescribeSpec({
        val aliasRepository = mockk<ScopeAliasRepository>()
        val aliasGenerationService = mockk<AliasGenerationService>()
        val service = ScopeAliasManagementService(aliasRepository, aliasGenerationService)

        val scopeId = ScopeId.generate()
        val aliasName = AliasName.create("test-alias").getOrNull()!!
        val newAliasName = AliasName.create("new-alias").getOrNull()!!

        beforeEach {
            clearMocks(aliasRepository, aliasGenerationService)
        }

        describe("assignCanonicalAlias") {
            it("should create new canonical alias when none exists") {
                coEvery { aliasRepository.findByAliasName(aliasName) } returns null.right()
                coEvery { aliasRepository.findCanonicalByScopeId(scopeId) } returns null.right()
                coEvery { aliasRepository.save(any()) } returns Unit.right()

                val result = service.assignCanonicalAlias(scopeId, aliasName)

                result.isRight() shouldBe true
                result.fold(
                    { fail("Expected success but got error: $it") },
                    { alias ->
                        alias.scopeId shouldBe scopeId
                        alias.aliasName shouldBe aliasName
                        alias.aliasType shouldBe AliasType.CANONICAL
                    },
                )

                coVerify(exactly = 1) {
                    aliasRepository.findByAliasName(aliasName)
                    aliasRepository.findCanonicalByScopeId(scopeId)
                    aliasRepository.save(any())
                }
            }

            it("should fail when alias name already exists for different scope") {
                val otherScopeId = ScopeId.generate()
                val existingAlias = ScopeAlias.createCanonical(otherScopeId, aliasName)

                coEvery { aliasRepository.findByAliasName(aliasName) } returns existingAlias.right()

                val result = service.assignCanonicalAlias(scopeId, aliasName)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeAliasError.DuplicateAlias>()
                        error.aliasName shouldBe aliasName.value
                        error.existingScopeId shouldBe otherScopeId
                        error.attemptedScopeId shouldBe scopeId
                    },
                    { fail("Expected error but got success: $it") },
                )
            }
        }

        describe("assignCustomAlias") {
            it("should create new custom alias") {
                coEvery { aliasRepository.findByAliasName(aliasName) } returns null.right()
                coEvery { aliasRepository.save(any()) } returns Unit.right()

                val result = service.assignCustomAlias(scopeId, aliasName)

                result.isRight() shouldBe true
                result.fold(
                    { fail("Expected success but got error: $it") },
                    { alias ->
                        alias.scopeId shouldBe scopeId
                        alias.aliasName shouldBe aliasName
                        alias.aliasType shouldBe AliasType.CUSTOM
                    },
                )

                coVerify(exactly = 1) {
                    aliasRepository.findByAliasName(aliasName)
                    aliasRepository.save(any())
                }
            }

            it("should fail when alias name already exists") {
                val existingAlias = ScopeAlias.createCustom(scopeId, aliasName)

                coEvery { aliasRepository.findByAliasName(aliasName) } returns existingAlias.right()

                val result = service.assignCustomAlias(scopeId, aliasName)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeAliasError.DuplicateAlias>()
                        error.aliasName shouldBe aliasName.value
                    },
                    { fail("Expected error but got success: $it") },
                )
            }
        }

        describe("generateCanonicalAlias") {
            it("should generate unique canonical alias on first attempt") {
                val generatedName = AliasName.create("fluffy-mountain").getOrNull()!!

                every { aliasGenerationService.generateCanonicalAlias(any()) } returns generatedName.right()
                coEvery { aliasRepository.findByAliasName(generatedName) } returns null.right()
                coEvery { aliasRepository.findCanonicalByScopeId(scopeId) } returns null.right()
                coEvery { aliasRepository.save(any()) } returns Unit.right()

                val result = service.generateCanonicalAlias(scopeId)

                result.isRight() shouldBe true
                result.fold(
                    { fail("Expected success but got error: $it") },
                    { alias ->
                        alias.scopeId shouldBe scopeId
                        alias.aliasName shouldBe generatedName
                        alias.aliasType shouldBe AliasType.CANONICAL
                    },
                )

                verify(exactly = 1) {
                    aliasGenerationService.generateCanonicalAlias(any())
                }
                coVerify(exactly = 1) {
                    aliasRepository.findByAliasName(generatedName)
                    aliasRepository.findCanonicalByScopeId(scopeId)
                    aliasRepository.save(any())
                }
            }

            it("should fail after max retries") {
                val collisionName = AliasName.create("common-name").getOrNull()!!
                val existingAlias = ScopeAlias.createCustom(ScopeId.generate(), collisionName)

                // All attempts return the same name that already exists
                every { aliasGenerationService.generateCanonicalAlias(any()) } returns collisionName.right()
                coEvery { aliasRepository.findByAliasName(collisionName) } returns existingAlias.right()

                val result = service.generateCanonicalAlias(scopeId, maxRetries = 3)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeAliasError.AliasGenerationFailed>()
                        error.scopeId shouldBe scopeId
                        error.retryCount shouldBe 3
                    },
                    { fail("Expected error but got success: $it") },
                )

                verify(exactly = 3) {
                    aliasGenerationService.generateCanonicalAlias(any())
                }
                coVerify(exactly = 3) {
                    aliasRepository.findByAliasName(collisionName)
                }
            }
        }

        describe("removeAlias") {
            it("should remove custom alias") {
                val customAlias = ScopeAlias.createCustom(scopeId, aliasName)

                coEvery { aliasRepository.findByAliasName(aliasName) } returns customAlias.right()
                coEvery { aliasRepository.removeByAliasName(aliasName) } returns true.right()

                val result = service.removeAlias(aliasName)

                result.isRight() shouldBe true
                result.fold(
                    { fail("Expected success but got error: $it") },
                    { alias ->
                        alias shouldBe customAlias
                    },
                )

                coVerify(exactly = 1) {
                    aliasRepository.findByAliasName(aliasName)
                    aliasRepository.removeByAliasName(aliasName)
                }
            }

            it("should fail when trying to remove canonical alias") {
                val canonicalAlias = ScopeAlias.createCanonical(scopeId, aliasName)

                coEvery { aliasRepository.findByAliasName(aliasName) } returns canonicalAlias.right()

                val result = service.removeAlias(aliasName)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeAliasError.CannotRemoveCanonicalAlias>()
                        error.scopeId shouldBe scopeId
                        error.aliasName shouldBe aliasName.value
                    },
                    { fail("Expected error but got success: $it") },
                )
            }

            it("should fail when alias not found") {
                coEvery { aliasRepository.findByAliasName(aliasName) } returns null.right()

                val result = service.removeAlias(aliasName)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<ScopeAliasError.AliasNotFound>()
                        error.aliasName shouldBe aliasName.value
                    },
                    { fail("Expected error but got success: $it") },
                )
            }
        }
    })
