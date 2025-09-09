package io.github.kamiazya.scopes.collaborativeversioning.domain.event

import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.ResourceChangeType
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import kotlinx.datetime.Instant

/**
 * Base class for all TrackedResource domain events.
 */
sealed class TrackedResourceEvent : DomainEvent {
    abstract val resourceId: ResourceId
    abstract val resourceType: ResourceType
    abstract override val occurredAt: Instant
}

/**
 * Event raised when a new tracked resource is created.
 */
data class TrackedResourceCreated(
    override val resourceId: ResourceId,
    override val resourceType: ResourceType,
    val initialVersionId: VersionId,
    val authorId: AgentId,
    val message: String,
    override val occurredAt: Instant,
) : TrackedResourceEvent()

/**
 * Event raised when a new snapshot is created for a tracked resource.
 */
data class SnapshotCreated(
    override val resourceId: ResourceId,
    override val resourceType: ResourceType,
    val snapshotId: SnapshotId,
    val versionId: VersionId,
    val versionNumber: VersionNumber,
    val previousVersionId: VersionId,
    val previousVersionNumber: VersionNumber,
    val authorId: AgentId,
    val message: String,
    val contentSize: Int,
    override val occurredAt: Instant,
) : TrackedResourceEvent()

/**
 * Event raised when a resource is restored to a previous version.
 */
data class ResourceRestored(
    override val resourceId: ResourceId,
    override val resourceType: ResourceType,
    val restoredToVersion: VersionNumber,
    val newVersionId: VersionId,
    val newVersionNumber: VersionNumber,
    val authorId: AgentId,
    val message: String,
    override val occurredAt: Instant,
) : TrackedResourceEvent()

/**
 * Event raised when a change is recorded for a tracked resource.
 */
data class ResourceChangeRecorded(
    override val resourceId: ResourceId,
    override val resourceType: ResourceType,
    val changeId: String,
    val fromVersionId: VersionId?,
    val toVersionId: VersionId,
    val fromVersionNumber: VersionNumber?,
    val toVersionNumber: VersionNumber,
    val changeType: ResourceChangeType,
    val authorId: AgentId,
    val message: String,
    override val occurredAt: Instant,
) : TrackedResourceEvent()

/**
 * Event raised when versions are merged.
 */
data class VersionsMerged(
    override val resourceId: ResourceId,
    override val resourceType: ResourceType,
    val baseVersionId: VersionId,
    val sourceVersionId: VersionId,
    val targetVersionId: VersionId,
    val mergedVersionId: VersionId,
    val mergedVersionNumber: VersionNumber,
    val authorId: AgentId,
    val message: String,
    override val occurredAt: Instant,
) : TrackedResourceEvent()

/**
 * Event raised when a tracked resource is deleted.
 */
data class TrackedResourceDeleted(
    override val resourceId: ResourceId,
    override val resourceType: ResourceType,
    val finalVersionNumber: VersionNumber,
    val deletedBy: AgentId,
    val reason: String,
    override val occurredAt: Instant,
) : TrackedResourceEvent()
