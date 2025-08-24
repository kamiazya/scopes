package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.HierarchyPolicyError
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

class HierarchyPolicyPropertyTest :
    StringSpec({

        "creating policy with valid positive values should succeed" {
            checkAll(validPositiveIntArb(), validPositiveIntArb()) { maxDepth, maxChildren ->
                val result = HierarchyPolicy.create(maxDepth, maxChildren)

                result.isRight() shouldBe true
                result.fold(
                    { throw AssertionError("Expected Right but got Left: $it") },
                    { policy ->
                        policy.maxDepth shouldBe maxDepth
                        policy.maxChildrenPerScope shouldBe maxChildren
                    },
                )
            }
        }

        "creating policy with null values should succeed and represent unlimited" {
            val result = HierarchyPolicy.create(null, null)

            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left: $it") },
                { policy ->
                    policy.maxDepth shouldBe null
                    policy.maxChildrenPerScope shouldBe null
                    policy.isDepthUnlimited() shouldBe true
                    policy.isChildrenPerScopeUnlimited() shouldBe true
                },
            )
        }

        "creating policy with mixed null and valid values should succeed" {
            checkAll(validPositiveIntArb().orNull(), validPositiveIntArb().orNull()) { maxDepth, maxChildren ->
                val result = HierarchyPolicy.create(maxDepth, maxChildren)

                result.isRight() shouldBe true
                result.fold(
                    { throw AssertionError("Expected Right but got Left: $it") },
                    { policy ->
                        policy.maxDepth shouldBe maxDepth
                        policy.maxChildrenPerScope shouldBe maxChildren
                        policy.isDepthUnlimited() shouldBe (maxDepth == null)
                        policy.isChildrenPerScopeUnlimited() shouldBe (maxChildren == null)
                    },
                )
            }
        }

        "creating policy with zero or negative maxDepth should fail with InvalidMaxDepth error" {
            checkAll(invalidIntArb()) { invalidDepth ->
                val result = HierarchyPolicy.create(invalidDepth, null)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxDepth>()
                        error.attemptedValue shouldBe invalidDepth
                        error.minimumAllowed shouldBe 1
                    },
                    { throw AssertionError("Expected Left but got Right: $it") },
                )
            }
        }

        "creating policy with zero or negative maxChildrenPerScope should fail with InvalidMaxChildrenPerScope error" {
            checkAll(invalidIntArb()) { invalidChildren ->
                val result = HierarchyPolicy.create(null, invalidChildren)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxChildrenPerScope>()
                        error.attemptedValue shouldBe invalidChildren
                        error.minimumAllowed shouldBe 1
                    },
                    { throw AssertionError("Expected Left but got Right: $it") },
                )
            }
        }

        "creating policy with both values invalid should fail with first error encountered" {
            checkAll(invalidIntArb(), invalidIntArb()) { invalidDepth, invalidChildren ->
                val result = HierarchyPolicy.create(invalidDepth, invalidChildren)

                result.isLeft() shouldBe true
                result.fold(
                    { error ->
                        // Should fail on maxDepth first due to ensure order in implementation
                        error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxDepth>()
                        error.attemptedValue shouldBe invalidDepth
                    },
                    { throw AssertionError("Expected Left but got Right: $it") },
                )
            }
        }

        "default() should create unlimited policy" {
            val defaultPolicy = HierarchyPolicy.default()

            defaultPolicy.maxDepth shouldBe null
            defaultPolicy.maxChildrenPerScope shouldBe null
            defaultPolicy.isDepthUnlimited() shouldBe true
            defaultPolicy.isChildrenPerScopeUnlimited() shouldBe true
        }

        "isDepthUnlimited should return correct values" {
            checkAll(validPositiveIntArb()) { depth ->
                val limitedPolicy = HierarchyPolicy.create(depth, null).getOrNull()!!
                val unlimitedPolicy = HierarchyPolicy.create(null, null).getOrNull()!!

                limitedPolicy.isDepthUnlimited() shouldBe false
                unlimitedPolicy.isDepthUnlimited() shouldBe true
            }
        }

        "isChildrenPerScopeUnlimited should return correct values" {
            checkAll(validPositiveIntArb()) { children ->
                val limitedPolicy = HierarchyPolicy.create(null, children).getOrNull()!!
                val unlimitedPolicy = HierarchyPolicy.create(null, null).getOrNull()!!

                limitedPolicy.isChildrenPerScopeUnlimited() shouldBe false
                unlimitedPolicy.isChildrenPerScopeUnlimited() shouldBe true
            }
        }

        "creating with same values should produce equal objects" {
            checkAll(validPositiveIntArb().orNull(), validPositiveIntArb().orNull()) { maxDepth, maxChildren ->
                val policy1 = HierarchyPolicy.create(maxDepth, maxChildren).getOrNull()!!
                val policy2 = HierarchyPolicy.create(maxDepth, maxChildren).getOrNull()!!

                policy1 shouldBe policy2
                policy1.hashCode() shouldBe policy2.hashCode()
            }
        }

        "creating with different values should produce different objects" {
            val policy1 = HierarchyPolicy.create(5, 10).getOrNull()!!
            val policy2 = HierarchyPolicy.create(10, 5).getOrNull()!!
            val policy3 = HierarchyPolicy.create(null, 10).getOrNull()!!

            policy1 shouldNotBe policy2
            policy1 shouldNotBe policy3
            policy2 shouldNotBe policy3
        }

        "toString should contain relevant information" {
            checkAll(validPositiveIntArb().orNull(), validPositiveIntArb().orNull()) { maxDepth, maxChildren ->
                val policy = HierarchyPolicy.create(maxDepth, maxChildren).getOrNull()!!
                val stringRep = policy.toString()

                stringRep.contains("HierarchyPolicy") shouldBe true
                stringRep.contains("maxDepth") shouldBe true
                stringRep.contains("maxChildrenPerScope") shouldBe true
            }
        }

        "error types should contain timestamp and proper error details" {
            val invalidDepthResult = HierarchyPolicy.create(-1, null)
            val invalidChildrenResult = HierarchyPolicy.create(null, -5)

            invalidDepthResult.fold(
                { error ->
                    error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxDepth>()
                    error.occurredAt shouldNotBe null
                    error.attemptedValue shouldBe -1
                },
                { throw AssertionError("Expected Left but got Right") },
            )

            invalidChildrenResult.fold(
                { error ->
                    error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxChildrenPerScope>()
                    error.occurredAt shouldNotBe null
                    error.attemptedValue shouldBe -5
                },
                { throw AssertionError("Expected Left but got Right") },
            )
        }

        "edge case: maximum integer values should be valid" {
            val result = HierarchyPolicy.create(Int.MAX_VALUE, Int.MAX_VALUE)

            result.isRight() shouldBe true
            result.fold(
                { throw AssertionError("Expected Right but got Left: $it") },
                { policy ->
                    policy.maxDepth shouldBe Int.MAX_VALUE
                    policy.maxChildrenPerScope shouldBe Int.MAX_VALUE
                    policy.isDepthUnlimited() shouldBe false
                    policy.isChildrenPerScopeUnlimited() shouldBe false
                },
            )
        }
    })

// Test data generators following the project patterns
private fun validPositiveIntArb(): Arb<Int> = Arb.int(1, 10000)

private fun invalidIntArb(): Arb<Int> = Arb.choice(
    Arb.int(Int.MIN_VALUE, 0), // Zero and negative values
)

// Additional generator for comprehensive testing
private fun hierarchyPolicyArb(): Arb<Pair<Int?, Int?>> = arbitrary {
    val maxDepth = validPositiveIntArb().orNull().bind()
    val maxChildren = validPositiveIntArb().orNull().bind()
    maxDepth to maxChildren
}
