package io.github.kamiazya.scopes.scopemanagement.domain.event

import arrow.core.Either
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
