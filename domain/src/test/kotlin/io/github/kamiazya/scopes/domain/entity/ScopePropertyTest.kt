package io.github.kamiazya.scopes.domain.entity

import arrow.core.nonEmptyListOf
import io.github.kamiazya.scopes.domain.valueobject.*
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.datetime.Clock

class ScopePropertyTest : StringSpec({

    "valid scopes should always be created successfully" {
        checkAll(validTitleArb(), validDescriptionArb()) { title, description ->
            val result = Scope.create(
                title = title,
                description = description
            )
            
            result.shouldBeRight()
            val scope = result.getOrNull()
            scope shouldNotBe null
            scope?.title?.value shouldBe title.trim()
            // Description can be null if empty after trimming
            if (description?.trim()?.isNotEmpty() == true) {
                scope?.description?.value shouldBe description.trim()
            } else {
                scope?.description shouldBe null
            }
            scope?.parentId shouldBe null
            scope?.id shouldNotBe null
        }
    }

    "scope creation should generate unique IDs" {
        checkAll(100, validTitleArb()) { title ->
            val scope1 = Scope.create(title = title).getOrNull()!!
            val scope2 = Scope.create(title = title).getOrNull()!!
            
            scope1.id shouldNotBe scope2.id
        }
    }

    "scope creation should set creation and update timestamps" {
        checkAll(validTitleArb()) { title ->
            val beforeCreation = Clock.System.now()
            val scope = Scope.create(title = title).getOrNull()!!
            val afterCreation = Clock.System.now()
            
            (scope.createdAt >= beforeCreation) shouldBe true
            (scope.createdAt <= afterCreation) shouldBe true
            scope.createdAt shouldBe scope.updatedAt
        }
    }

    "scope with parent should maintain parent reference" {
        checkAll(validTitleArb()) { title ->
            val parentId = ScopeId.generate()
            val scope = Scope.create(
                title = title,
                parentId = parentId
            ).getOrNull()!!
            
            scope.parentId shouldBe parentId
        }
    }

    "updating scope title should preserve other properties" {
        checkAll(validTitleArb(), validTitleArb()) { originalTitle, newTitle ->
            val originalScope = Scope.create(
                title = originalTitle,
                description = "Test description"
            ).getOrNull()
            
            if (originalScope != null) {
                Thread.sleep(1) // Ensure timestamp difference
                val updateResult = originalScope.updateTitle(newTitle)
                if (updateResult.isRight()) {
                    val updatedScope = updateResult.getOrNull()!!
                    updatedScope.id shouldBe originalScope.id
                    updatedScope.title.value shouldBe newTitle.trim()
                    updatedScope.description shouldBe originalScope.description
                    updatedScope.parentId shouldBe originalScope.parentId
                    updatedScope.createdAt shouldBe originalScope.createdAt
                    (updatedScope.updatedAt >= originalScope.updatedAt) shouldBe true
                }
            }
        }
    }

    "updating scope description should preserve other properties" {
        checkAll(validTitleArb(), validDescriptionArb()) { title, newDescription ->
            val originalScope = Scope.create(title = title).getOrNull()
            if (originalScope != null) {
                Thread.sleep(1) // Ensure timestamp difference
                val updateResult = originalScope.updateDescription(newDescription)
                if (updateResult.isRight()) {
                    val updatedScope = updateResult.getOrNull()!!
                    updatedScope.id shouldBe originalScope.id
                    updatedScope.title shouldBe originalScope.title
                    // Description can be null if empty after trimming
                    if (newDescription?.trim()?.isNotEmpty() == true) {
                        updatedScope.description?.value shouldBe newDescription.trim()
                    } else {
                        updatedScope.description shouldBe null
                    }
                    updatedScope.parentId shouldBe originalScope.parentId
                    updatedScope.createdAt shouldBe originalScope.createdAt
                    (updatedScope.updatedAt >= originalScope.updatedAt) shouldBe true
                }
            }
        }
    }

    "moving scope to new parent should update parent reference" {
        checkAll(validTitleArb()) { title ->
            val originalScope = Scope.create(title = title).getOrNull()!!
            val newParentId = ScopeId.generate()
            
            Thread.sleep(1) // Ensure timestamp difference
            val movedScope = originalScope.moveToParent(newParentId)
            
            movedScope.id shouldBe originalScope.id
            movedScope.title shouldBe originalScope.title
            movedScope.parentId shouldBe newParentId
            (movedScope.updatedAt > originalScope.updatedAt) shouldBe true
        }
    }

    "scope should handle aspect operations correctly" {
        checkAll(validTitleArb(), validAspectKeyArb(), validAspectValueArb()) { title, key, value ->
            val scope = Scope.create(title = title).getOrNull()!!
            val aspectKey = AspectKey.create(key).getOrNull()!!
            val aspectValue = AspectValue.create(value).getOrNull()!!
            
            // Initially no aspects
            scope.hasAspect(aspectKey) shouldBe false
            scope.getAspectValue(aspectKey) shouldBe null
            
            Thread.sleep(1) // Ensure timestamp difference
            // Set aspect
            val withAspect = scope.setAspect(aspectKey, aspectValue)
            withAspect.hasAspect(aspectKey) shouldBe true
            withAspect.getAspectValue(aspectKey) shouldBe aspectValue
            (withAspect.updatedAt > scope.updatedAt) shouldBe true
            
            Thread.sleep(1) // Ensure timestamp difference
            // Remove aspect
            val withoutAspect = withAspect.removeAspect(aspectKey)
            withoutAspect.hasAspect(aspectKey) shouldBe false
            withoutAspect.getAspectValue(aspectKey) shouldBe null
            (withoutAspect.updatedAt > withAspect.updatedAt) shouldBe true
        }
    }

    "scope should handle multiple aspect values" {
        checkAll(
            validTitleArb(),
            validAspectKeyArb(),
            Arb.list(validAspectValueArb(), 1..5)
        ) { title, key, values ->
            val scope = Scope.create(title = title).getOrNull()!!
            val aspectKey = AspectKey.create(key).getOrNull()!!
            val aspectValues = values.mapNotNull { AspectValue.create(it).getOrNull() }
            
            if (aspectValues.isNotEmpty()) {
                val nonEmptyValues = nonEmptyListOf(aspectValues.first(), *aspectValues.drop(1).toTypedArray())
                val withAspects = scope.setAspect(aspectKey, nonEmptyValues)
                
                withAspects.hasAspect(aspectKey) shouldBe true
                withAspects.getAspectValues(aspectKey) shouldBe nonEmptyValues
                withAspects.getAspectValue(aspectKey) shouldBe nonEmptyValues.head
            }
        }
    }

    "clearing aspects should remove all aspects" {
        checkAll(
            validTitleArb(),
            Arb.list(Arb.pair(validAspectKeyArb(), validAspectValueArb()), 1..5)
        ) { title, aspectsList ->
            val scopeResult = Scope.create(title = title)
            if (scopeResult.isRight()) {
                val scope = scopeResult.getOrNull()!!
            
                // Add multiple aspects
                var withAspects: Scope = scope
                aspectsList.forEach { (key, value) ->
                    val aspectKey = AspectKey.create(key).getOrNull()!!
                    val aspectValue = AspectValue.create(value).getOrNull()!!
                    withAspects = withAspects.setAspect(aspectKey, aspectValue)
                }
                
                Thread.sleep(1) // Ensure timestamp difference
                // Clear all aspects
                val cleared = withAspects.clearAspects()
                cleared.getAspects().isEmpty() shouldBe true
                (cleared.updatedAt >= withAspects.updatedAt) shouldBe true
            }
        }
    }

    "scope immutability - operations should return new instances" {
        checkAll(validTitleArb()) { title ->
            val original = Scope.create(title = title).getOrNull()!!
            
            val updated = original.updateTitle("New Title").getOrNull()!!
            original shouldNotBeSameInstanceAs updated
            original.title.value shouldBe title.trim()
            
            val moved = original.moveToParent(ScopeId.generate())
            original shouldNotBeSameInstanceAs moved
            original.parentId shouldBe null
        }
    }

    "isRoot should correctly identify root scopes" {
        checkAll(validTitleArb()) { title ->
            val rootScope = Scope.create(title = title).getOrNull()!!
            rootScope.isRoot() shouldBe true
            
            val childScope = Scope.create(
                title = title,
                parentId = ScopeId.generate()
            ).getOrNull()!!
            childScope.isRoot() shouldBe false
        }
    }

    "isChildOf should correctly identify parent-child relationships" {
        checkAll(validTitleArb()) { title ->
            val parent = Scope.create(title = "Parent").getOrNull()!!
            val child = Scope.create(
                title = title,
                parentId = parent.id
            ).getOrNull()!!
            val unrelated = Scope.create(title = "Unrelated").getOrNull()!!
            
            child.isChildOf(parent) shouldBe true
            child.isChildOf(unrelated) shouldBe false
            parent.isChildOf(child) shouldBe false
        }
    }

    "canBeParentOf should prevent self-parenting" {
        checkAll(validTitleArb()) { title ->
            val scope = Scope.create(title = title).getOrNull()!!
            scope.canBeParentOf(scope) shouldBe false
        }
    }

    "canBeParentOf should prevent circular references" {
        checkAll(validTitleArb()) { title ->
            val parent = Scope.create(title = "Parent").getOrNull()!!
            val child = Scope.create(
                title = title,
                parentId = parent.id
            ).getOrNull()!!
            
            parent.canBeParentOf(child) shouldBe false // Would create circular reference
        }
    }

    "invalid titles should fail scope creation" {
        checkAll(invalidTitleArb()) { title ->
            val result = Scope.create(title = title)
            result.isLeft() shouldBe true
        }
    }

    "invalid descriptions should fail scope creation" {
        checkAll(validTitleArb(), tooLongDescriptionArb()) { title, description ->
            val result = Scope.create(
                title = title,
                description = description
            )
            result.shouldBeLeft()
        }
    }

    "scope aspect operations should be chainable" {
        checkAll(validTitleArb()) { title ->
            val scope = Scope.create(title = title).getOrNull()!!
            val key1 = AspectKey.create("priority").getOrNull()!!
            val key2 = AspectKey.create("status").getOrNull()!!
            val value1 = AspectValue.create("high").getOrNull()!!
            val value2 = AspectValue.create("active").getOrNull()!!
            
            val chained = scope
                .setAspect(key1, value1)
                .setAspect(key2, value2)
            
            chained.getAspectValue(key1) shouldBe value1
            chained.getAspectValue(key2) shouldBe value2
        }
    }
})

