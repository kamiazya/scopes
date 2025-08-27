package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.datetime.Clock

class ScopeAliasManagementServiceTest :
    DescribeSpec({
        describe("ScopeAliasManagementService") {
            val repository = mockk<ScopeAliasRepository>()
            val aliasGenerator = mockk<AliasGenerationService>()
            val service = ScopeAliasManagementService(repository, aliasGenerator)

            val scopeId = ScopeId.generate()
            val aliasName = AliasName.create("test-alias").getOrNull()!!

            beforeEach {
                // Reset mocks before each test
                io.mockk.clearMocks(repository, aliasGenerator)
            }

            describe("assignCanonicalAlias") {
                it("should assign canonical alias when no conflicts exist") {
                    val aliasSlot = slot<ScopeAlias>()

                    coEvery { repository.findByAliasName(aliasName) } returns null.right()
                    coEvery { repository.findCanonicalByScopeId(scopeId) } returns null.right()
                    coEvery { repository.save(capture(aliasSlot)) } returns Unit.right()

                    val result = service.assignCanonicalAlias(scopeId, aliasName)

                    result.shouldBeRight()
                    val savedAlias = aliasSlot.captured
                    savedAlias.scopeId shouldBe scopeId
                    savedAlias.aliasName shouldBe aliasName
                    savedAlias.aliasType shouldBe AliasType.CANONICAL

                    coVerify(exactly = 1) { repository.save(any()) }
                }

                it("should replace existing canonical alias") {
                    val existingCanonicalName = AliasName.create("old-canonical").getOrNull()!!
                    val existingCanonical = ScopeAlias.createCanonical(scopeId, existingCanonicalName)
                    val newAliasName = AliasName.create("new-alias").getOrNull()!!
                    val updateSlot = slot<ScopeAlias>()
                    val saveSlot = slot<ScopeAlias>()

                    coEvery { repository.findByAliasName(newAliasName) } returns null.right()
                    coEvery { repository.findCanonicalByScopeId(scopeId) } returns existingCanonical.right()
                    coEvery { repository.update(capture(updateSlot)) } returns true.right()
                    coEvery { repository.save(capture(saveSlot)) } returns Unit.right()

                    val result = service.assignCanonicalAlias(scopeId, newAliasName)

                    result.shouldBeRight()

                    // Should convert existing canonical to custom
                    val updatedAlias = updateSlot.captured
                    updatedAlias.scopeId shouldBe scopeId
                    updatedAlias.aliasName shouldBe existingCanonicalName
                    updatedAlias.aliasType shouldBe AliasType.CUSTOM

                    // Should save new canonical
                    val savedAlias = saveSlot.captured
                    savedAlias.scopeId shouldBe scopeId
                    savedAlias.aliasName shouldBe newAliasName
                    savedAlias.aliasType shouldBe AliasType.CANONICAL

                    coVerify(exactly = 1) { repository.update(any()) }
                    coVerify(exactly = 1) { repository.save(any()) }
                }

                it("should fail when alias already exists for different scope") {
                    val otherScopeId = ScopeId.generate()
                    val existingAlias = ScopeAlias.createCanonical(otherScopeId, aliasName)

                    coEvery { repository.findByAliasName(aliasName) } returns existingAlias.right()

                    val result = service.assignCanonicalAlias(scopeId, aliasName)

                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ScopeAliasError.DuplicateAlias>()
                    (error as ScopeAliasError.DuplicateAlias).aliasName shouldBe aliasName.value
                    error.existingScopeId shouldBe otherScopeId
                    error.attemptedScopeId shouldBe scopeId

                    coVerify(exactly = 0) { repository.save(any()) }
                }

                it("should be idempotent when assigning same canonical alias") {
                    val existingCanonical = ScopeAlias.createCanonical(scopeId, aliasName)

                    coEvery { repository.findByAliasName(aliasName) } returns existingCanonical.right()

                    val result = service.assignCanonicalAlias(scopeId, aliasName)

                    result.shouldBeRight()
                    result.getOrNull() shouldBe existingCanonical

                    // No updates or saves should occur
                    coVerify(exactly = 0) { repository.save(any()) }
                    coVerify(exactly = 0) { repository.update(any()) }
                }
            }

            describe("generateCanonicalAlias") {
                it("should generate and assign canonical alias") {
                    val aliasId = AliasId.generate()
                    val generatedName = AliasName.create("generated-alias").getOrNull()!!
                    val aliasSlot = slot<ScopeAlias>()

                    coEvery { aliasGenerator.generateCanonicalAlias(any()) } returns generatedName.right()
                    coEvery { repository.findByAliasName(generatedName) } returns null.right()
                    coEvery { repository.findCanonicalByScopeId(scopeId) } returns null.right()
                    coEvery { repository.save(capture(aliasSlot)) } returns Unit.right()

                    val result = service.generateCanonicalAlias(scopeId)

                    result.shouldBeRight()
                    val savedAlias = aliasSlot.captured
                    savedAlias.scopeId shouldBe scopeId
                    savedAlias.aliasName shouldBe generatedName
                    savedAlias.aliasType shouldBe AliasType.CANONICAL

                    coVerify(exactly = 1) { repository.save(any()) }
                }

                it("should retry generation when name collision occurs") {
                    val firstGenerated = AliasName.create("taken-alias").getOrNull()!!
                    val secondGenerated = AliasName.create("available-alias").getOrNull()!!
                    val existingAlias = ScopeAlias.createCanonical(ScopeId.generate(), firstGenerated)
                    val aliasSlot = slot<ScopeAlias>()

                    coEvery { repository.findCanonicalByScopeId(scopeId) } returns null.right()
                    coEvery { aliasGenerator.generateCanonicalAlias(any()) } returnsMany listOf(
                        firstGenerated.right(),
                        secondGenerated.right(),
                    )
                    coEvery { repository.findByAliasName(firstGenerated) } returns existingAlias.right()
                    coEvery { repository.findByAliasName(secondGenerated) } returns null.right()
                    coEvery { repository.save(capture(aliasSlot)) } returns Unit.right()

                    val result = service.generateCanonicalAlias(scopeId)

                    result.shouldBeRight()
                    aliasSlot.captured.aliasName shouldBe secondGenerated

                    coVerify(exactly = 2) { aliasGenerator.generateCanonicalAlias(any()) }
                    coVerify(exactly = 2) { repository.findByAliasName(any()) }
                }

                it("should fail after maximum retries") {
                    val generatedName = AliasName.create("always-taken").getOrNull()!!
                    val existingAlias = ScopeAlias.createCanonical(ScopeId.generate(), generatedName)

                    coEvery { repository.findCanonicalByScopeId(scopeId) } returns null.right()
                    coEvery { aliasGenerator.generateCanonicalAlias(any()) } returns generatedName.right()
                    coEvery { repository.findByAliasName(generatedName) } returns existingAlias.right()

                    val result = service.generateCanonicalAlias(scopeId)

                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ScopeAliasError.DuplicateAlias>()

                    // Default maxRetries is 10
                    coVerify(exactly = 10) { aliasGenerator.generateCanonicalAlias(any()) }
                    coVerify(exactly = 10) { repository.findByAliasName(any()) }
                }
            }

            describe("assignCustomAlias") {
                it("should create custom alias when name is available") {
                    val aliasSlot = slot<ScopeAlias>()

                    coEvery { repository.findByAliasName(aliasName) } returns null.right()
                    coEvery { repository.save(capture(aliasSlot)) } returns Unit.right()

                    val result = service.assignCustomAlias(scopeId, aliasName)

                    result.shouldBeRight()
                    val savedAlias = aliasSlot.captured
                    savedAlias.scopeId shouldBe scopeId
                    savedAlias.aliasName shouldBe aliasName
                    savedAlias.aliasType shouldBe AliasType.CUSTOM
                }

                it("should fail when alias name already exists") {
                    val existingAlias = ScopeAlias.createCustom(ScopeId.generate(), aliasName)

                    coEvery { repository.findByAliasName(aliasName) } returns existingAlias.right()

                    val result = service.assignCustomAlias(scopeId, aliasName)

                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ScopeAliasError.DuplicateAlias>()

                    coVerify(exactly = 0) { repository.save(any()) }
                }
            }

            describe("removeAlias") {
                it("should remove custom alias") {
                    val customAlias = ScopeAlias.createCustom(scopeId, aliasName)

                    coEvery { repository.findByAliasName(aliasName) } returns customAlias.right()
                    coEvery { repository.removeByAliasName(aliasName) } returns true.right()

                    val result = service.removeAlias(aliasName)

                    result.shouldBeRight()
                    result.getOrNull() shouldBe customAlias
                    coVerify(exactly = 1) { repository.removeByAliasName(aliasName) }
                }

                it("should fail when trying to remove canonical alias") {
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, aliasName)

                    coEvery { repository.findByAliasName(aliasName) } returns canonicalAlias.right()

                    val result = service.removeAlias(aliasName)

                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ScopeAliasError.CannotRemoveCanonicalAlias>()
                    (error as ScopeAliasError.CannotRemoveCanonicalAlias).scopeId shouldBe scopeId
                    error.aliasName shouldBe aliasName.value

                    coVerify(exactly = 0) { repository.removeByAliasName(any()) }
                }

                it("should fail when alias not found") {
                    coEvery { repository.findByAliasName(aliasName) } returns null.right()

                    val result = service.removeAlias(aliasName)

                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ScopeAliasError.AliasNotFound>()
                    (error as ScopeAliasError.AliasNotFound).aliasName shouldBe aliasName.value
                }
            }

            describe("resolveAlias") {
                it("should resolve alias to scope ID") {
                    val alias = ScopeAlias.createCustom(scopeId, aliasName)

                    coEvery { repository.findByAliasName(aliasName) } returns alias.right()

                    val result = service.resolveAlias(aliasName)

                    result.shouldBeRight()
                    result.getOrNull() shouldBe scopeId
                }

                it("should fail when alias not found") {
                    coEvery { repository.findByAliasName(aliasName) } returns null.right()

                    val result = service.resolveAlias(aliasName)

                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ScopeAliasError.AliasNotFound>()
                    (error as ScopeAliasError.AliasNotFound).aliasName shouldBe aliasName.value
                }
            }

            describe("getAliasesForScope") {
                it("should return all aliases for a scope") {
                    val canonical = ScopeAlias.createCanonical(scopeId, aliasName)
                    val custom1 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-1").getOrNull()!!,
                    )
                    val custom2 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-2").getOrNull()!!,
                    )
                    val aliases = listOf(canonical, custom1, custom2)

                    coEvery { repository.findByScopeId(scopeId) } returns aliases.right()

                    val result = service.getAliasesForScope(scopeId)

                    result.shouldBeRight()
                    result.getOrNull() shouldBe aliases
                }

                it("should return empty list when no aliases found") {
                    coEvery { repository.findByScopeId(scopeId) } returns emptyList<ScopeAlias>().right()

                    val result = service.getAliasesForScope(scopeId)

                    result.shouldBeRight()
                    result.getOrNull() shouldBe emptyList()
                }
            }

            describe("findAliasesByPrefix") {
                it("should return aliases matching prefix") {
                    val prefix = "test"
                    val matching1 = ScopeAlias.createCanonical(
                        scopeId,
                        AliasName.create("test-alias-1").getOrNull()!!,
                    )
                    val matching2 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("testing-alias").getOrNull()!!,
                    )
                    val matches = listOf(matching1, matching2)

                    coEvery { repository.findByAliasNamePrefix(prefix, 10) } returns matches.right()

                    val result = service.findAliasesByPrefix(prefix, 10)

                    result.shouldBeRight()
                    result.getOrNull() shouldBe matches
                }

                it("should respect limit parameter") {
                    val prefix = "test"
                    val matches = (1..5).map { i ->
                        ScopeAlias.createCustom(
                            scopeId,
                            AliasName.create("test-alias-$i").getOrNull()!!,
                        )
                    }

                    coEvery { repository.findByAliasNamePrefix(prefix, 3) } returns matches.take(3).right()

                    val result = service.findAliasesByPrefix(prefix, 3)

                    result.shouldBeRight()
                    result.getOrNull()?.size shouldBe 3
                }

                it("should use default limit when not specified") {
                    val prefix = "test"
                    val matches = listOf(
                        ScopeAlias.createCustom(scopeId, AliasName.create("test-1").getOrNull()!!),
                    )

                    coEvery { repository.findByAliasNamePrefix(prefix, 50) } returns matches.right()

                    val result = service.findAliasesByPrefix(prefix)

                    result.shouldBeRight()
                    result.getOrNull() shouldBe matches

                    coVerify { repository.findByAliasNamePrefix(prefix, 50) }
                }
            }

            describe("error propagation") {
                it("should propagate repository errors") {
                    val repositoryError = io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError.NotFound(Clock.System.now(), "Alias", "test")

                    coEvery { repository.findByAliasName(aliasName) } returns repositoryError.left()

                    val result = service.removeAlias(aliasName)

                    result.shouldBeLeft()
                    val error = result.leftOrNull()!!
                    error.shouldBeInstanceOf<ScopeAliasError.AliasNotFound>()
                }
            }
        }
    })
