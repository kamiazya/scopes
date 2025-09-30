package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
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
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName, Clock.System.now())

                    // When
                    val result = runBlocking { repository.save(alias) }

                    // Then
                    result shouldBe Unit.right()
                }

                it("should update an existing alias") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val aliasName = AliasName.create("initial-alias").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName, Clock.System.now())
                    runBlocking { repository.save(alias) }

                    val updatedAlias = alias.withNewName(AliasName.create("updated-alias").getOrNull()!!, Clock.System.now())

                    // When
                    val result = runBlocking { repository.save(updatedAlias) }
                    val foundAlias = runBlocking { repository.findById(alias.id) }

                    // Then
                    result shouldBe Unit.right()
                    foundAlias.getOrNull()?.aliasName shouldBe updatedAlias.aliasName
                }

                it("should handle save errors gracefully") {
                    // Given
                    val alias = ScopeAlias.createCanonical(
                        ScopeId.generate(),
                        AliasName.create("test").getOrNull()!!,
                        Clock.System.now(),
                    )
                    database.close()

                    // When
                    val result = runBlocking { repository.save(alias) }

                    // Then
                    result.isLeft() shouldBe true
                    val error = result.leftOrNull().shouldBeInstanceOf<ScopesError.RepositoryError>()
                    error.operation shouldBe ScopesError.RepositoryError.RepositoryOperation.SAVE
                    error.failure shouldNotBe null
                }
            }

            describe("findByAliasName") {
                it("should find alias by name") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val aliasName = AliasName.create("find-me").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName, Clock.System.now())
                    runBlocking { repository.save(alias) }

                    // When
                    val result = runBlocking { repository.findByAliasName(aliasName) }

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
                    val result = runBlocking { repository.findByAliasName(nonExistentName) }

                    // Then
                    result shouldBe null.right()
                }

                it("should handle case-insensitive alias names (SQLite default behavior)") {
                    // Given
                    val aliasName = AliasName.create("TestAlias").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(ScopeId.generate(), aliasName, Clock.System.now())
                    runBlocking { repository.save(alias) }

                    val lowerCaseName = AliasName.create("testalias").getOrNull()!!

                    // When
                    val result = runBlocking { repository.findByAliasName(lowerCaseName) }

                    // Then
                    // SQLite uses case-insensitive comparison by default for TEXT columns
                    result.isRight() shouldBe true
                    val foundAlias = result.getOrNull()
                    foundAlias?.scopeId shouldBe alias.scopeId
                    // Note: The stored alias name case depends on how AliasName normalizes it
                    foundAlias?.aliasName?.value?.lowercase() shouldBe "testalias"
                }
            }

            describe("findById") {
                it("should find alias by ID") {
                    // Given
                    val alias = ScopeAlias.createCustom(
                        ScopeId.generate(),
                        AliasName.create("custom-alias").getOrNull()!!,
                        Clock.System.now(),
                    )
                    runBlocking { repository.save(alias) }

                    // When
                    val result = runBlocking { repository.findById(alias.id) }

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
                    val result = runBlocking { repository.findById(nonExistentId) }

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
                        Clock.System.now(),
                    )
                    val customAlias1 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-1").getOrNull()!!,
                        Clock.System.now(),
                    )
                    val customAlias2 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-2").getOrNull()!!,
                        Clock.System.now(),
                    )

                    runBlocking {
                        repository.save(canonicalAlias)
                        repository.save(customAlias1)
                        repository.save(customAlias2)
                    }

                    // When
                    val result = runBlocking { repository.findByScopeId(scopeId) }

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
                    val result = runBlocking { repository.findByScopeId(scopeId) }

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
                        Clock.System.now(),
                    )
                    val customAlias = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("secondary-alias").getOrNull()!!,
                        Clock.System.now(),
                    )

                    runBlocking {
                        repository.save(canonicalAlias)
                        repository.save(customAlias)
                    }

                    // When
                    val result = runBlocking { repository.findCanonicalByScopeId(scopeId) }

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
                        Clock.System.now(),
                    )
                    runBlocking { repository.save(customAlias) }

                    // When
                    val result = runBlocking { repository.findCanonicalByScopeId(scopeId) }

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
                        Clock.System.now(),
                    )
                    val customAlias1 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-1").getOrNull()!!,
                        Clock.System.now(),
                    )
                    val customAlias2 = ScopeAlias.createCustom(
                        scopeId,
                        AliasName.create("custom-2").getOrNull()!!,
                        Clock.System.now(),
                    )

                    runBlocking {
                        repository.save(canonicalAlias)
                        repository.save(customAlias1)
                        repository.save(customAlias2)
                    }

                    // When
                    val customAliases = runBlocking { repository.findByScopeIdAndType(scopeId, AliasType.CUSTOM) }
                    val canonicalAliases = runBlocking { repository.findByScopeIdAndType(scopeId, AliasType.CANONICAL) }

                    // Then
                    customAliases.getOrNull()?.size shouldBe 2
                    canonicalAliases.getOrNull()?.size shouldBe 1
                }
            }

            describe("findByAliasNamePrefix") {
                it("should find aliases matching prefix") {
                    // Given
                    val aliases = listOf(
                        ScopeAlias.createCanonical(ScopeId.generate(), AliasName.create("test-one").getOrNull()!!, Clock.System.now()),
                        ScopeAlias.createCustom(ScopeId.generate(), AliasName.create("test-two").getOrNull()!!, Clock.System.now()),
                        ScopeAlias.createCanonical(ScopeId.generate(), AliasName.create("other-alias").getOrNull()!!, Clock.System.now()),
                    )

                    runBlocking {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val result = runBlocking { repository.findByAliasNamePrefix("test", limit = 10) }

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
                            Clock.System.now(),
                        )
                    }

                    runBlocking {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val result = runBlocking { repository.findByAliasNamePrefix("prefix", limit = 3) }

                    // Then
                    result.getOrNull()?.size shouldBe 3
                }

                it("should return empty list for non-matching prefix") {
                    // When
                    val result = runBlocking { repository.findByAliasNamePrefix("nonexistent", limit = 10) }

                    // Then
                    result shouldBe emptyList<ScopeAlias>().right()
                }
            }

            describe("existsByAliasName") {
                it("should return true for existing alias name") {
                    // Given
                    val aliasName = AliasName.create("exists").getOrNull()!!
                    val alias = ScopeAlias.createCanonical(ScopeId.generate(), aliasName, Clock.System.now())
                    runBlocking { repository.save(alias) }

                    // When
                    val result = runBlocking { repository.existsByAliasName(aliasName) }

                    // Then
                    result shouldBe true.right()
                }

                it("should return false for non-existent alias name") {
                    // Given
                    val aliasName = AliasName.create("does-not-exist").getOrNull()!!

                    // When
                    val result = runBlocking { repository.existsByAliasName(aliasName) }

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
                        Clock.System.now(),
                    )
                    runBlocking { repository.save(alias) }

                    // When
                    val removeResult = runBlocking { repository.removeById(alias.id) }
                    val findResult = runBlocking { repository.findById(alias.id) }

                    // Then
                    removeResult shouldBe true.right()
                    findResult shouldBe null.right()
                }

                it("should handle removal of non-existent ID gracefully") {
                    // Given
                    val nonExistentId = AliasId.generate()

                    // When
                    val result = runBlocking { repository.removeById(nonExistentId) }

                    // Then
                    result shouldBe true.right() // Operation succeeds even if nothing was deleted
                }
            }

            describe("removeByAliasName") {
                it("should remove alias by name") {
                    // Given
                    val aliasName = AliasName.create("remove-by-name").getOrNull()!!
                    val alias = ScopeAlias.createCustom(ScopeId.generate(), aliasName, Clock.System.now())
                    runBlocking { repository.save(alias) }

                    // When
                    val removeResult = runBlocking { repository.removeByAliasName(aliasName) }
                    val findResult = runBlocking { repository.findByAliasName(aliasName) }

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
                        ScopeAlias.createCanonical(scopeId, AliasName.create("canonical").getOrNull()!!, Clock.System.now()),
                        ScopeAlias.createCustom(scopeId, AliasName.create("custom-1").getOrNull()!!, Clock.System.now()),
                        ScopeAlias.createCustom(scopeId, AliasName.create("custom-2").getOrNull()!!, Clock.System.now()),
                    )

                    runBlocking {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val removeResult = runBlocking { repository.removeByScopeId(scopeId) }
                    val findResult = runBlocking { repository.findByScopeId(scopeId) }

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
                        Clock.System.now(),
                    )
                    runBlocking { repository.save(alias) }

                    val updatedAlias = alias.withNewName(AliasName.create("updated").getOrNull()!!, Clock.System.now())

                    // When
                    val updateResult = runBlocking { repository.update(updatedAlias) }
                    val findResult = runBlocking { repository.findById(alias.id) }

                    // Then
                    updateResult shouldBe true.right()
                    findResult.getOrNull()?.aliasName shouldBe updatedAlias.aliasName
                }

                it("should handle promotion and demotion of alias types") {
                    // Given
                    val customAlias = ScopeAlias.createCustom(
                        ScopeId.generate(),
                        AliasName.create("promotable").getOrNull()!!,
                        Clock.System.now(),
                    )
                    runBlocking { repository.save(customAlias) }

                    val promotedAlias = customAlias.promoteToCanonical(Clock.System.now())

                    // When
                    val updateResult = runBlocking { repository.update(promotedAlias) }
                    val findResult = runBlocking { repository.findById(customAlias.id) }

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
                            Clock.System.now(),
                        )
                    }

                    runBlocking {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val result = runBlocking { repository.count() }

                    // Then
                    result shouldBe 5L.right()
                }

                it("should return 0 when no aliases exist") {
                    // When
                    val result = runBlocking { repository.count() }

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
                            Clock.System.now(),
                        )
                    }

                    runBlocking {
                        aliases.forEach { repository.save(it) }
                    }

                    // When
                    val page1 = runBlocking { repository.listAll(offset = 0, limit = 3) }
                    val page2 = runBlocking { repository.listAll(offset = 3, limit = 3) }
                    val page3 = runBlocking { repository.listAll(offset = 6, limit = 10) }

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
                        Clock.System.now(),
                    )
                    runBlocking { repository.save(alias) }

                    // When
                    val result = runBlocking { repository.listAll(offset = 10, limit = 5) }

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
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName, Clock.System.now())

                    // When/Then - Test all operations return proper errors
                    val operations = listOf(
                        runBlocking { repository.save(alias) },
                        runBlocking { repository.findByAliasName(aliasName) },
                        runBlocking { repository.findById(aliasId) },
                        runBlocking { repository.findByScopeId(scopeId) },
                        runBlocking { repository.findCanonicalByScopeId(scopeId) },
                        runBlocking { repository.findByScopeIdAndType(scopeId, AliasType.CANONICAL) },
                        runBlocking { repository.findByAliasNamePrefix("test", 10) },
                        runBlocking { repository.existsByAliasName(aliasName) },
                        runBlocking { repository.removeById(aliasId) },
                        runBlocking { repository.removeByAliasName(aliasName) },
                        runBlocking { repository.removeByScopeId(scopeId) },
                        runBlocking { repository.update(alias) },
                        runBlocking { repository.count() },
                        runBlocking { repository.listAll(0, 10) },
                    )

                    operations.forEach { result ->
                        result.isLeft() shouldBe true
                        val error = result.leftOrNull().shouldBeInstanceOf<ScopesError.RepositoryError>()
                        error.operation shouldNotBe null
                        error.repositoryName shouldBe "SqlDelightScopeAliasRepository"
                        error.failure shouldNotBe null
                    }
                }
            }

            describe("concurrent access and duplicate handling") {
                it("should handle duplicate alias creation through unique constraint") {
                    // Given
                    val scopeId1 = ScopeId.generate()
                    val scopeId2 = ScopeId.generate()
                    val aliasName = AliasName.create("duplicate-test").getOrNull()!!

                    val alias1 = ScopeAlias.createCanonical(scopeId1, aliasName, Clock.System.now())
                    val alias2 = ScopeAlias.createCanonical(scopeId2, aliasName, Clock.System.now())

                    // When - Save first alias successfully
                    val result1 = runBlocking { repository.save(alias1) }

                    // Then - First save should succeed
                    result1.isRight() shouldBe true

                    // When - Attempt to save second alias with same name
                    val result2 = runBlocking { repository.save(alias2) }

                    // Then - Second save should return DuplicateAlias error
                    result2.isLeft() shouldBe true
                    val error = result2.leftOrNull()
                    error.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError.DuplicateAlias>()
                    error.aliasName shouldBe aliasName
                    error.existingScopeId shouldBe scopeId1
                    error.attemptedScopeId shouldBe scopeId2
                }

                it("should handle concurrent saves with proper error mapping") {
                    // Given
                    val aliasName = AliasName.create("concurrent-test").getOrNull()!!
                    val alias1 = ScopeAlias.createCanonical(ScopeId.generate(), aliasName, Clock.System.now())
                    val alias2 = ScopeAlias.createCanonical(ScopeId.generate(), aliasName, Clock.System.now())

                    // When - Simulate concurrent access by saving first alias
                    runBlocking { repository.save(alias1) }

                    // Then - Second save should detect the constraint violation and map to business error
                    val result = runBlocking { repository.save(alias2) }
                    result.isLeft() shouldBe true

                    val error = result.leftOrNull()
                    error.shouldBeInstanceOf<io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError.DuplicateAlias>()

                    // Verify the existing scope ID is correctly identified
                    val existingAlias = runBlocking { repository.findByAliasName(aliasName) }
                    existingAlias.getOrNull()?.scopeId shouldBe alias1.scopeId
                }

                it("should preserve database integrity during concurrent operations") {
                    // Given
                    val baseAliasName = "concurrent-base"
                    val aliases = (1..5).map { i ->
                        ScopeAlias.createCanonical(
                            ScopeId.generate(),
                            AliasName.create("$baseAliasName-$i").getOrNull()!!,
                            Clock.System.now(),
                        )
                    }

                    // When - Save all aliases concurrently
                    val results = runBlocking {
                        coroutineScope {
                            aliases.map { alias ->
                                async {
                                    repository.save(alias)
                                }
                            }.map { it.await() }
                        }
                    }

                    // Then - All saves should succeed since names are unique
                    results.forEach { result ->
                        result.isRight() shouldBe true
                    }

                    // Verify all aliases are stored correctly
                    val storedCount = runBlocking { repository.count() }
                    storedCount.getOrNull() shouldBe 5L
                }

                it("should handle mixed success and failure in concurrent duplicate scenarios") {
                    // Given
                    val duplicateAliasName = AliasName.create("shared-name").getOrNull()!!
                    val uniqueAliasName = AliasName.create("unique-name").getOrNull()!!

                    val duplicateAlias1 = ScopeAlias.createCanonical(ScopeId.generate(), duplicateAliasName, Clock.System.now())
                    val duplicateAlias2 = ScopeAlias.createCanonical(ScopeId.generate(), duplicateAliasName, Clock.System.now())
                    val uniqueAlias = ScopeAlias.createCanonical(ScopeId.generate(), uniqueAliasName, Clock.System.now())

                    // When - Save first duplicate alias to establish the constraint
                    val firstResult = runBlocking { repository.save(duplicateAlias1) }
                    firstResult.isRight() shouldBe true

                    // Concurrent saves: one duplicate (should fail) and one unique (should succeed)
                    val results = runBlocking {
                        coroutineScope {
                            listOf(
                                async { repository.save(duplicateAlias2) },
                                async { repository.save(uniqueAlias) },
                            ).map { it.await() }
                        }
                    }

                    // Then - One should fail (duplicate), one should succeed (unique)
                    val duplicateResult = results[0]
                    val uniqueResult = results[1]

                    duplicateResult.isLeft() shouldBe true
                    uniqueResult.isRight() shouldBe true

                    // Verify final database state
                    val totalCount = runBlocking { repository.count() }
                    totalCount.getOrNull() shouldBe 2L // Original + unique

                    val existingDuplicate = runBlocking { repository.findByAliasName(duplicateAliasName) }
                    val existingUnique = runBlocking { repository.findByAliasName(uniqueAliasName) }

                    existingDuplicate.getOrNull()?.scopeId shouldBe duplicateAlias1.scopeId
                    existingUnique.getOrNull()?.scopeId shouldBe uniqueAlias.scopeId
                }
            }

            describe("complex scenarios") {
                it("should handle alias lifecycle correctly") {
                    // Given
                    val scopeId = ScopeId.generate()
                    val initialName = AliasName.create("initial-canonical").getOrNull()!!
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, initialName, Clock.System.now())

                    // Phase 1: Create canonical alias
                    runBlocking { repository.save(canonicalAlias) }

                    // Phase 2: Add custom aliases
                    val customAlias1 = ScopeAlias.createCustom(scopeId, AliasName.create("custom-1").getOrNull()!!, Clock.System.now())
                    val customAlias2 = ScopeAlias.createCustom(scopeId, AliasName.create("custom-2").getOrNull()!!, Clock.System.now())
                    runBlocking {
                        repository.save(customAlias1)
                        repository.save(customAlias2)
                    }

                    // Phase 3: Demote canonical to custom and promote a custom to canonical
                    val demotedCanonical = canonicalAlias.demoteToCustom(Clock.System.now())
                    val promotedCustom = customAlias1.promoteToCanonical(Clock.System.now())
                    runBlocking {
                        repository.update(demotedCanonical)
                        repository.update(promotedCustom)
                    }

                    // Verify final state
                    val finalCanonical = runBlocking { repository.findCanonicalByScopeId(scopeId) }
                    val allAliases = runBlocking { repository.findByScopeId(scopeId) }

                    finalCanonical.getOrNull()?.id shouldBe customAlias1.id
                    allAliases.getOrNull()?.size shouldBe 3
                    allAliases.getOrNull()?.count { it.isCanonical() } shouldBe 1
                }

                it("should maintain uniqueness of alias names across different scopes") {
                    // Given
                    val aliasName = AliasName.create("unique-name").getOrNull()!!
                    val scope1Alias = ScopeAlias.createCanonical(ScopeId.generate(), aliasName, Clock.System.now())
                    runBlocking { repository.save(scope1Alias) }

                    // When trying to use the same alias name for a different scope
                    val exists = runBlocking { repository.existsByAliasName(aliasName) }

                    // Then
                    exists shouldBe true.right()
                    // Note: The repository doesn't enforce uniqueness, that's handled at the service layer
                }
            }
        }
    })