// Custom Arbitrary generators
private fun validTitleArb(): Arb<String> = Arb.string(1..100)
    .filter { it.trim().isNotEmpty() && !it.contains('\n') && !it.contains('\r') }

private fun validDescriptionArb(): Arb<String?> = Arb.choice(
    Arb.constant(null),
    Arb.string(0..500).filter { it.trim().length <= 1000 }
)

private fun invalidTitleArb(): Arb<String> = Arb.choice(
    Arb.of("", " ", "  ", "\t"),
    Arb.string(201..300).filter { it.trim().length > 200 }, // Too long after trimming
    Arb.string(2..50).filter { it.trim().length >= 2 && !it.contains('\n') && !it.contains('\r') }
        .map { base -> 
            val trimmed = base.trim()
            val midpoint = trimmed.length / 2
            if (midpoint > 0) {
                trimmed.take(midpoint) + "\n" + trimmed.drop(midpoint)
            } else {
                trimmed + "\n"
            }
        }, // Newline in middle
    Arb.string(2..50).filter { it.trim().length >= 2 && !it.contains('\n') && !it.contains('\r') }
        .map { base -> 
            val trimmed = base.trim()
            val midpoint = trimmed.length / 2
            if (midpoint > 0) {
                trimmed.take(midpoint) + "\r" + trimmed.drop(midpoint)
            } else {
                trimmed + "\r"
            }
        } // Carriage return in middle
)

private fun tooLongDescriptionArb(): Arb<String> = Arb.string(1001..2000).filter { it.trim().length > 1000 }

private fun validAspectKeyArb(): Arb<String> = Arb.stringPattern("[a-z][a-z0-9_]{0,49}")

private fun validAspectValueArb(): Arb<String> = Arb.string(1..50)
    .filter { it.trim().isNotEmpty() }
