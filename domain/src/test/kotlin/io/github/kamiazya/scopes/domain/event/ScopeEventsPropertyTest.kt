package io.github.kamiazya.scopes.domain.event

import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import arrow.core.getOrElse
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.valueobject.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import io.kotest.property.checkAll
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.hours
import com.github.guepardoapps.kulid.ULID

class ScopeEventsPropertyTest : StringSpec({

    "ScopeCreated event should preserve all properties from Scope entity" {
        checkAll(
            validScopeTitleArb(),
            validScopeDescriptionArb().orNull(0.3),
            validScopeIdArb().orNull(0.5),
            validAspectsArb()
        ) { title, description, parentId, aspects ->
            // Create scope
            val scopeResult = Scope.create(
                title = title.value,
                description = description?.value,
                parentId = parentId,
                aspectsData = aspects
            )
            
            scopeResult.isRight() shouldBe true
            scopeResult.fold(
                { throw AssertionError("Failed to create scope: $it") },
                { scope ->
                    val eventId = EventId.create("ScopeCreated").getOrElse { throw AssertionError() }
                    val result = ScopeCreated.from(scope, eventId)
                    
                    result.isRight() shouldBe true
                    result.fold(
                        { throw AssertionError("Failed to create event: $it") },
                        { event ->
                            // Verify all properties are preserved
                            event.scopeId shouldBe scope.id
                            event.title shouldBe scope.title
                            event.description shouldBe scope.description
                            event.parentId shouldBe scope.parentId
                            event.aspects shouldBe scope.getAspects()
                            event.occurredAt shouldBe scope.createdAt
                            event.version shouldBe 1
                            event.eventId shouldBe eventId
                            event.aggregateId.aggregateType shouldBe "Scope"
                            event.eventType shouldBe "ScopeCreated"
                        }
                    )
                }
            )
        }
    }

    "ScopeTitleUpdated event should have different old and new titles" {
        checkAll(
            validScopeIdArb(),
            validScopeTitleArb(),
            validScopeTitleArb(),
            Arb.int(2..100)
        ) { scopeId, oldTitle, newTitle, version ->
            if (oldTitle != newTitle) {
                val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
                val eventId = EventId.create("ScopeTitleUpdated").getOrElse { throw AssertionError() }
                val occurredAt = Clock.System.now()
                
                val event = ScopeTitleUpdated(
                    aggregateId = aggregateId,
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = version,
                    scopeId = scopeId,
                    oldTitle = oldTitle,
                    newTitle = newTitle
                )
                
                event.oldTitle shouldNotBe event.newTitle
                event.scopeId shouldBe scopeId
                event.version shouldBe version
                event.eventType shouldBe "ScopeTitleUpdated"
            }
        }
    }

    "ScopeDescriptionUpdated event should handle null descriptions correctly" {
        checkAll(
            validScopeIdArb(),
            validScopeDescriptionArb().orNull(0.5),
            validScopeDescriptionArb().orNull(0.5),
            Arb.int(2..100)
        ) { scopeId, oldDescription, newDescription, version ->
            val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
            val eventId = EventId.create("ScopeDescriptionUpdated").getOrElse { throw AssertionError() }
            val occurredAt = Clock.System.now()
            
            val event = ScopeDescriptionUpdated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                oldDescription = oldDescription,
                newDescription = newDescription
            )
            
            event.oldDescription shouldBe oldDescription
            event.newDescription shouldBe newDescription
            event.eventType shouldBe "ScopeDescriptionUpdated"
        }
    }

    "ScopeParentChanged event should handle null parent IDs correctly" {
        checkAll(
            validScopeIdArb(),
            validScopeIdArb().orNull(0.3),
            validScopeIdArb().orNull(0.3),
            Arb.int(2..100)
        ) { scopeId, oldParentId, newParentId, version ->
            if (oldParentId != newParentId) {
                val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
                val eventId = EventId.create("ScopeParentChanged").getOrElse { throw AssertionError() }
                val occurredAt = Clock.System.now()
                
                val event = ScopeParentChanged(
                    aggregateId = aggregateId,
                    eventId = eventId,
                    occurredAt = occurredAt,
                    version = version,
                    scopeId = scopeId,
                    oldParentId = oldParentId,
                    newParentId = newParentId
                )
                
                event.oldParentId shouldBe oldParentId
                event.newParentId shouldBe newParentId
                event.eventType shouldBe "ScopeParentChanged"
            }
        }
    }

    "ScopeAspectAdded event should have valid aspect key and value" {
        checkAll(
            validScopeIdArb(),
            validAspectKeyArb(),
            validAspectValueArb(),
            Arb.int(2..100)
        ) { scopeId, aspectKey, aspectValue, version ->
            val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
            val eventId = EventId.create("ScopeAspectAdded").getOrElse { throw AssertionError() }
            val occurredAt = Clock.System.now()
            
            val event = ScopeAspectAdded(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                aspectKey = aspectKey,
                aspectValue = aspectValue
            )
            
            event.aspectKey shouldBe aspectKey
            event.aspectValue shouldBe aspectValue
            event.eventType shouldBe "ScopeAspectAdded"
        }
    }

    "ScopeDeleted event should have valid deleted timestamp" {
        checkAll(
            validScopeIdArb(),
            Arb.int(2..100)
        ) { scopeId, version ->
            val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
            val eventId = EventId.create("ScopeDeleted").getOrElse { throw AssertionError() }
            val occurredAt = Clock.System.now()
            val deletedAt = occurredAt
            
            val event = ScopeDeleted(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                deletedAt = deletedAt
            )
            
            event.scopeId shouldBe scopeId
            event.deletedAt shouldBe deletedAt
            event.deletedAt shouldBe event.occurredAt
            event.eventType shouldBe "ScopeDeleted"
        }
    }

    "All scope events should have consistent aggregate ID properties" {
        checkAll(validScopeIdArb()) { scopeId ->
            val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
            val eventId = EventId.create("ScopeCreated").getOrElse { throw AssertionError() }
            val occurredAt = Clock.System.now()
            
            val title = ScopeTitle.create("Test").getOrElse { throw AssertionError() }
            val event = ScopeCreated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = 1,
                scopeId = scopeId,
                title = title,
                description = null,
                parentId = null,
                aspects = emptyMap()
            )
            
            // Verify aggregate ID properties
            event.aggregateId.value shouldBe "gid://scopes/Scope/${scopeId.value}"
            event.aggregateId.aggregateType shouldBe "Scope"
            event.aggregateId.id shouldBe scopeId.value
        }
    }

    "Event versions should be positive integers" {
        checkAll(
            validScopeIdArb(),
            Arb.int(1..Int.MAX_VALUE)
        ) { scopeId, version ->
            val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
            val eventId = EventId.create("ScopeArchived").getOrElse { throw AssertionError() }
            val occurredAt = Clock.System.now()
            
            val event = ScopeArchived(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = version,
                scopeId = scopeId,
                reason = "Archived for testing"
            )
            
            event.version shouldBe version
            (event.version > 0) shouldBe true
        }
    }

    "ScopeChanges should handle all combinations of changes" {
        checkAll(
            validScopeTitleArb().orNull(0.3).map { it?.let { old -> 
                ScopeChanges.TitleChange(old, ScopeTitle.create("New Title").getOrElse { throw AssertionError() })
            }},
            validScopeDescriptionArb().orNull(0.5).map { it?.let { old ->
                ScopeChanges.DescriptionChange(old, ScopeDescription.create("New Description").getOrElse { throw AssertionError() })
            }},
            validScopeIdArb().orNull(0.3).map { it?.let { old ->
                ScopeChanges.ParentChange(old, ScopeId.generate())
            }},
            Arb.list(validAspectChangeArb(), 0..5)
        ) { titleChange, descriptionChange, parentChange, aspectChanges ->
            val changes = ScopeChanges(
                titleChange = titleChange,
                descriptionChange = descriptionChange,
                parentChange = parentChange,
                aspectChanges = aspectChanges
            )
            
            changes.titleChange shouldBe titleChange
            changes.descriptionChange shouldBe descriptionChange
            changes.parentChange shouldBe parentChange
            changes.aspectChanges shouldBe aspectChanges
        }
    }

    "Event timestamps should be reasonable" {
        checkAll(validScopeIdArb()) { scopeId ->
            val aggregateId = scopeId.toAggregateId().getOrElse { throw AssertionError() }
            val eventId = EventId.create("ScopeRestored").getOrElse { throw AssertionError() }
            val occurredAt = Clock.System.now()
            
            val event = ScopeRestored(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = occurredAt,
                version = 2,
                scopeId = scopeId
            )
            
            // Verify timestamp is recent (within last hour)
            val now = Clock.System.now()
            val oneHourAgo = now - 1.hours
            (event.occurredAt >= oneHourAgo) shouldBe true
            (event.occurredAt <= now) shouldBe true
        }
    }
})

