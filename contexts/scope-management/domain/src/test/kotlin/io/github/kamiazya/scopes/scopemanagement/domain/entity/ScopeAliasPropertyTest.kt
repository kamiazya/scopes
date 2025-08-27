package io.github.kamiazya.scopes.scopemanagement.domain.entity

import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ScopeAliasPropertyTest :
    StringSpec({

        "canonical aliases should always have CANONICAL type" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val alias = ScopeAlias.createCanonical(scopeId, aliasName)

                alias.aliasType shouldBe AliasType.CANONICAL
                alias.scopeId shouldBe scopeId
                alias.aliasName shouldBe aliasName
                alias.id.shouldBeInstanceOf<AliasId>()
                alias.createdAt.shouldBeInstanceOf<Instant>()
                alias.updatedAt shouldBe alias.createdAt
            }
        }

        "custom aliases should always have CUSTOM type" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val alias = ScopeAlias.createCustom(scopeId, aliasName)

                alias.aliasType shouldBe AliasType.CUSTOM
                alias.scopeId shouldBe scopeId
                alias.aliasName shouldBe aliasName
                alias.id.shouldBeInstanceOf<AliasId>()
                alias.createdAt.shouldBeInstanceOf<Instant>()
                alias.updatedAt shouldBe alias.createdAt
            }
        }

        "createCanonical should generate unique IDs" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val alias1 = ScopeAlias.createCanonical(scopeId, aliasName)
                val alias2 = ScopeAlias.createCanonical(scopeId, aliasName)

                alias1.id shouldNotBe alias2.id
                alias1.aliasName shouldBe alias2.aliasName
                alias1.scopeId shouldBe alias2.scopeId
                alias1.aliasType shouldBe alias2.aliasType
            }
        }

        "createCustom should generate unique IDs" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val alias1 = ScopeAlias.createCustom(scopeId, aliasName)
                val alias2 = ScopeAlias.createCustom(scopeId, aliasName)

                alias1.id shouldNotBe alias2.id
                alias1.aliasName shouldBe alias2.aliasName
                alias1.scopeId shouldBe alias2.scopeId
                alias1.aliasType shouldBe alias2.aliasType
            }
        }

        "aliases should have timestamps close to creation time" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val beforeCreation = Clock.System.now()
                val alias = ScopeAlias.createCanonical(scopeId, aliasName)
                val afterCreation = Clock.System.now()

                alias.createdAt shouldBe { it >= beforeCreation && it <= afterCreation }
                alias.updatedAt shouldBe alias.createdAt
            }
        }

        "copy should preserve immutability principles" {
            checkAll(scopeIdArb(), validAliasNameArb(), validAliasNameArb()) { scopeId, originalName, newName ->
                val original = ScopeAlias.createCustom(scopeId, originalName)
                val updated = original.copy(aliasName = newName, updatedAt = Clock.System.now())

                // Original should be unchanged
                original.aliasName shouldBe originalName
                original.scopeId shouldBe scopeId

                // Updated should have new values
                updated.aliasName shouldBe newName
                updated.scopeId shouldBe scopeId // Should be preserved
                updated.id shouldBe original.id // Should be preserved
                updated.aliasType shouldBe original.aliasType // Should be preserved
                updated.createdAt shouldBe original.createdAt // Should be preserved
                updated.updatedAt shouldBe { it >= original.updatedAt }
            }
        }

        "equals should work correctly based on ID" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val alias1 = ScopeAlias.createCanonical(scopeId, aliasName)
                val alias2 = alias1.copy() // Same ID, different instance
                val alias3 = ScopeAlias.createCanonical(scopeId, aliasName) // Different ID

                alias1 shouldBe alias2 // Same ID
                alias1 shouldNotBe alias3 // Different ID
                alias1.hashCode() shouldBe alias2.hashCode()
            }
        }

        "aliases with same properties but different IDs should not be equal" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val alias1 = ScopeAlias.createCanonical(scopeId, aliasName)
                val alias2 = ScopeAlias.createCanonical(scopeId, aliasName)

                alias1 shouldNotBe alias2 // Different IDs
                alias1.aliasName shouldBe alias2.aliasName
                alias1.scopeId shouldBe alias2.scopeId
                alias1.aliasType shouldBe alias2.aliasType
            }
        }

        "canonical and custom aliases with same properties should have different types" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val canonical = ScopeAlias.createCanonical(scopeId, aliasName)
                val custom = ScopeAlias.createCustom(scopeId, aliasName)

                canonical.aliasType shouldBe AliasType.CANONICAL
                custom.aliasType shouldBe AliasType.CUSTOM
                canonical shouldNotBe custom // Different types and IDs
                canonical.aliasName shouldBe custom.aliasName
                canonical.scopeId shouldBe custom.scopeId
            }
        }

        "alias creation should handle edge case names" {
            checkAll(scopeIdArb(), edgeAliasNameArb()) { scopeId, aliasName ->
                val canonical = ScopeAlias.createCanonical(scopeId, aliasName)
                val custom = ScopeAlias.createCustom(scopeId, aliasName)

                canonical.aliasName shouldBe aliasName
                custom.aliasName shouldBe aliasName
                canonical.aliasType shouldBe AliasType.CANONICAL
                custom.aliasType shouldBe AliasType.CUSTOM
            }
        }

        "alias should maintain invariants after updates" {
            checkAll(scopeIdArb(), validAliasNameArb(), validAliasNameArb()) { scopeId, originalName, newName ->
                val alias = ScopeAlias.createCustom(scopeId, originalName)
                val updateTime = Clock.System.now()
                val updated = alias.copy(aliasName = newName, updatedAt = updateTime)

                // Invariants should be maintained
                updated.id shouldBe alias.id // Identity preserved
                updated.scopeId shouldBe scopeId // Scope relationship preserved
                updated.aliasType shouldBe AliasType.CUSTOM // Type preserved
                updated.createdAt shouldBe alias.createdAt // Creation time preserved
                updated.updatedAt shouldBe updateTime // Update time changed
                updated.aliasName shouldBe newName // Content updated
            }
        }

        "concurrent alias creation should produce unique IDs" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val aliases = (1..100).map {
                    ScopeAlias.createCanonical(scopeId, aliasName)
                }

                val uniqueIds = aliases.map { it.id }.distinct()
                uniqueIds.size shouldBe aliases.size // All IDs should be unique

                // All should have same properties except ID and timestamps
                aliases.forEach { alias ->
                    alias.scopeId shouldBe scopeId
                    alias.aliasName shouldBe aliasName
                    alias.aliasType shouldBe AliasType.CANONICAL
                }
            }
        }

        "alias timestamps should be monotonic within reasonable bounds" {
            checkAll(scopeIdArb(), validAliasNameArb()) { scopeId, aliasName ->
                val aliases = mutableListOf<ScopeAlias>()

                repeat(5) {
                    aliases.add(ScopeAlias.createCustom(scopeId, aliasName))
                    Thread.sleep(1) // Small delay
                }

                // Check that timestamps are generally increasing
                for (i in 0 until aliases.size - 1) {
                    val current = aliases[i]
                    val next = aliases[i + 1]

                    // Should be equal or increasing (within reasonable tolerance)
                    next.createdAt shouldBe { it >= current.createdAt }
                }
            }
        }
    })

// Test helpers
private fun scopeIdArb(): Arb<ScopeId> = arbitrary {
    ScopeId.generate()
}

private fun validAliasNameArb(): Arb<AliasName> = arbitrary {
    val validNames = listOf(
        "test-alias",
        "my-scope",
        "project-alpha",
        "service_v1",
        "api-gateway",
        "user-auth",
        "data-store",
        "cache-layer",
    )
    AliasName.create(validNames.random()).getOrNull()!!
}

private fun edgeAliasNameArb(): Arb<AliasName> = arbitrary {
    val edgeNames = listOf(
        "aa", // Minimum length (2 chars)
        "a" + "b".repeat(63), // Maximum length (64 chars)
        "a-b-c-d-e-f-g", // Many hyphens
        "a_b_c_d_e_f_g", // Many underscores
        "a1b2c3d4e5f6", // Many numbers
        "alpha-beta-gamma-delta", // Longer realistic name
    )
    AliasName.create(edgeNames.random()).getOrNull()!!
}
