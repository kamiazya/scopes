package io.github.kamiazya.scopes.scopemanagement.infrastructure.repository

import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

class InMemoryScopeAliasRepositoryTest :
    DescribeSpec({

        describe("InMemoryScopeAliasRepository") {
            lateinit var repository: InMemoryScopeAliasRepository
            lateinit var scopeId: ScopeId
            lateinit var aliasName: AliasName
            lateinit var alias: ScopeAlias

            beforeEach {
                repository = InMemoryScopeAliasRepository()
                scopeId = ScopeId.generate()
                aliasName = AliasName.create("test-alias").getOrNull()!!
                alias = ScopeAlias.createCanonical(scopeId, aliasName)
            }

            describe("save") {
                it("should save an alias successfully") {
                    // When
                    val result = repository.save(alias)

                    // Then
                    result.shouldBeRight()
                }

                it("should allow saving multiple aliases") {
                    // Given
                    val alias1 = ScopeAlias.createCanonical(scopeId, AliasName.create("alias1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("alias2").getOrNull()!!)

                    // When
                    val result1 = repository.save(alias1)
                    val result2 = repository.save(alias2)

                    // Then
                    result1.shouldBeRight()
                    result2.shouldBeRight()
                }
            }

            describe("findByAliasName") {
                it("should return null when alias does not exist") {
                    // Given
                    val nonExistentName = AliasName.create("non-existent").getOrNull()!!

                    // When
                    val result = repository.findByAliasName(nonExistentName)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull().shouldBeNull()
                }

                it("should return alias when it exists") {
                    // Given
                    repository.save(alias)

                    // When
                    val result = repository.findByAliasName(aliasName)

                    // Then
                    result.shouldBeRight()
                    val foundAlias = result.getOrNull()
                    foundAlias.shouldNotBeNull()
                    foundAlias.id shouldBe alias.id
                    foundAlias.aliasName shouldBe aliasName
                }
            }

            describe("findById") {
                it("should return null when alias ID does not exist") {
                    // Given
                    val nonExistentId = AliasId.generate()

                    // When
                    val result = repository.findById(nonExistentId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull().shouldBeNull()
                }

                it("should return alias when it exists") {
                    // Given
                    repository.save(alias)

                    // When
                    val result = repository.findById(alias.id)

                    // Then
                    result.shouldBeRight()
                    val foundAlias = result.getOrNull()
                    foundAlias.shouldNotBeNull()
                    foundAlias.id shouldBe alias.id
                }
            }

            describe("findByScopeId") {
                it("should return empty list when no aliases exist for scope") {
                    // Given
                    val otherScopeId = ScopeId.generate()

                    // When
                    val result = repository.findByScopeId(otherScopeId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull()!!.shouldHaveSize(0)
                }

                it("should return all aliases for a scope") {
                    // Given
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, AliasName.create("canonical").getOrNull()!!)
                    val customAlias = ScopeAlias.createCustom(scopeId, AliasName.create("custom").getOrNull()!!)

                    repository.save(canonicalAlias)
                    repository.save(customAlias)

                    // When
                    val result = repository.findByScopeId(scopeId)

                    // Then
                    result.shouldBeRight()
                    val aliases = result.getOrNull()!!
                    aliases.shouldHaveSize(2)
                    aliases.shouldContain(canonicalAlias)
                    aliases.shouldContain(customAlias)
                }
            }

            describe("findCanonicalByScopeId") {
                it("should return null when no canonical alias exists") {
                    // Given
                    val customAlias = ScopeAlias.createCustom(scopeId, AliasName.create("custom").getOrNull()!!)
                    repository.save(customAlias)

                    // When
                    val result = repository.findCanonicalByScopeId(scopeId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull().shouldBeNull()
                }

                it("should return canonical alias when it exists") {
                    // Given
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, AliasName.create("canonical").getOrNull()!!)
                    val customAlias = ScopeAlias.createCustom(scopeId, AliasName.create("custom").getOrNull()!!)

                    repository.save(canonicalAlias)
                    repository.save(customAlias)

                    // When
                    val result = repository.findCanonicalByScopeId(scopeId)

                    // Then
                    result.shouldBeRight()
                    val foundAlias = result.getOrNull()
                    foundAlias.shouldNotBeNull()
                    foundAlias.shouldBeInstanceOf<ScopeAlias>()
                    foundAlias.aliasType shouldBe AliasType.CANONICAL
                }
            }

            describe("findByScopeIdAndType") {
                it("should return aliases of specified type only") {
                    // Given
                    val canonicalAlias = ScopeAlias.createCanonical(scopeId, AliasName.create("canonical").getOrNull()!!)
                    val customAlias1 = ScopeAlias.createCustom(scopeId, AliasName.create("custom1").getOrNull()!!)
                    val customAlias2 = ScopeAlias.createCustom(scopeId, AliasName.create("custom2").getOrNull()!!)

                    repository.save(canonicalAlias)
                    repository.save(customAlias1)
                    repository.save(customAlias2)

                    // When
                    val result = repository.findByScopeIdAndType(scopeId, AliasType.CUSTOM)

                    // Then
                    result.shouldBeRight()
                    val customAliases = result.getOrNull()!!
                    customAliases.shouldHaveSize(2)
                    customAliases.forEach { it.aliasType shouldBe AliasType.CUSTOM }
                }
            }

            describe("findByAliasNamePrefix") {
                it("should find aliases matching prefix") {
                    // Given
                    val alias1 = ScopeAlias.createCustom(scopeId, AliasName.create("prefix-test1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("prefix-test2").getOrNull()!!)
                    val alias3 = ScopeAlias.createCustom(scopeId, AliasName.create("other-alias").getOrNull()!!)

                    repository.save(alias1)
                    repository.save(alias2)
                    repository.save(alias3)

                    // When
                    val result = repository.findByAliasNamePrefix("prefix-", 10)

                    // Then
                    result.shouldBeRight()
                    val foundAliases = result.getOrNull()!!
                    foundAliases.shouldHaveSize(2)
                    foundAliases.forEach {
                        it.aliasName.value.startsWith("prefix-") shouldBe true
                    }
                }

                it("should respect limit parameter") {
                    // Given
                    val alias1 = ScopeAlias.createCustom(scopeId, AliasName.create("test1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("test2").getOrNull()!!)
                    val alias3 = ScopeAlias.createCustom(scopeId, AliasName.create("test3").getOrNull()!!)

                    repository.save(alias1)
                    repository.save(alias2)
                    repository.save(alias3)

                    // When
                    val result = repository.findByAliasNamePrefix("test", 2)

                    // Then
                    result.shouldBeRight()
                    val foundAliases = result.getOrNull()!!
                    foundAliases.shouldHaveSize(2)
                }
            }

            describe("existsByAliasName") {
                it("should return false when alias does not exist") {
                    // Given
                    val nonExistentName = AliasName.create("non-existent").getOrNull()!!

                    // When
                    val result = repository.existsByAliasName(nonExistentName)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe false
                }

                it("should return true when alias exists") {
                    // Given
                    repository.save(alias)

                    // When
                    val result = repository.existsByAliasName(aliasName)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe true
                }
            }

            describe("removeByAliasName") {
                it("should return false when alias does not exist") {
                    // Given
                    val nonExistentName = AliasName.create("non-existent").getOrNull()!!

                    // When
                    val result = repository.removeByAliasName(nonExistentName)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe false
                }

                it("should remove alias and return true when it exists") {
                    // Given
                    repository.save(alias)

                    // When
                    val result = repository.removeByAliasName(aliasName)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe true

                    // Verify removal
                    val findResult = repository.findByAliasName(aliasName)
                    findResult.getOrNull().shouldBeNull()
                }
            }

            describe("removeByScopeId") {
                it("should return 0 when no aliases exist for scope") {
                    // Given
                    val otherScopeId = ScopeId.generate()

                    // When
                    val result = repository.removeByScopeId(otherScopeId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe 0
                }

                it("should remove all aliases for scope and return count") {
                    // Given
                    val alias1 = ScopeAlias.createCanonical(scopeId, AliasName.create("alias1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("alias2").getOrNull()!!)

                    repository.save(alias1)
                    repository.save(alias2)

                    // When
                    val result = repository.removeByScopeId(scopeId)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe 2

                    // Verify removal
                    val findResult = repository.findByScopeId(scopeId)
                    findResult.getOrNull()!!.shouldHaveSize(0)
                }
            }

            describe("update") {
                it("should return false when alias does not exist") {
                    // Given
                    val nonExistentAlias = ScopeAlias.createCustom(scopeId, AliasName.create("new-name").getOrNull()!!)

                    // When
                    val result = repository.update(nonExistentAlias)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe false
                }

                it("should update alias and return true when it exists") {
                    // Given
                    repository.save(alias)
                    val updatedName = AliasName.create("updated-alias").getOrNull()!!
                    val updatedAlias = alias.copy(
                        aliasName = updatedName,
                        updatedAt = Clock.System.now(),
                    )

                    // When
                    val result = repository.update(updatedAlias)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe true

                    // Verify update
                    val findResult = repository.findById(alias.id)
                    val foundAlias = findResult.getOrNull()
                    foundAlias.shouldNotBeNull()
                    foundAlias.aliasName shouldBe updatedName
                }
            }

            describe("count") {
                it("should return 0 when repository is empty") {
                    // When
                    val result = repository.count()

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe 0L
                }

                it("should return correct count after adding aliases") {
                    // Given
                    val alias1 = ScopeAlias.createCanonical(scopeId, AliasName.create("alias1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("alias2").getOrNull()!!)

                    repository.save(alias1)
                    repository.save(alias2)

                    // When
                    val result = repository.count()

                    // Then
                    result.shouldBeRight()
                    result.getOrNull() shouldBe 2L
                }
            }

            describe("listAll") {
                it("should return empty list when repository is empty") {
                    // When
                    val result = repository.listAll(0, 10)

                    // Then
                    result.shouldBeRight()
                    result.getOrNull()!!.shouldHaveSize(0)
                }

                it("should return all aliases with pagination") {
                    // Given
                    val alias1 = ScopeAlias.createCanonical(scopeId, AliasName.create("alias1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("alias2").getOrNull()!!)
                    val alias3 = ScopeAlias.createCustom(scopeId, AliasName.create("alias3").getOrNull()!!)

                    repository.save(alias1)
                    repository.save(alias2)
                    repository.save(alias3)

                    // When
                    val result = repository.listAll(0, 2)

                    // Then
                    result.shouldBeRight()
                    val aliases = result.getOrNull()!!
                    aliases.shouldHaveSize(2)
                }

                it("should handle offset correctly") {
                    // Given
                    val alias1 = ScopeAlias.createCanonical(scopeId, AliasName.create("alias1").getOrNull()!!)
                    val alias2 = ScopeAlias.createCustom(scopeId, AliasName.create("alias2").getOrNull()!!)
                    val alias3 = ScopeAlias.createCustom(scopeId, AliasName.create("alias3").getOrNull()!!)

                    repository.save(alias1)
                    repository.save(alias2)
                    repository.save(alias3)

                    // When
                    val result = repository.listAll(1, 2)

                    // Then
                    result.shouldBeRight()
                    val aliases = result.getOrNull()!!
                    aliases.shouldHaveSize(2)
                }
            }
        }
    })