// Custom Arbitrary generators
private fun validScopeIdArb(): Arb<ScopeId> = arbitrary {
    ScopeId.generate()
}

private fun validScopeTitleArb(): Arb<ScopeTitle> = arbitrary {
    val titles = listOf(
        "Project Alpha", "Sprint 1", "Bug Fix", "Feature Development",
        "Documentation", "Testing Phase", "Release Planning", "Code Review"
    )
    ScopeTitle.create(titles.random()).getOrElse { throw AssertionError() }
}

private fun validScopeDescriptionArb(): Arb<ScopeDescription> = arbitrary {
    val descriptions = listOf(
        "This is a detailed description of the scope",
        "Implementation of new feature with enhanced functionality",
        "Bug fix for critical issue in production",
        "Performance optimization for database queries",
        "Refactoring legacy code to improve maintainability"
    )
    ScopeDescription.create(descriptions.random()).getOrElse { throw AssertionError() }!!
}

private fun validAspectKeyArb(): Arb<AspectKey> = arbitrary {
    val keys = listOf("status", "priority", "category", "type", "stage", "environment")
    AspectKey.create(keys.random()).getOrElse { throw AssertionError() }
}

private fun validAspectValueArb(): Arb<AspectValue> = arbitrary {
    val values = listOf("active", "high", "bug", "feature", "development", "production", "testing")
    AspectValue.create(values.random()).getOrElse { throw AssertionError() }
}

private fun validAspectsArb(): Arb<Map<AspectKey, NonEmptyList<AspectValue>>> = arbitrary {
    val aspectCount = Arb.int(0..3).bind()
    val aspects = mutableMapOf<AspectKey, NonEmptyList<AspectValue>>()
    
    repeat(aspectCount) {
        val key = validAspectKeyArb().bind()
        val valueCount = Arb.int(1..3).bind()
        val values = (1..valueCount).map { validAspectValueArb().bind() }
        if (values.isNotEmpty()) {
            aspects[key] = nonEmptyListOf(values.first(), *values.drop(1).toTypedArray())
        }
    }
    
    aspects
}

private fun validAspectChangeArb(): Arb<ScopeChanges.AspectChange> = arbitrary {
    val key = validAspectKeyArb().bind()
    val value = validAspectValueArb().bind()
    
    if (Arb.boolean().bind()) {
        ScopeChanges.AspectChange.Added(key, value)
    } else {
        ScopeChanges.AspectChange.Removed(key, value)
    }
}