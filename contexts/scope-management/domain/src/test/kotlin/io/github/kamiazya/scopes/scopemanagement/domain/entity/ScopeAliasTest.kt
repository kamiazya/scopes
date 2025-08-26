package io.github.kamiazya.scopes.scopemanagement.domain.entity

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

class ScopeAliasTest :
    DescribeSpec({
        describe("ScopeAlias") {
            val scopeId = ScopeId.generate()
            val aliasName = AliasName.create("test-alias").getOrNull()!!

            describe("createCanonical") {
                it("should create a canonical alias with generated ID and current timestamp") {
                    val before = Clock.System.now()
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                    val after = Clock.System.now()

                    alias.scopeId shouldBe scopeId
                    alias.aliasName shouldBe aliasName
                    alias.aliasType shouldBe AliasType.CANONICAL
                    alias.id.shouldBeInstanceOf<AliasId>()

                    // Verify timestamps are within expected range
                    val createdAt = alias.createdAt
                    val updatedAt = alias.updatedAt

                    (createdAt >= before) shouldBe true
                    (createdAt <= after) shouldBe true
                    createdAt shouldBe updatedAt
                }

                it("should generate unique IDs for different aliases") {
                    val alias1 = ScopeAlias.createCanonical(scopeId, aliasName)
                    val alias2 = ScopeAlias.createCanonical(scopeId, aliasName)

                    alias1.id shouldNotBe alias2.id
                }
            }

            describe("createCustom") {
                it("should create a custom alias with generated ID and current timestamp") {
                    val before = Clock.System.now()
                    val alias = ScopeAlias.createCustom(scopeId, aliasName)
                    val after = Clock.System.now()

                    alias.scopeId shouldBe scopeId
                    alias.aliasName shouldBe aliasName
                    alias.aliasType shouldBe AliasType.CUSTOM
                    alias.id.shouldBeInstanceOf<AliasId>()

                    // Verify timestamps are within expected range
                    val createdAt = alias.createdAt
                    val updatedAt = alias.updatedAt

                    (createdAt >= before) shouldBe true
                    (createdAt <= after) shouldBe true
                    createdAt shouldBe updatedAt
                }

                it("should generate unique IDs for different aliases") {
                    val alias1 = ScopeAlias.createCustom(scopeId, aliasName)
                    val alias2 = ScopeAlias.createCustom(scopeId, aliasName)

                    alias1.id shouldNotBe alias2.id
                }
            }

            describe("copy with new alias name") {
                it("should update alias name and updatedAt timestamp") {
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                    val originalUpdatedAt = alias.updatedAt

                    Thread.sleep(10) // Ensure different timestamp

                    val newAliasName = AliasName.create("new-alias-name").getOrNull()!!
                    val updatedAlias = alias.copy(aliasName = newAliasName).withUpdatedTimestamp()

                    updatedAlias.id shouldBe alias.id
                    updatedAlias.scopeId shouldBe alias.scopeId
                    updatedAlias.aliasName shouldBe newAliasName
                    updatedAlias.aliasType shouldBe alias.aliasType
                    updatedAlias.createdAt shouldBe alias.createdAt
                    (updatedAlias.updatedAt > originalUpdatedAt) shouldBe true
                }

                it("should not modify original alias (immutability)") {
                    val originalName = aliasName
                    val alias = ScopeAlias.createCanonical(scopeId, originalName)
                    val newAliasName = AliasName.create("new-alias-name").getOrNull()!!

                    alias.copy(aliasName = newAliasName)

                    alias.aliasName shouldBe originalName
                }
            }

            describe("copy with new alias type") {
                it("should update alias type from CANONICAL to CUSTOM") {
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                    val originalUpdatedAt = alias.updatedAt

                    Thread.sleep(10) // Ensure different timestamp

                    val updatedAlias = alias.copy(aliasType = AliasType.CUSTOM).withUpdatedTimestamp()

                    updatedAlias.id shouldBe alias.id
                    updatedAlias.scopeId shouldBe alias.scopeId
                    updatedAlias.aliasName shouldBe alias.aliasName
                    updatedAlias.aliasType shouldBe AliasType.CUSTOM
                    updatedAlias.createdAt shouldBe alias.createdAt
                    (updatedAlias.updatedAt > originalUpdatedAt) shouldBe true
                }

                it("should update alias type from CUSTOM to CANONICAL") {
                    val alias = ScopeAlias.createCustom(scopeId, aliasName)
                    val originalUpdatedAt = alias.updatedAt

                    Thread.sleep(10) // Ensure different timestamp

                    val updatedAlias = alias.copy(aliasType = AliasType.CANONICAL).withUpdatedTimestamp()

                    updatedAlias.id shouldBe alias.id
                    updatedAlias.scopeId shouldBe alias.scopeId
                    updatedAlias.aliasName shouldBe alias.aliasName
                    updatedAlias.aliasType shouldBe AliasType.CANONICAL
                    updatedAlias.createdAt shouldBe alias.createdAt
                    (updatedAlias.updatedAt > originalUpdatedAt) shouldBe true
                }

                it("should not modify original alias (immutability)") {
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)

                    alias.copy(aliasType = AliasType.CUSTOM)

                    alias.aliasType shouldBe AliasType.CANONICAL
                }
            }

            describe("data class functionality") {
                it("should correctly implement equals") {
                    val id = AliasId.generate()
                    val now = Clock.System.now()

                    val alias1 = ScopeAlias(
                        id = id,
                        scopeId = scopeId,
                        aliasName = aliasName,
                        aliasType = AliasType.CANONICAL,
                        createdAt = now,
                        updatedAt = now,
                    )

                    val alias2 = ScopeAlias(
                        id = id,
                        scopeId = scopeId,
                        aliasName = aliasName,
                        aliasType = AliasType.CANONICAL,
                        createdAt = now,
                        updatedAt = now,
                    )

                    (alias1 == alias2) shouldBe true
                }

                it("should correctly implement hashCode") {
                    val id = AliasId.generate()
                    val now = Clock.System.now()

                    val alias1 = ScopeAlias(
                        id = id,
                        scopeId = scopeId,
                        aliasName = aliasName,
                        aliasType = AliasType.CANONICAL,
                        createdAt = now,
                        updatedAt = now,
                    )

                    val alias2 = ScopeAlias(
                        id = id,
                        scopeId = scopeId,
                        aliasName = aliasName,
                        aliasType = AliasType.CANONICAL,
                        createdAt = now,
                        updatedAt = now,
                    )

                    alias1.hashCode() shouldBe alias2.hashCode()
                }

                it("should have different hash codes for different aliases") {
                    val alias1 = ScopeAlias.createCanonical(scopeId, aliasName)
                    val alias2 = ScopeAlias.createCanonical(scopeId, aliasName)

                    alias1.hashCode() shouldNotBe alias2.hashCode()
                }

                it("should correctly implement copy") {
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                    val newName = AliasName.create("copied-alias").getOrNull()!!

                    val copied = alias.copy(aliasName = newName)

                    copied.id shouldBe alias.id
                    copied.scopeId shouldBe alias.scopeId
                    copied.aliasName shouldBe newName
                    copied.aliasType shouldBe alias.aliasType
                    copied.createdAt shouldBe alias.createdAt
                    copied.updatedAt shouldBe alias.updatedAt
                }
            }

            describe("business rules") {
                it("should maintain consistency between scope and alias") {
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)

                    // The alias should always be associated with the same scope
                    alias.scopeId shouldBe scopeId

                    // Even after changes, the scope association should remain
                    val updatedAlias = alias.copy(
                        aliasName = AliasName.create("updated-name").getOrNull()!!,
                    )
                    updatedAlias.scopeId shouldBe scopeId
                }

                it("should preserve creation timestamp through updates") {
                    val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                    val originalCreatedAt = alias.createdAt

                    Thread.sleep(10)

                    val updated1 = alias.copy(
                        aliasName = AliasName.create("updated-name").getOrNull()!!,
                    )
                    val updated2 = updated1.copy(aliasType = AliasType.CUSTOM)

                    updated1.createdAt shouldBe originalCreatedAt
                    updated2.createdAt shouldBe originalCreatedAt
                }
            }
        }
    })
