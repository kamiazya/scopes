package io.github.kamiazya.scopes.scopemanagement.domain.event

import arrow.core.Either
import arrow.core.NonEmptyList
import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventTypeId
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.event.EventMetadata
import io.github.kamiazya.scopes.platform.domain.event.MetadataSupport
import io.github.kamiazya.scopes.platform.domain.event.VersionSupport
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.error.AggregateIdError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle

/**
 * Events related to Scope aggregate.
 */
sealed class ScopeEvent : DomainEvent {
    abstract override val metadata: EventMetadata?
}

/**
 * Event fired when a new Scope is created.
 */
@EventTypeId("scope-management.scope.created.v1")
data class ScopeCreated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val title: ScopeTitle,
    val description: ScopeDescription?,
    val parentId: ScopeId?,
) : ScopeEvent(),
    MetadataSupport<ScopeCreated>,
    VersionSupport<ScopeCreated> {
    override fun withMetadata(metadata: EventMetadata): ScopeCreated = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeCreated = copy(aggregateVersion = version)

    companion object {
        fun from(scope: Scope, eventId: EventId): Either<AggregateIdError, ScopeCreated> = scope.id.toAggregateId().map { aggregateId ->
            ScopeCreated(
                aggregateId = aggregateId,
                eventId = eventId,
                aggregateVersion = AggregateVersion.initial().increment(),
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
@EventTypeId("scope-management.scope.title-updated.v1")
data class ScopeTitleUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val oldTitle: ScopeTitle,
    val newTitle: ScopeTitle,
) : ScopeEvent(),
    MetadataSupport<ScopeTitleUpdated>,
    VersionSupport<ScopeTitleUpdated> {
    override fun withMetadata(metadata: EventMetadata): ScopeTitleUpdated = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeTitleUpdated = copy(aggregateVersion = version)
}

/**
 * Event fired when a Scope's description is updated.
 */
@EventTypeId("scope-management.scope.description-updated.v1")
data class ScopeDescriptionUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val oldDescription: ScopeDescription?,
    val newDescription: ScopeDescription?,
) : ScopeEvent(),
    MetadataSupport<ScopeDescriptionUpdated>,
    VersionSupport<ScopeDescriptionUpdated> {
    override fun withMetadata(metadata: EventMetadata): ScopeDescriptionUpdated = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeDescriptionUpdated = copy(aggregateVersion = version)
}

/**
 * Event fired when a Scope's parent is changed.
 */
@EventTypeId("scope-management.scope.parent-changed.v1")
data class ScopeParentChanged(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val oldParentId: ScopeId?,
    val newParentId: ScopeId?,
) : ScopeEvent(),
    MetadataSupport<ScopeParentChanged>,
    VersionSupport<ScopeParentChanged> {
    override fun withMetadata(metadata: EventMetadata): ScopeParentChanged = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeParentChanged = copy(aggregateVersion = version)
}

/**
 * Event fired when a Scope is archived (soft deleted).
 */
@EventTypeId("scope-management.scope.archived.v1")
data class ScopeArchived(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val reason: String?,
) : ScopeEvent(),
    MetadataSupport<ScopeArchived>,
    VersionSupport<ScopeArchived> {
    override fun withMetadata(metadata: EventMetadata): ScopeArchived = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeArchived = copy(aggregateVersion = version)
}

/**
 * Event fired when an archived Scope is restored.
 */
@EventTypeId("scope-management.scope.restored.v1")
data class ScopeRestored(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
) : ScopeEvent(),
    MetadataSupport<ScopeRestored>,
    VersionSupport<ScopeRestored> {
    override fun withMetadata(metadata: EventMetadata): ScopeRestored = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeRestored = copy(aggregateVersion = version)
}

/**
 * Event fired when a Scope is permanently deleted.
 */
@EventTypeId("scope-management.scope.deleted.v1")
data class ScopeDeleted(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
) : ScopeEvent(),
    MetadataSupport<ScopeDeleted>,
    VersionSupport<ScopeDeleted> {
    override fun withMetadata(metadata: EventMetadata): ScopeDeleted = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeDeleted = copy(aggregateVersion = version)
}

/**
 * Event fired when an aspect is added to a scope.
 */
@EventTypeId("scope-management.scope.aspect-added.v1")
data class ScopeAspectAdded(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val aspectKey: AspectKey,
    val aspectValues: NonEmptyList<AspectValue>,
) : ScopeEvent(),
    MetadataSupport<ScopeAspectAdded>,
    VersionSupport<ScopeAspectAdded> {
    override fun withMetadata(metadata: EventMetadata): ScopeAspectAdded = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeAspectAdded = copy(aggregateVersion = version)
}

/**
 * Event fired when an aspect is removed from a scope.
 */
@EventTypeId("scope-management.scope.aspect-removed.v1")
data class ScopeAspectRemoved(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val aspectKey: AspectKey,
) : ScopeEvent(),
    MetadataSupport<ScopeAspectRemoved>,
    VersionSupport<ScopeAspectRemoved> {
    override fun withMetadata(metadata: EventMetadata): ScopeAspectRemoved = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeAspectRemoved = copy(aggregateVersion = version)
}

/**
 * Event fired when all aspects are cleared from a scope.
 */
@EventTypeId("scope-management.scope.aspects-cleared.v1")
data class ScopeAspectsCleared(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
) : ScopeEvent(),
    MetadataSupport<ScopeAspectsCleared>,
    VersionSupport<ScopeAspectsCleared> {
    override fun withMetadata(metadata: EventMetadata): ScopeAspectsCleared = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeAspectsCleared = copy(aggregateVersion = version)
}

/**
 * Event fired when aspects are updated on a scope.
 */
@EventTypeId("scope-management.scope.aspects-updated.v1")
data class ScopeAspectsUpdated(
    override val aggregateId: AggregateId,
    override val eventId: EventId,
    override val aggregateVersion: AggregateVersion,
    override val metadata: EventMetadata? = null,
    val scopeId: ScopeId,
    val oldAspects: Aspects,
    val newAspects: Aspects,
) : ScopeEvent(),
    MetadataSupport<ScopeAspectsUpdated>,
    VersionSupport<ScopeAspectsUpdated> {
    override fun withMetadata(metadata: EventMetadata): ScopeAspectsUpdated = copy(metadata = metadata)
    override fun withVersion(version: AggregateVersion): ScopeAspectsUpdated = copy(aggregateVersion = version)
}
