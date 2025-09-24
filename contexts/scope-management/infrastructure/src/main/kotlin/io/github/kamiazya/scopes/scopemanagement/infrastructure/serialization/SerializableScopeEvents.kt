package io.github.kamiazya.scopes.scopemanagement.infrastructure.serialization

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable wrapper classes for domain events.
 *
 * These classes exist in the infrastructure layer to provide serialization support
 * while keeping the domain layer free from framework dependencies.
 */

@Serializable
@SerialName("scope-management.scope.created.v1")
data class SerializableScopeCreated(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val title: String,
    val description: String?,
    val parentId: String?,
)

@Serializable
@SerialName("scope-management.scope.deleted.v1")
data class SerializableScopeDeleted(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
)

@Serializable
@SerialName("scope-management.scope.archived.v1")
data class SerializableScopeArchived(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val reason: String?,
)

@Serializable
@SerialName("scope-management.scope.restored.v1")
data class SerializableScopeRestored(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
)

@Serializable
@SerialName("scope-management.scope.title-updated.v1")
data class SerializableScopeTitleUpdated(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val oldTitle: String,
    val newTitle: String,
)

@Serializable
@SerialName("scope-management.scope.description-updated.v1")
data class SerializableScopeDescriptionUpdated(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val oldDescription: String?,
    val newDescription: String?,
)

@Serializable
@SerialName("scope-management.scope.parent-changed.v1")
data class SerializableScopeParentChanged(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val oldParentId: String?,
    val newParentId: String?,
)

@Serializable
@SerialName("scope-management.scope.aspect-added.v1")
data class SerializableScopeAspectAdded(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val aspectKey: String,
    val aspectValues: List<String>,
)

@Serializable
@SerialName("scope-management.scope.aspect-removed.v1")
data class SerializableScopeAspectRemoved(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val aspectKey: String,
)

@Serializable
@SerialName("scope-management.scope.aspects-cleared.v1")
data class SerializableScopeAspectsCleared(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
)

@Serializable
@SerialName("scope-management.scope.aspects-updated.v1")
data class SerializableScopeAspectsUpdated(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val oldAspects: Map<String, List<String>>,
    val newAspects: Map<String, List<String>>,
)

// Alias events

@Serializable
@SerialName("scope-management.alias.assigned.v1")
data class SerializableAliasAssigned(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val aliasId: String,
    val aliasName: String,
    val scopeId: String,
    val aliasType: String,
)

@Serializable
@SerialName("scope-management.alias.removed.v1")
data class SerializableAliasRemoved(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val aliasId: String,
    val aliasName: String,
    val scopeId: String,
    val aliasType: String,
    val removedAt: Instant,
)

@Serializable
@SerialName("scope-management.alias.name-changed.v1")
data class SerializableAliasNameChanged(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val aliasId: String,
    val scopeId: String,
    val oldAliasName: String,
    val newAliasName: String,
)

@Serializable
@SerialName("scope-management.alias.canonical-replaced.v1")
data class SerializableCanonicalAliasReplaced(
    val aggregateId: String,
    val eventId: String,
    val occurredAt: Instant,
    val aggregateVersion: Long,
    val metadata: SerializableEventMetadata? = null,
    val scopeId: String,
    val oldAliasId: String,
    val oldAliasName: String,
    val newAliasId: String,
    val newAliasName: String,
)

@Serializable
data class SerializableEventMetadata(
    val correlationId: String? = null,
    val causationId: String? = null,
    val userId: String? = null,
    val timestamp: Instant? = null,
    val additionalData: Map<String, String> = emptyMap(),
)
