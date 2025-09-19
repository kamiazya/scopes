package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.error.HierarchyPolicyError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for HierarchyPolicy value object.
 *
 * Business rules:
 * - HierarchyPolicy encapsulates business rules for scope hierarchies
 * - null values represent unlimited constraints
 * - Positive values represent specific limits
 * - Zero and negative values are invalid for limits
 * - Policy supports validation, merging, and comparison operations
 */
class HierarchyPolicyTest :
    StringSpec({

        // Factory method tests
        "should create default policy with unlimited constraints" {
            val policy = HierarchyPolicy.default()

            policy.maxDepth shouldBe null
            policy.maxChildrenPerScope shouldBe null
            policy.isDepthUnlimited() shouldBe true
            policy.isChildrenPerScopeUnlimited() shouldBe true
            policy.hasRestrictions() shouldBe false
        }

        "should create policy with valid positive depth" {
            val result = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null)

            val policy = result.shouldBeRight()
            policy.maxDepth shouldBe 10
            policy.maxChildrenPerScope shouldBe null
            policy.isDepthUnlimited() shouldBe false
            policy.isChildrenPerScopeUnlimited() shouldBe true
            policy.hasRestrictions() shouldBe true
        }

        "should create policy with valid positive children limit" {
            val result = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 50)

            val policy = result.shouldBeRight()
            policy.maxDepth shouldBe null
            policy.maxChildrenPerScope shouldBe 50
            policy.isDepthUnlimited() shouldBe true
            policy.isChildrenPerScopeUnlimited() shouldBe false
            policy.hasRestrictions() shouldBe true
        }

        "should create policy with both limits specified" {
            val result = HierarchyPolicy.create(maxDepth = 15, maxChildrenPerScope = 25)

            val policy = result.shouldBeRight()
            policy.maxDepth shouldBe 15
            policy.maxChildrenPerScope shouldBe 25
            policy.isDepthUnlimited() shouldBe false
            policy.isChildrenPerScopeUnlimited() shouldBe false
            policy.hasRestrictions() shouldBe true
        }

        "should reject zero depth limit" {
            val result = HierarchyPolicy.create(maxDepth = 0, maxChildrenPerScope = null)

            val error = result.shouldBeLeft()
            val typedError = error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxDepth>()
            typedError.attemptedValue shouldBe 0
        }

        "should reject negative depth limit" {
            val result = HierarchyPolicy.create(maxDepth = -5, maxChildrenPerScope = null)

            val error = result.shouldBeLeft()
            val typedError = error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxDepth>()
            typedError.attemptedValue shouldBe -5
        }

        "should reject zero children limit" {
            val result = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 0)

            val error = result.shouldBeLeft()
            val typedError = error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxChildrenPerScope>()
            typedError.attemptedValue shouldBe 0
        }

        "should reject negative children limit" {
            val result = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = -10)

            val error = result.shouldBeLeft()
            val typedError = error.shouldBeInstanceOf<HierarchyPolicyError.InvalidMaxChildrenPerScope>()
            typedError.attemptedValue shouldBe -10
        }

        // Validation tests
        "should validate depth within limit" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!

            policy.isDepthAllowed(5) shouldBe true
            policy.isDepthAllowed(10) shouldBe true
            policy.validateDepth(5).shouldBeRight()
            policy.validateDepth(10).shouldBeRight()
        }

        "should validate depth exceeding limit" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!

            policy.isDepthAllowed(11) shouldBe false
            policy.isDepthAllowed(15) shouldBe false

            val result = policy.validateDepth(11)
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<HierarchyPolicyError.DepthExceeded>()
            error.currentDepth shouldBe 11
            error.maxAllowed shouldBe 10
        }

        "should validate unlimited depth allows any value" {
            val policy = HierarchyPolicy.default()

            policy.isDepthAllowed(1000) shouldBe true
            policy.isDepthAllowed(Int.MAX_VALUE) shouldBe true
            policy.validateDepth(1000).shouldBeRight()
            policy.validateDepth(Int.MAX_VALUE).shouldBeRight()
        }

        "should validate children count within limit" {
            val policy = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 50).getOrNull()!!

            policy.isChildrenCountAllowed(25) shouldBe true
            policy.isChildrenCountAllowed(50) shouldBe true
            policy.validateChildrenCount(25).shouldBeRight()
            policy.validateChildrenCount(50).shouldBeRight()
        }

        "should validate children count exceeding limit" {
            val policy = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 50).getOrNull()!!

            policy.isChildrenCountAllowed(51) shouldBe false
            policy.isChildrenCountAllowed(100) shouldBe false

            val result = policy.validateChildrenCount(51)
            val error = result.shouldBeLeft()
            error.shouldBeInstanceOf<HierarchyPolicyError.TooManyChildren>()
            error.currentCount shouldBe 51
            error.maxAllowed shouldBe 50
        }

        "should validate unlimited children allows any value" {
            val policy = HierarchyPolicy.default()

            policy.isChildrenCountAllowed(1000) shouldBe true
            policy.isChildrenCountAllowed(Int.MAX_VALUE) shouldBe true
            policy.validateChildrenCount(1000).shouldBeRight()
            policy.validateChildrenCount(Int.MAX_VALUE).shouldBeRight()
        }

        // Effective limits tests
        "should return effective limits for restricted policy" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 25).getOrNull()!!

            policy.effectiveMaxDepth() shouldBe 10
            policy.effectiveMaxChildrenPerScope() shouldBe 25
        }

        "should return effective limits for unlimited policy" {
            val policy = HierarchyPolicy.default()

            policy.effectiveMaxDepth() shouldBe Int.MAX_VALUE
            policy.effectiveMaxChildrenPerScope() shouldBe Int.MAX_VALUE
        }

        "should return effective limits for mixed policy" {
            val policy = HierarchyPolicy.create(maxDepth = 15, maxChildrenPerScope = null).getOrNull()!!

            policy.effectiveMaxDepth() shouldBe 15
            policy.effectiveMaxChildrenPerScope() shouldBe Int.MAX_VALUE
        }

        // Remaining capacity tests
        "should calculate remaining depth correctly" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!

            policy.remainingDepth(5) shouldBe 5
            policy.remainingDepth(10) shouldBe 0
            policy.remainingDepth(8) shouldBe 2
        }

        "should calculate remaining depth for unlimited policy" {
            val policy = HierarchyPolicy.default()

            policy.remainingDepth(100) shouldBe null
            policy.remainingDepth(0) shouldBe null
        }

        "should handle negative remaining depth" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!

            policy.remainingDepth(15) shouldBe 0 // maxOf(0, 10 - 15) = 0
        }

        "should calculate remaining children slots correctly" {
            val policy = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 20).getOrNull()!!

            policy.remainingChildrenSlots(5) shouldBe 15
            policy.remainingChildrenSlots(20) shouldBe 0
            policy.remainingChildrenSlots(12) shouldBe 8
        }

        "should calculate remaining children slots for unlimited policy" {
            val policy = HierarchyPolicy.default()

            policy.remainingChildrenSlots(100) shouldBe null
            policy.remainingChildrenSlots(0) shouldBe null
        }

        "should handle negative remaining children slots" {
            val policy = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 20).getOrNull()!!

            policy.remainingChildrenSlots(25) shouldBe 0 // maxOf(0, 20 - 25) = 0
        }

        // Comparison tests
        "should compare restrictiveness between policies with depth limits" {
            val moreRestrictive = HierarchyPolicy.create(maxDepth = 5, maxChildrenPerScope = null).getOrNull()!!
            val lessRestrictive = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!

            moreRestrictive.isMoreRestrictiveThan(lessRestrictive) shouldBe true
            lessRestrictive.isMoreRestrictiveThan(moreRestrictive) shouldBe false
        }

        "should compare restrictiveness between policies with children limits" {
            val moreRestrictive = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 10).getOrNull()!!
            val lessRestrictive = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 20).getOrNull()!!

            moreRestrictive.isMoreRestrictiveThan(lessRestrictive) shouldBe true
            lessRestrictive.isMoreRestrictiveThan(moreRestrictive) shouldBe false
        }

        "should compare unlimited policy as less restrictive" {
            val unlimited = HierarchyPolicy.default()
            val limited = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!

            unlimited.isMoreRestrictiveThan(limited) shouldBe false
            limited.isMoreRestrictiveThan(unlimited) shouldBe true
        }

        "should handle equal policies as not more restrictive" {
            val policy1 = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!
            val policy2 = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!

            policy1.isMoreRestrictiveThan(policy2) shouldBe false
            policy2.isMoreRestrictiveThan(policy1) shouldBe false
        }

        // Merging tests
        "should merge policies taking more restrictive depth" {
            val policy1 = HierarchyPolicy.create(maxDepth = 5, maxChildrenPerScope = null).getOrNull()!!
            val policy2 = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!

            val merged = policy1.mergeRestrictive(policy2)
            merged.maxDepth shouldBe 5
            merged.maxChildrenPerScope shouldBe null
        }

        "should merge policies taking more restrictive children limit" {
            val policy1 = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 15).getOrNull()!!
            val policy2 = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 25).getOrNull()!!

            val merged = policy1.mergeRestrictive(policy2)
            merged.maxDepth shouldBe null
            merged.maxChildrenPerScope shouldBe 15
        }

        "should merge unlimited with limited taking limited" {
            val unlimited = HierarchyPolicy.default()
            val limited = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!

            val merged1 = unlimited.mergeRestrictive(limited)
            val merged2 = limited.mergeRestrictive(unlimited)

            merged1.maxDepth shouldBe 10
            merged1.maxChildrenPerScope shouldBe 20
            merged2.maxDepth shouldBe 10
            merged2.maxChildrenPerScope shouldBe 20
        }

        "should merge two unlimited policies remaining unlimited" {
            val unlimited1 = HierarchyPolicy.default()
            val unlimited2 = HierarchyPolicy.default()

            val merged = unlimited1.mergeRestrictive(unlimited2)
            merged.maxDepth shouldBe null
            merged.maxChildrenPerScope shouldBe null
        }

        // Limit adjustment tests
        "should increase limits correctly" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!

            val increased = policy.increaseLimits(depthIncrease = 5, childrenIncrease = 10)
            increased.maxDepth shouldBe 15
            increased.maxChildrenPerScope shouldBe 30
        }

        "should increase limits with zero values" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!

            val unchanged = policy.increaseLimits(depthIncrease = 0, childrenIncrease = 0)
            unchanged.maxDepth shouldBe 10
            unchanged.maxChildrenPerScope shouldBe 20
        }

        "should increase only specified limits" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!

            val depthOnly = policy.increaseLimits(depthIncrease = 5)
            depthOnly.maxDepth shouldBe 15
            depthOnly.maxChildrenPerScope shouldBe 20

            val childrenOnly = policy.increaseLimits(childrenIncrease = 10)
            childrenOnly.maxDepth shouldBe 10
            childrenOnly.maxChildrenPerScope shouldBe 30
        }

        "should increase unlimited policy remaining unlimited" {
            val unlimited = HierarchyPolicy.default()

            val increased = unlimited.increaseLimits(depthIncrease = 10, childrenIncrease = 20)
            increased.maxDepth shouldBe null
            increased.maxChildrenPerScope shouldBe null
        }

        "should increase mixed policy correctly" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!

            val increased = policy.increaseLimits(depthIncrease = 5, childrenIncrease = 10)
            increased.maxDepth shouldBe 15
            increased.maxChildrenPerScope shouldBe null
        }

        // Edge cases and boundary tests
        "should handle edge case values" {
            val policy = HierarchyPolicy.create(maxDepth = 1, maxChildrenPerScope = 1).getOrNull()!!

            policy.isDepthAllowed(1) shouldBe true
            policy.isDepthAllowed(2) shouldBe false
            policy.isChildrenCountAllowed(1) shouldBe true
            policy.isChildrenCountAllowed(2) shouldBe false

            policy.remainingDepth(0) shouldBe 1
            policy.remainingDepth(1) shouldBe 0
            policy.remainingChildrenSlots(0) shouldBe 1
            policy.remainingChildrenSlots(1) shouldBe 0
        }

        "should handle large values correctly" {
            val largeDepth = 1000000
            val largeChildren = 500000
            val policy = HierarchyPolicy.create(maxDepth = largeDepth, maxChildrenPerScope = largeChildren).getOrNull()!!

            policy.isDepthAllowed(largeDepth) shouldBe true
            policy.isDepthAllowed(largeDepth + 1) shouldBe false
            policy.isChildrenCountAllowed(largeChildren) shouldBe true
            policy.isChildrenCountAllowed(largeChildren + 1) shouldBe false
        }

        // Equality and immutability tests
        "should maintain equality for same policies" {
            val policy1 = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!
            val policy2 = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!
            val policy3 = HierarchyPolicy.create(maxDepth = 15, maxChildrenPerScope = 20).getOrNull()!!

            (policy1 == policy2) shouldBe true
            (policy1 == policy3) shouldBe false
            policy1.hashCode() shouldBe policy2.hashCode()
        }

        "should maintain equality for unlimited policies" {
            val unlimited1 = HierarchyPolicy.default()
            val unlimited2 = HierarchyPolicy.default()

            (unlimited1 == unlimited2) shouldBe true
            unlimited1.hashCode() shouldBe unlimited2.hashCode()
        }

        "should ensure immutability of policies" {
            val original = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!
            val modified = original.increaseLimits(depthIncrease = 5)

            // Original should remain unchanged
            original.maxDepth shouldBe 10
            original.maxChildrenPerScope shouldBe 20

            // Modified should have new values
            modified.maxDepth shouldBe 15
            modified.maxChildrenPerScope shouldBe 20

            // They should be different instances
            (original == modified) shouldBe false
        }

        "should have meaningful toString representation" {
            val policy = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = 20).getOrNull()!!
            val unlimited = HierarchyPolicy.default()

            policy.toString() shouldNotBe ""
            unlimited.toString() shouldNotBe ""
            policy.toString() shouldNotBe unlimited.toString()
        }

        "should handle complex policy scenarios" {
            // Realistic scenario: combining user preferences with system defaults
            val userPreference = HierarchyPolicy.create(maxDepth = 50, maxChildrenPerScope = null).getOrNull()!!
            val systemDefault = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 100).getOrNull()!!

            val effective = userPreference.mergeRestrictive(systemDefault)
            effective.maxDepth shouldBe 50 // from user preference
            effective.maxChildrenPerScope shouldBe 100 // from system default

            // Test validation with effective policy
            effective.isDepthAllowed(25) shouldBe true
            effective.isDepthAllowed(75) shouldBe false
            effective.isChildrenCountAllowed(50) shouldBe true
            effective.isChildrenCountAllowed(150) shouldBe false
        }

        "should handle edge cases in merging with partial limits" {
            val depthOnly = HierarchyPolicy.create(maxDepth = 10, maxChildrenPerScope = null).getOrNull()!!
            val childrenOnly = HierarchyPolicy.create(maxDepth = null, maxChildrenPerScope = 20).getOrNull()!!

            val merged = depthOnly.mergeRestrictive(childrenOnly)
            merged.maxDepth shouldBe 10
            merged.maxChildrenPerScope shouldBe 20

            // Verify the merged policy works correctly
            merged.isDepthAllowed(5) shouldBe true
            merged.isDepthAllowed(15) shouldBe false
            merged.isChildrenCountAllowed(10) shouldBe true
            merged.isChildrenCountAllowed(25) shouldBe false
        }
    })
