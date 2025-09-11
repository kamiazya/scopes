package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.JsonPath
import kotlinx.datetime.Instant

/**
 * Errors related to JSON diff operations.
 */
sealed class JsonDiffError : CollaborativeVersioningError() {
    data class DocumentTooLarge(val actualSize: Long, val maxSize: Long, override val occurredAt: Instant = SystemTimeProvider().now()) : JsonDiffError()

    data class InvalidJsonStructure(val reason: String, val path: String? = null, override val occurredAt: Instant = SystemTimeProvider().now()) :
        JsonDiffError()

    data class DiffCalculationFailed(val reason: String, val cause: Throwable? = null, override val occurredAt: Instant = SystemTimeProvider().now()) :
        JsonDiffError()

    data class PathNotFound(val path: JsonPath, override val occurredAt: Instant = SystemTimeProvider().now()) : JsonDiffError()

    data class ChangeSetGenerationFailed(val reason: String, override val occurredAt: Instant = SystemTimeProvider().now()) : JsonDiffError()

    data class ConflictDetectionFailed(val reason: String, val cause: Throwable? = null, override val occurredAt: Instant = SystemTimeProvider().now()) :
        JsonDiffError()

    data class MergeOperationFailed(
        val reason: String,
        val conflictPaths: List<String> = emptyList(),
        override val occurredAt: Instant = SystemTimeProvider().now(),
    ) : JsonDiffError()

    data class InvalidMergeStrategy(val strategyName: String, val reason: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        JsonDiffError()

    data class CyclicDependencyDetected(val paths: List<String>, override val occurredAt: Instant = SystemTimeProvider().now()) : JsonDiffError()

    data class MaxDiffDepthExceeded(val maxDepth: Int, val actualDepth: Int, val path: String, override val occurredAt: Instant = SystemTimeProvider().now()) :
        JsonDiffError()
}
