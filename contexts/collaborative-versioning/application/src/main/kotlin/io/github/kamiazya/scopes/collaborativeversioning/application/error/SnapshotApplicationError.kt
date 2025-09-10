package io.github.kamiazya.scopes.collaborativeversioning.application.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.TrackedResourceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import io.github.kamiazya.scopes.platform.application.error.ApplicationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Application-level errors for snapshot operations.
 *
 * These errors provide context-specific information for snapshot-related
 * failures in the application layer.
 */
sealed class SnapshotApplicationError : ApplicationError {

    /**
     * Error when a tracked resource cannot be found.
     */
    data class TrackedResourceNotFound(
        val resourceId: ResourceId,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : SnapshotApplicationError()

    /**
     * Error when snapshot creation fails.
     */
    data class SnapshotCreationFailed(
        val resourceId: ResourceId,
        val reason: String,
        val domainError: SnapshotServiceError,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : SnapshotApplicationError()

    /**
     * Error when snapshot restoration fails.
     */
    data class SnapshotRestorationFailed(
        val resourceId: ResourceId,
        val targetSnapshotId: SnapshotId?,
        val targetVersion: VersionNumber?,
        val reason: String,
        val domainError: SnapshotServiceError,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : SnapshotApplicationError()

    /**
     * Error when batch processing fails.
     */
    data class BatchProcessingFailed(
        val processedCount: Int,
        val failedCount: Int,
        val reason: String,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : SnapshotApplicationError()

    /**
     * Error when validation fails.
     */
    data class ValidationFailed(
        val field: String,
        val reason: String,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : SnapshotApplicationError()

    /**
     * Error when repository operation fails.
     */
    data class RepositoryOperationFailed(
        val operation: String,
        val reason: String,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : SnapshotApplicationError()

    /**
     * Error when a domain rule is violated.
     */
    data class DomainRuleViolation(
        val rule: String,
        val domainError: TrackedResourceError,
        override val occurredAt: Instant = Clock.System.now(),
        override val cause: Throwable? = null,
    ) : SnapshotApplicationError()
}
