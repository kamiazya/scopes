package io.github.kamiazya.scopes.collaborativeversioning.domain.error

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.JsonPath

/**
 * Errors related to JSON diff operations.
 */
sealed class JsonDiffError : CollaborativeVersioningError() {
    data class DocumentTooLarge(val actualSize: Long, val maxSize: Long) : JsonDiffError()

    data class InvalidJsonStructure(val reason: String, val path: String? = null) : JsonDiffError()

    data class DiffCalculationFailed(val reason: String, val cause: Throwable? = null) : JsonDiffError()

    data class PathNotFound(val path: JsonPath) : JsonDiffError()

    data class ChangeSetGenerationFailed(val reason: String) : JsonDiffError()

    data class ConflictDetectionFailed(val reason: String, val cause: Throwable? = null) : JsonDiffError()

    data class MergeOperationFailed(val reason: String, val conflictPaths: List<String> = emptyList()) : JsonDiffError()

    data class InvalidMergeStrategy(val strategyName: String, val reason: String) : JsonDiffError()

    data class CyclicDependencyDetected(val paths: List<String>) : JsonDiffError()

    data class MaxDiffDepthExceeded(val maxDepth: Int, val actualDepth: Int, val path: String) : JsonDiffError()
}
