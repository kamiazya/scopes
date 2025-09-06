package io.github.kamiazya.scopes.scopemanagement.domain.event

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventTypeId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ContextView
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
import kotlinx.datetime.Instant

/**
 * Events related to ContextView aggregate.
 */
sealed class ContextViewEvent : DomainEvent

/**
 * Event fired when a new ContextView is created.
 */
@EventTypeId("scope-management.context-view.created.v1")
data class ContextViewCreated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val key: ContextViewKey,
    val name: ContextViewName,
    val filter: ContextViewFilter,
    val description: ContextViewDescription?,
) : ContextViewEvent() {
    companion object {
        fun from(contextView: ContextView, eventId: EventId): Either<AggregateIdError, ContextViewCreated> = contextView.id.toAggregateId().map { aggregateId ->
            ContextViewCreated(
                aggregateId = aggregateId,
                eventId = eventId,
                occurredAt = contextView.createdAt,
                aggregateVersion = AggregateVersion.initial().increment(),
                contextViewId = contextView.id,
                key = contextView.key,
                name = contextView.name,
                filter = contextView.filter,
                description = contextView.description,
            )
        }
    }
}

/**
 * Event fired when a ContextView is updated.
 */
@EventTypeId("scope-management.context-view.updated.v1")
data class ContextViewUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val changes: ContextViewChanges,
) : ContextViewEvent()

/**
 * Event fired when a ContextView's name is changed.
 */
@EventTypeId("scope-management.context-view.name-changed.v1")
data class ContextViewNameChanged(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val oldName: ContextViewName,
    val newName: ContextViewName,
) : ContextViewEvent()

/**
 * Event fired when a ContextView's filter is updated.
 */
@EventTypeId("scope-management.context-view.filter-updated.v1")
data class ContextViewFilterUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val oldFilter: ContextViewFilter,
    val newFilter: ContextViewFilter,
) : ContextViewEvent()

/**
 * Event fired when a ContextView's description is updated.
 */
@EventTypeId("scope-management.context-view.description-updated.v1")
data class ContextViewDescriptionUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val oldDescription: ContextViewDescription?,
    val newDescription: ContextViewDescription?,
) : ContextViewEvent()

/**
 * Event fired when a ContextView is deleted.
 */
@EventTypeId("scope-management.context-view.deleted.v1")
data class ContextViewDeleted(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val deletedAt: Instant,
) : ContextViewEvent()

/**
 * Represents changes made to a ContextView for update events.
 */
data class ContextViewChanges(
    val keyChange: KeyChange? = null,
    val nameChange: NameChange? = null,
    val filterChange: FilterChange? = null,
    val descriptionChange: DescriptionChange? = null,
) {
    data class KeyChange(val oldKey: ContextViewKey, val newKey: ContextViewKey)
    data class NameChange(val oldName: ContextViewName, val newName: ContextViewName)
    data class FilterChange(val oldFilter: ContextViewFilter, val newFilter: ContextViewFilter)
    data class DescriptionChange(val oldDescription: ContextViewDescription?, val newDescription: ContextViewDescription?)
}

/**
 * Event fired when a ContextView is activated (becomes the active context).
 * This event is critical for audit logging of context switches.
 */
@EventTypeId("scope-management.context-view.activated.v1")
data class ContextViewActivated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val contextKey: ContextViewKey,
    val contextName: ContextViewName,
    val previousContextId: ContextViewId? = null,
    val activatedBy: String? = null, // User/system that activated the context
) : ContextViewEvent()

/**
 * Event fired when the active context is cleared (no context is active).
 * This event is important for tracking when users work without an active filter.
 */
@EventTypeId("scope-management.active-context.cleared.v1")
data class ActiveContextCleared(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val previousContextId: ContextViewId,
    val previousContextKey: ContextViewKey,
    val clearedBy: String? = null, // User/system that cleared the context
) : ContextViewEvent()

/**
 * Event fired when scopes are filtered using a context view.
 * This provides audit trail for context usage without switching active context.
 */
@EventTypeId("scope-management.context-view.applied.v1")
data class ContextViewApplied(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val occurredAt: Instant,
    override val aggregateVersion: AggregateVersion,
    val contextViewId: ContextViewId,
    val contextKey: ContextViewKey,
    val scopeCount: Int, // Number of scopes returned after filtering
    val totalScopeCount: Int, // Total number of scopes before filtering
    val appliedBy: String? = null, // User/system that applied the filter
) : ContextViewEvent()
