package io.github.kamiazya.scopes.userpreferences.domain.value

import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll

class HierarchyPreferencesPropertyTest :
    StringSpec({

        "creating preferences with valid positive values should succeed" {
            checkAll(validPositiveIntArb(), validPositiveIntArb()) { maxDepth, maxChildren ->
                val result = HierarchyPreferences.create(maxDepth, maxChildren)

                val preferences = result.shouldBeRight()
                preferences.maxDepth shouldBe maxDepth
                preferences.maxChildrenPerScope shouldBe maxChildren
            }
        }

        "creating preferences with null values should succeed and use defaults" {
            val result = HierarchyPreferences.create(null, null)

            val preferences = result.shouldBeRight()
            preferences.maxDepth shouldBe null
            preferences.maxChildrenPerScope shouldBe null
        }

        "creating preferences with mixed null and valid values should succeed" {
            checkAll(validPositiveIntArb().orNull(), validPositiveIntArb().orNull()) { maxDepth, maxChildren ->
                val result = HierarchyPreferences.create(maxDepth, maxChildren)

                val preferences = result.shouldBeRight()
                preferences.maxDepth shouldBe maxDepth
                preferences.maxChildrenPerScope shouldBe maxChildren
            }
        }

        "creating preferences with zero or negative maxDepth should fail" {
            checkAll(invalidIntArb()) { invalidDepth ->
                val result = HierarchyPreferences.create(invalidDepth, null)

                val error = result.shouldBeLeft()
                error.shouldBeInstanceOf<UserPreferencesError.InvalidHierarchyPreferences>()
                error.reason shouldBe "Maximum depth must be positive if specified"
            }
        }

        "creating preferences with zero or negative maxChildrenPerScope should fail" {
            checkAll(invalidIntArb()) { invalidChildren ->
                val result = HierarchyPreferences.create(null, invalidChildren)

                val error = result.shouldBeLeft()
                error.shouldBeInstanceOf<UserPreferencesError.InvalidHierarchyPreferences>()
                error.reason shouldBe "Maximum children per scope must be positive if specified"
            }
        }

        "creating preferences with both values invalid should fail with first error encountered" {
            checkAll(invalidIntArb(), invalidIntArb()) { invalidDepth, invalidChildren ->
                val result = HierarchyPreferences.create(invalidDepth, invalidChildren)

                val error = result.shouldBeLeft()
                error.shouldBeInstanceOf<UserPreferencesError.InvalidHierarchyPreferences>()
                // Should fail on maxDepth first (as per implementation order)
                error.reason shouldBe "Maximum depth must be positive if specified"
            }
        }

        "DEFAULT should have null values for unlimited preferences" {
            val defaultPreferences = HierarchyPreferences.DEFAULT

            defaultPreferences.maxDepth shouldBe null
            defaultPreferences.maxChildrenPerScope shouldBe null
        }

        "creating with same values should produce equal objects" {
            checkAll(validPositiveIntArb().orNull(), validPositiveIntArb().orNull()) { maxDepth, maxChildren ->
                val preferences1 = HierarchyPreferences.create(maxDepth, maxChildren).getOrNull()!!
                val preferences2 = HierarchyPreferences.create(maxDepth, maxChildren).getOrNull()!!

                preferences1 shouldBe preferences2
                preferences1.hashCode() shouldBe preferences2.hashCode()
            }
        }

        "creating with different values should produce different objects" {
            val preferences1 = HierarchyPreferences.create(5, 10).getOrNull()!!
            val preferences2 = HierarchyPreferences.create(10, 5).getOrNull()!!

            preferences1 shouldNotBe preferences2
        }

        "creating new instances with create method should work correctly" {
            checkAll(validPositiveIntArb().orNull(), validPositiveIntArb().orNull()) { maxDepth, maxChildren ->
                val preferences1 = HierarchyPreferences.create(maxDepth, maxChildren).getOrNull()!!
                val preferences2 = HierarchyPreferences.create(maxDepth, maxChildren).getOrNull()!!

                // Same parameters should create equal objects
                preferences1 shouldBe preferences2

                // Different parameters should create different objects
                if (maxDepth != null && maxDepth < 1000) {
                    val differentDepth = HierarchyPreferences.create(maxDepth + 1, maxChildren).getOrNull()!!
                    differentDepth shouldNotBe preferences1
                }
            }
        }

        "toString should contain relevant information" {
            checkAll(validPositiveIntArb().orNull(), validPositiveIntArb().orNull()) { maxDepth, maxChildren ->
                val preferences = HierarchyPreferences.create(maxDepth, maxChildren).getOrNull()!!
                val stringRep = preferences.toString()

                stringRep.contains("HierarchyPreferences") shouldBe true
                stringRep.contains("maxDepth") shouldBe true
                stringRep.contains("maxChildrenPerScope") shouldBe true
            }
        }
    })

// Test data generators following the patterns from ScopeIdPropertyTest
private fun validPositiveIntArb(): Arb<Int> = Arb.int(1, 10000)

private fun invalidIntArb(): Arb<Int> = Arb.choice(
    Arb.int(Int.MIN_VALUE, 0), // Zero and negative values
)

// Additional generator for comprehensive testing
private fun hierarchyPreferencesArb(): Arb<Pair<Int?, Int?>> = arbitrary {
    val maxDepth = validPositiveIntArb().orNull().bind()
    val maxChildren = validPositiveIntArb().orNull().bind()
    maxDepth to maxChildren
}
