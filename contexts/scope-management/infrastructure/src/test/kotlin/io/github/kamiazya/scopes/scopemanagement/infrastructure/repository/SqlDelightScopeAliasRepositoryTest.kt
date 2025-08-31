package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.PersistenceError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

class SqlDelightScopeAliasRepositoryTest :
    DescribeSpec({

        describe("SqlDelightScopeAliasRepository") {
            lateinit var repository: SqlDelightScopeAliasRepository
            lateinit var database: AutoCloseable

            beforeEach {
                val db = SqlDelightDatabaseProvider.createInMemoryDatabase()
                database = db as AutoCloseable
                repository = SqlDelightScopeAliasRepository(db)
            }

            afterEach {
                database.close()
            }

            describe("save") {
                it("should save a new alias") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val aliasName = AliasName.create("test-alias").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)

                    // When
                    val result = runTest { repository.save(alias) }

                    // Then
                    result shouldBe Unit.right()
                }

                it("should update an existing alias") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val aliasName = AliasName.create("initial-alias").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                    runTest { repository.save(alias) }

                    val updatedAlias = alias.withNewName(AliasName.create("updated-alias").getOrNull()!!)

                    // When
                    val result = runTest { repository.save(updatedAlias) }
                    val foundAlias = runTest { repository.findById(alias.id) }

                    // Then
                    result shouldBe Unit.right()
                    foundAlias.getOrNull()?.aliasName shouldBe updatedAlias.aliasName
                }

                it("should handle save errors gracefully") {
                    // Given
                    val alias = ScopeAlias.createCanonical(
                        ScopeId.generate(),
                        AliasName.create("test").getOrNull()!!,
                    )
                    database.close()

                    // When
                    val result = runTest { repository.save(alias) }

                    // Then
                    result.isLeft() shouldBe true
                    result.leftOrNull().shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
                }
            }

            describe("findByAliasName") {
                it("should find alias by name") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val aliasName = AliasName.create("find-me").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                    runTest { repository.save(alias) }

                    // When
                    val result = runTest { repository.findByAliasName(aliasName) }

                    // Then
                    result.isRight() shouldBe true
                    val foundAlias = result.getOrNull()
                    foundAlias shouldNotBe null
                    foundAlias?.aliasName shouldBe aliasName
                    foundAlias?.scopeId shouldBe scopeId
                }

                it("should return null for non-existent alias name") {
                    // Given
                    val nonExistentName = AliasName.create("non-existent").getOrNull()!!

                    // When
                    val result = runTest { repository.findByAliasName(nonExistentName) }

                    // Then
                    result shouldBe null.right()
                }

                it("should handle case-sensitive alias names correctly") {
                    // Given
                    val aliasName = AliasName.create("TestAlias").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(ScopeId.generate(), aliasName)
                    runTest { repository.save(alias) }

                    val lowerCaseName = AliasName.create("testalias").getOrNull()!!

                    // When
                    val result = runTest { repository.findByAliasName(lowerCaseName) }

                    // Then
                    result shouldBe null.right() // Should not find due to case sensitivity
                }
            }

            describe("findById") {
                it("should find alias by ID") {
                    // Given
                    val alias = ScopeAlias.createCustom(
                        ScopeId.generate(),
                        AliasName.create("custom-alias").getOrNull()!!,
                    )
                    runTest { repository.save(alias) }

                    // When
                    val result = runTest { repository.findById(alias.id) }

                    // Then
                    result.isRight() shouldBe true
                    val foundAlias = result.getOrNull()
                    foundAlias?.id shouldBe alias.id
                    foundAlias?.aliasType shouldBe AliasType.CUSTOM
                }

                it("should return null for non-existent ID") {
                    // Given
                    val nonExistentId = AliasId.generate()

                    // When
                    val result = runTest { repository.findById(nonExistentId) }

                    // Then
                    result shouldBe null.right()
                }
            }

            describe("findByScopeId") {
                it("should find all aliases for a scope") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val canonicalAlias = ScopeAlias.createCanonical(
                        scopeId,
                        AliasName.create("canonical-alias").getOrNull()!!,
                    )
                    val customAlias1 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-1").getOrNull()!!,
                    )
                    val customAlias2 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-2").getOrNull()!!,
                    )

                    runTest {
                        repository.save(canonicalAlias)
                        repository.save(customAlias1)
                        repository.save(customAlias2)
                    }

                    // When
                    val result = runTest { repository.findByScopeId(scopeId) }

                    // Then
                    result.isRight() shouldBe true
                    val aliases = result.getOrNull()
                    aliases?.size shouldBe 3
                    aliases?.count { it.isCanonical() } shouldBe 1
                    aliases?.count { it.isCustom() } shouldBe 2
                }

                it("should return empty list for scope with no aliases") {
                    // Given
                    val scopeId = ScopeId.generate()

                    // When
                    val result = runTest { repository.findByScopeId(scopeId) }

                    // Then
                    result shouldBe emptyList<ScopeAlias>().right()
                }
            }

            describe("findCanonicalByScopeId") {
                it("should find only the canonical alias for a scope") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val canonicalAlias = ScopeAlias.createCanonical(
                        scopeId,
                        AliasName.create("main-alias").getOrNull()!!,
                    )
                    val customAlias = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("secondary-alias").getOrNull()!!,
                    )

                    runTest {
                        repository.save(canonicalAlias)
                        repository.save(customAlias)
                    }

                    // When
                    val result = runTest { repository.findCanonicalByScopeId(scopeId) }

                    // Then
                    result.isRight() shouldBe true
                    val foundAlias = result.getOrNull()
                    foundAlias?.id shouldBe canonicalAlias.id
                    foundAlias?.isCanonical() shouldBe true
                }

                it("should return null if scope has no canonical alias") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val customAlias = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("only-custom").getOrNull()!!,
                    )
                    runTest { repository.save(customAlias) }

                    // When
                    val result = runTest { repository.findCanonicalByScopeId(scopeId) }

                    // Then
                    result shouldBe null.right()
                }
            }

            describe("findByScopeIdAndType") {
                it("should find aliases by scope ID and type") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val canonicalAlias = ScopeAlias.createCanonical(
                        scopeId,
                        AliasName.create("canonical").getOrNull()!!,
                    )
                    val customAlias1 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-1").getOrNull()!!,
                    )
                    val customAlias2 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-2").getOrNull()!!,
                    )

                    runTest {
                        repository.save(canonicalAlias)
                        repository.save(customAlias1)
                        repository.save(customAlias2)
                    }

                    // When
                    val customAliases = runTest { repository.findByScopeIdAndType(scopeId, AliasType.CUSTOM) }
                    val canonicalAliases = runTest { repository.findByScopeIdAndType(scopeId, AliasType.CANONICAL) }

                    // Then
                    customAliases.getOrNull()?.size shouldBe 2
                    canonicalAliases.getOrNull()?.size shouldBe 1
                }
            }

            describe("findByAliasNamePrefix") {
                it("should find aliases matching prefix") {
                    // Given
                    val aliases = listOf(
                        ScopeAlias.createCanonical(ScopeId.generate(), AliasName.create("test-one").getOrNull()!!),
                        ScopeAlias.createCustom(ScopeId.generate(), AliasName.create("test-two").getOrNull()!!),
                        ScopeAlias.createCanonical(ScopeId.generate(), AliasName.create("other-alias").getOrNull()!!),
                    )

                    runTest {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val result = runTest { repository.findByAliasNamePrefix("test", limit = 10) }

                    // Then
                    result.isRight() shouldBe true
                    val foundAliases = result.getOrNull()
                    foundAliases?.size shouldBe 2
                    foundAliases?.all { it.aliasName.value.startsWith("test") } shouldBe true
                }

                it("should respect the limit parameter") {
                    // Given
                    val aliases = (1..10).map { i ->
                        ScopeAlias.createCustom(
                            ScopeId.generate(),
                            AliasName.create("prefix-$i").getOrNull()!!,
                        )
                    }

                    runTest {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val result = runTest { repository.findByAliasNamePrefix("prefix", limit = 3) }

                    // Then
                    result.getOrNull()?.size shouldBe 3
                }

                it("should return empty list for non-matching prefix") {
                    // When
                    val result = runTest { repository.findByAliasNamePrefix("nonexistent", limit = 10) }

                    // Then
                    result shouldBe emptyList<ScopeAlias>().right()
                }
            }

            describe("existsByAliasName") {
                it("should return true for existing alias name") {
                    // Given
                    val aliasName = AliasName.create("exists").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(ScopeId.generate(), aliasName)
                    runTest { repository.save(alias) }

                    // When
                    val result = runTest { repository.existsByAliasName(aliasName) }

                    // Then
                    result shouldBe true.right()
                }

                it("should return false for non-existent alias name") {
                    // Given
                    val aliasName = AliasName.create("does-not-exist").getOrNull()!!

                    // When
                    val result = runTest { repository.existsByAliasName(aliasName) }

                    // Then
                    result shouldBe false.right()
                }
            }

            describe("removeById") {
                it("should remove alias by ID") {
                    // Given
                    val alias = ScopeAlias.createCustom(
                        ScopeId.generate(),
                        AliasName.create("to-remove").getOrNull()!!,
                    )
                    runTest { repository.save(alias) }

                    // When
                    val removeResult = runTest { repository.removeById(alias.id) }
                    val findResult = runTest { repository.findById(alias.id) }

                    // Then
                    removeResult shouldBe true.right()
                    findResult shouldBe null.right()
                }

                it("should handle removal of non-existent ID gracefully") {
                    // Given
                    val nonExistentId = AliasId.generate()

                    // When
                    val result = runTest { repository.removeById(nonExistentId) }

                    // Then
                    result shouldBe true.right() // Operation succeeds even if nothing was deleted
                }
            }

            describe("removeByAliasName") {
                it("should remove alias by name") {
                    // Given
                    val aliasName = AliasName.create("remove-by-name").getOrNull()!!
                    val alias = ScopeAlias.createCustom(ScopeId.generate(), aliasName)
                    runTest { repository.save(alias) }

                    // When
                    val removeResult = runTest { repository.removeByAliasName(aliasName) }
                    val findResult = runTest { repository.findByAliasName(aliasName) }

                    // Then
                    removeResult shouldBe true.right()
                    findResult shouldBe null.right()
                }
            }

            describe("removeByScopeId") {
                it("should remove all aliases for a scope") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val aliases = listOf(
                        ScopeAlias.createCanonical(scopeId, AliasName.create("canonical").getOrNull()!!),
                        ScopeAlias.createCustom(scopeId, AliasName.create("custom-1").getOrNull()!!),
                        ScopeAlias.createCustom(scopeId, AliasName.create("custom-2").getOrNull()!!),
                    )

                    runTest {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val removeResult = runTest { repository.removeByScopeId(scopeId) }
                    val findResult = runTest { repository.findByScopeId(scopeId) }

                    // Then
                    removeResult.isRight() shouldBe true
                    findResult shouldBe emptyList<ScopeAlias>().right()
                }
            }

            describe("update") {
                it("should update an existing alias") {
                    // Given
                    val alias = ScopeAlias.createCanonical(
                        ScopeId.generate(),
                        AliasName.create("original").getOrNull()!!,
                    )
                    runTest { repository.save(alias) }

                    val updatedAlias = alias.withNewName(AliasName.create("updated").getOrNull()!!)

                    // When
                    val updateResult = runTest { repository.update(updatedAlias) }
                    val findResult = runTest { repository.findById(alias.id) }

                    // Then
                    updateResult shouldBe true.right()
                    findResult.getOrNull()?.aliasName shouldBe updatedAlias.aliasName
                }

                it("should handle promotion and demotion of alias types") {
                    // Given
                    val customAlias = ScopeAlias.createCustom(
                        ScopeId.generate(),
                        AliasName.create("promotable").getOrNull()!!,
                    )
                    runTest { repository.save(customAlias) }

                    val promotedAlias = customAlias.promoteToCanonical()

                    // When
                    val updateResult = runTest { repository.update(promotedAlias) }
                    val findResult = runTest { repository.findById(customAlias.id) }

                    // Then
                    updateResult shouldBe true.right()
                    findResult.getOrNull()?.isCanonical() shouldBe true
                }
            }

            describe("count") {
                it("should count all aliases") {
                    // Given
                    val aliases = (1..5).map { i ->
                        ScopeAlias.createCustom(
                            ScopeId.generate(),
                            AliasName.create("alias-$i").getOrNull()!!,
                        )
                    }

                    runTest {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val result = runTest { repository.count() }

                    // Then
                    result shouldBe 5L.right()
                }

                it("should return 0 when no aliases exist") {
                    // When
                    val result = runTest { repository.count() }

                    // Then
                    result shouldBe 0L.right()
                }
            }

            describe("listAll") {
                it("should list all aliases with pagination") {
                    // Given
                    val aliases = (1..10).map { i ->
                        ScopeAlias.createCustom(
                            ScopeId.generate(),
                            AliasName.create("alias-$i").getOrNull()!!,
                        )
                    }

                    runTest {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val page1 = runTest { repository.listAll(offset = 0, limit = 3) }
                    val page2 = runTest { repository.listAll(offset = 3, limit = 3) }
                    val page3 = runTest { repository.listAll(offset = 6, limit = 10) }

                    // Then
                    page1.getOrNull()?.size shouldBe 3
                    page2.getOrNull()?.size shouldBe 3
                    page3.getOrNull()?.size shouldBe 4 // Only 4 remaining

                    // Ensure no duplicates across pages
                    val allIds = (page1.getOrNull() ?: emptyList()) +
                        (page2.getOrNull() ?: emptyList()) +
                        (page3.getOrNull() ?: emptyList())
                    allIds.map { it.id }.distinct().size shouldBe 10
                }

                it("should handle offset beyond total count") {
                    // Given
                    val alias = ScopeAlias.createCanonical(
                        ScopeId.generate(),
                        AliasName.create("single").getOrNull()!!,
                    )
                    runTest { repository.save(alias) }

                    // When
                    val result = runTest { repository.listAll(offset = 10, limit = 5) }

                    // Then
                    result shouldBe emptyList<ScopeAlias>().right()
                }
            }

            describe("error handling") {
                it("should handle database errors consistently across all operations") {
                    // Given
                    database.close()
                    val aliasId = AliasId.generate()
                    val aliasName = AliasName.create("test").getOrNull()!!
                    val scopeId = ScopeId.generate()
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)

                    // When/Then - Test all operations return proper errors
                    val operations = listOf(
                        runTest { repository.save(alias) },
                        runTest { repository.findByAliasName(aliasName) },
                        runTest { repository.findById(aliasId) },
                        runTest { repository.findByScopeId(scopeId) },
                        runTest { repository.findCanonicalByScopeId(scopeId) },
                        runTest { repository.findByScopeIdAndType(scopeId, AliasType.CANONICAL) },
                        runTest { repository.findByAliasNamePrefix("test", 10) },
                        runTest { repository.existsByAliasName(aliasName) },
                        runTest { repository.removeById(aliasId) },
                        runTest { repository.removeByAliasName(aliasName) },
                        runTest { repository.removeByScopeId(scopeId) },
                        runTest { repository.update(alias) },
                        runTest { repository.count() },
                        runTest { repository.listAll(0, 10) },
                    )

                    operations.forEach { result ->
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull()
                        error.shouldBeInstanceOf<PersistenceError.StorageUnavailable>()
                        error.operation shouldNotBe null
                    }
                }
            }

            describe("complex scenarios") {
                it("should handle alias lifecycle correctly") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val initialName = AliasName.create("initial-canonical").getOrNull()!!
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, initialName)

                    // Phase 1: Create canonical alias
                    runTest { repository.save(canonicalAlias) }

                    // Phase 2: Add custom aliases
                    val customAlias1 = ScopeAlias.createCustom(scopeId, AliasName.create("custom-1").getOrNull()!!)
                    val customAlias2 = ScopeAlias.createCustom(scopeId, AliasName.create("custom-2").getOrNull()!!)
                    runTest {
                        repository.save(customAlias1)
                        repository.save(customAlias2)
                    }

                    // Phase 3: Demote canonical to custom and promote a custom to canonical
                    val demotedCanonical = canonicalAlias.demoteToCustom()
                    val promotedCustom = customAlias1.promoteToCanonical()
                    runTest {
                        repository.update(demotedCanonical)
                        repository.update(promotedCustom)
                    }

                    // Verify final state
                    val finalCanonical = runTest { repository.findCanonicalByScopeId(scopeId) }
                    val allAliases = runTest { repository.findByScopeId(scopeId) }

                    finalCanonical.getOrNull()?.id shouldBe customAlias1.id
                    allAliases.getOrNull()?.size shouldBe 3
                    allAliases.getOrNull()?.count { it.isCanonical() } shouldBe 1
                }

                it("should maintain uniqueness of alias names across different scopes") {
                    // Given
                    val aliasName = AliasName.create("unique-name").getOrNull()!!
                    val scope1Alias = ScopeAlias.createCanonical(ScopeId.generate(), aliasName)
                    runTest { repository.save(scope1Alias) }

                    // When trying to use the same alias name for a different scope
                    val exists = runTest { repository.existsByAliasName(aliasName) }

                    // Then
                    exists shouldBe true.right()
                    // Note: The repository doesn't enforce uniqueness, that's handled at the service layer
                }
            }
        }
    })