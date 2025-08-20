package io.github.kamiazya.scopes.domain.entity

import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.domain.valueobject.*
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll

/**
 * Simplified property-based tests for Scope entity focusing on core invariants.
 */
class SimpleScopePropertyTest : StringSpec({

    "valid titles should create valid scopes" {
        checkAll(Arb.string(1..100).filter {
            it.trim().isNotEmpty() &&
            !it.contains('\n') &&
            !it.contains('\r')
        }) { title ->
            val result = Scope.create(title = title)
            result.shouldBeRight()
            result.getOrNull()?.title?.value shouldBe title.trim()
        }
    }

    "empty titles should fail" {
        checkAll(Arb.of("", " ", "  ", "\t", "    ")) { title ->
            val result = Scope.create(title = title)
            result.shouldBeLeft()
        }
    }

    "titles with newlines should fail" {
        checkAll(Arb.string(2..25).filter { it.trim().length >= 2 && !it.contains('\n') && !it.contains('\r') }) { base ->
            // Put newline in the middle so it survives trimming
            // Ensure we have at least 1 character on each side of the newline
            val trimmed = base.trim()
            val midpoint = trimmed.length / 2
            val titleWithNewline = if (midpoint > 0) {
                trimmed.take(midpoint) + "\n" + trimmed.drop(midpoint)
            } else {
                trimmed + "\n" + trimmed  // Fallback for edge case
            }
            val result = Scope.create(title = titleWithNewline)
            result.shouldBeLeft()
        }
    }

    "very long titles should fail" {
        checkAll(Arb.string(201..300).filter { it.trim().length > 200 }) { title ->
            val result = Scope.create(title = title)
            result.shouldBeLeft()
        }
    }

    "scope IDs should be unique" {
        val ids = (1..100).map {
            Scope.create(title = "Test $it").getOrNull()?.id
        }.filterNotNull()

        ids.distinct().size shouldBe ids.size
    }

    "updating title preserves identity" {
        checkAll(
            Arb.string(1..50).filter { it.trim().isNotEmpty() && !it.contains('\n') },
            Arb.string(1..50).filter { it.trim().isNotEmpty() && !it.contains('\n') }
        ) { title1, title2 ->
            val scope = Scope.create(title = title1).getOrNull()
            if (scope != null) {
                val updated = scope.updateTitle(title2).getOrNull()
                if (updated != null) {
                    updated.id shouldBe scope.id
                    updated.title.value shouldBe title2.trim()
                }
            }
        }
    }

    "aspects can be added and removed" {
        checkAll(
            Arb.string(1..50).filter { it.trim().isNotEmpty() && !it.contains('\n') }
        ) { title ->
            val scope = Scope.create(title = title).getOrNull()
            if (scope != null) {
                // Test with a known valid aspect key/value
                val key = AspectKey.create("status").getOrNull()!!
                val value = AspectValue.create("active").getOrNull()!!

                // Add aspect
                val withAspect = scope.setAspect(key, value)
                withAspect.hasAspect(key) shouldBe true
                withAspect.getAspectValue(key) shouldBe value

                // Remove aspect
                val withoutAspect = withAspect.removeAspect(key)
                withoutAspect.hasAspect(key) shouldBe false
            }
        }
    }

    "scope is immutable" {
        checkAll(
            Arb.string(1..50).filter { it.trim().isNotEmpty() && !it.contains('\n') }
        ) { title ->
            val scope = Scope.create(title = title).getOrNull()
            if (scope != null) {
                val updated = scope.updateTitle("New Title")
                // Original should remain unchanged
                scope.title.value shouldBe title.trim()
            }
        }
    }

    "parent relationships are maintained" {
        checkAll(
            Arb.string(1..50).filter { it.trim().isNotEmpty() && !it.contains('\n') }
        ) { title ->
            val parentId = ScopeId.generate()
            val scope = Scope.create(title = title, parentId = parentId).getOrNull()
            scope?.parentId shouldBe parentId
            scope?.isRoot() shouldBe false
        }
    }

    "root scopes have no parent" {
        checkAll(
            Arb.string(1..50).filter { it.trim().isNotEmpty() && !it.contains('\n') }
        ) { title ->
            val scope = Scope.create(title = title).getOrNull()
            scope?.parentId shouldBe null
            scope?.isRoot() shouldBe true
        }
    }
})
