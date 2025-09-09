package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Error types for find operations.
 */
sealed class FindChangesetError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : FindChangesetError()
    data class IndexCorruption(val changesetId: ChangesetId, val message: String, override val occurredAt: Instant = Clock.System.now()) : FindChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : FindChangesetError()
}

/**
 * Error types for save operations.
 */
sealed class SaveChangesetError : CollaborativeVersioningError() {
    data class ConcurrentModification(
        val changesetId: ChangesetId,
        val expectedVersion: Int,
        val actualVersion: Int,
        override val occurredAt: Instant = Clock.System.now(),
    ) : SaveChangesetError()
    data class ValidationFailed(val violations: List<String>, override val occurredAt: Instant = Clock.System.now()) : SaveChangesetError()
    data class StorageQuotaExceeded(val currentSize: Long, val maxSize: Long, override val occurredAt: Instant = Clock.System.now()) : SaveChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : SaveChangesetError()
}

/**
 * Error types for apply operations.
 */
sealed class ApplyChangesetError : CollaborativeVersioningError() {
    data class TargetVersionNotFound(val versionId: VersionId, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
    data class IncompatibleChangeset(val reason: String, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
    data class ConflictDetected(val conflicts: List<String>, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : ApplyChangesetError()
}

/**
 * Error types for exists operations.
 */
sealed class ExistsChangesetError : CollaborativeVersioningError() {
    data class QueryTimeout(val operation: String, val timeoutMs: Long, override val occurredAt: Instant = Clock.System.now()) : ExistsChangesetError()
    data class NetworkError(val message: String, val cause: Throwable?, override val occurredAt: Instant = Clock.System.now()) : ExistsChangesetError()
}
