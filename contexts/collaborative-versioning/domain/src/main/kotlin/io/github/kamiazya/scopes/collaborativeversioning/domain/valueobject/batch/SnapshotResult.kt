package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch

import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Result of a snapshot operation.
 */
sealed class SnapshotResult {
    abstract val resourceId: ResourceId
    abstract val requestTime: Instant
    abstract val completionTime: Instant

    data class Success(override val resourceId: ResourceId, val snapshot: Snapshot, override val requestTime: Instant, override val completionTime: Instant) :
        SnapshotResult()

    data class Failure(
        override val resourceId: ResourceId,
        val error: SnapshotServiceError,
        override val requestTime: Instant,
        override val completionTime: Instant,
    ) : SnapshotResult()

    fun processingDuration(): Duration = completionTime - requestTime
}
