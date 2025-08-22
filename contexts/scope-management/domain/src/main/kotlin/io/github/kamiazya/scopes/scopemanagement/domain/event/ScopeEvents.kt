package io.github.kamiazya.scopes.scopemanagement.domain.event

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AggregateId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
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
) : ScopeEvent() {
    companion object {
        fun from(scope: Scope, eventId: EventId): Either<AggregateIdError, ScopeCreated> = scope.id.toAggregateId().map { aggregateId ->
            ScopeCreated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = scope.createdAt,
                version = 1,
                scopeId = scope.id,
                title = scope.title,
                description = scope.description,
                parentId = scope.parentId,
            )
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
    val newTitle: ScopeTitle,
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
    val newDescription: ScopeDescription?,
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
    val newParentId: ScopeId?,
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
    val reason: String?,
) : ScopeEvent()

/**
 * Event fired when an archived Scope is restored.
 */
data class ScopeRestored(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val version: Int,
    val scopeId: ScopeId,
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
) : ScopeEvent()
