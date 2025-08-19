package io.github.kamiazya.scopes.domain.event

import arrow.core.Either
import arrow.core.NonEmptyList
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import io.github.kamiazya.scopes.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.domain.valueobject.EventId
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.valueobject.ScopeTitle
import io.github.kamiazya.scopes.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.domain.valueobject.AspectValue
import kotlinx.datetime.Instant

/**
 * Events related to Scope aggregate.
 */
sealed class ScopeEvent : DomainEvent()

/**
 * Event fired when a new Scope is created.
 */
data class ScopeCreated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val title: ScopeTitle,
    val description: ScopeDescription?,
    val parentId: ScopeId?,
    val aspects: Map<AspectKey, NonEmptyList<AspectValue>>
) : ScopeEvent() {
    companion object {
        fun from(scope: Scope, eventId: EventId): Either<AggregateIdError, ScopeCreated> {
            return scope.id.toAggregateId().map { aggregateId ->
                ScopeCreated(
                    aggregateId = aggregateId,
                    eventId = eventId,
                    occurredAt = scope.createdAt,
                    version = 1,
                    scopeId = scope.id,
                    title = scope.title,
                    description = scope.description,
                    parentId = scope.parentId,
                    aspects = scope.getAspects()
                )
            }
        }
    }
}

/**
 * Event fired when a Scope's title is updated.
 */
data class ScopeTitleUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val oldTitle: ScopeTitle,
    val newTitle: ScopeTitle
) : ScopeEvent()

/**
 * Event fired when a Scope's description is updated.
 */
data class ScopeDescriptionUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val oldDescription: ScopeDescription?,
    val newDescription: ScopeDescription?
) : ScopeEvent()

/**
 * Event fired when a Scope's parent is changed.
 */
data class ScopeParentChanged(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val oldParentId: ScopeId?,
    val newParentId: ScopeId?
) : ScopeEvent()

/**
 * Event fired when an aspect is added to a Scope.
 */
data class ScopeAspectAdded(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val aspectKey: AspectKey,
    val aspectValue: AspectValue
) : ScopeEvent()

/**
 * Event fired when an aspect is removed from a Scope.
 */
data class ScopeAspectRemoved(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val aspectKey: AspectKey,
    val aspectValue: AspectValue
) : ScopeEvent()

/**
 * Event fired when all values for an aspect key are removed from a Scope.
 */
data class ScopeAspectCleared(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val aspectKey: AspectKey,
    val removedValues: NonEmptyList<AspectValue>
) : ScopeEvent()

/**
 * Event fired when a Scope is archived (soft deleted).
 */
data class ScopeArchived(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val reason: String?
) : ScopeEvent()

/**
 * Event fired when an archived Scope is restored.
 */
data class ScopeRestored(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId
) : ScopeEvent()

/**
 * Event fired when a Scope is permanently deleted.
 */
data class ScopeDeleted(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
    val deletedAt: Instant
) : ScopeEvent()

/**
 * Represents changes made to a Scope for update events.
 */
data class ScopeChanges(
    val titleChange: TitleChange? = null,
    val descriptionChange: DescriptionChange? = null,
    val parentChange: ParentChange? = null,
    val aspectChanges: List<AspectChange> = emptyList()
) {
    data class TitleChange(val oldTitle: ScopeTitle, val newTitle: ScopeTitle)
    data class DescriptionChange(val oldDescription: ScopeDescription?, val newDescription: ScopeDescription?)
    data class ParentChange(val oldParentId: ScopeId?, val newParentId: ScopeId?)
    
    sealed class AspectChange {
        data class Added(val key: AspectKey, val value: AspectValue) : AspectChange()
        data class Removed(val key: AspectKey, val value: AspectValue) : AspectChange()
        data class Cleared(val key: AspectKey, val values: NonEmptyList<AspectValue>) : AspectChange()
    }
}