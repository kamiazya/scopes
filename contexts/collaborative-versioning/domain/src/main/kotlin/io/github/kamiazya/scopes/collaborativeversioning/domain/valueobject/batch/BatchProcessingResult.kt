package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch

import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Value object representing the result of batch snapshot processing.
 */
data class BatchProcessingResult(
    val totalRequests: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<SnapshotResult>,
    val startTime: Instant,
    val endTime: Instant,
) {
    val successRate: Double = if (totalRequests > 0) {
        successCount.toDouble() / totalRequests
    } else {
        0.0
    }

    val totalDuration: Duration = endTime - startTime

    fun getSuccesses(): List<SnapshotResult.Success> = results.filterIsInstance<SnapshotResult.Success>()

    fun getFailures(): List<SnapshotResult.Failure> = results.filterIsInstance<SnapshotResult.Failure>()
}
