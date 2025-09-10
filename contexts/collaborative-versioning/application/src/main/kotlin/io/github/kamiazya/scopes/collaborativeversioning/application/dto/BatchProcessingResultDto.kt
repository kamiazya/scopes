package io.github.kamiazya.scopes.collaborativeversioning.application.dto

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch.BatchProcessingResult
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * DTO for batch processing results.
 */
data class BatchProcessingResultDto(
    val totalRequests: Int,
    val successCount: Int,
    val failureCount: Int,
    val successRate: Double,
    val startTime: Instant,
    val endTime: Instant,
    val totalDuration: Duration,
    val successfulSnapshots: List<SnapshotDto>,
    val failures: List<SnapshotFailureDto>,
) {
    companion object {
        /**
         * Convert domain BatchProcessingResult to DTO.
         */
        fun fromDomain(result: BatchProcessingResult): BatchProcessingResultDto = BatchProcessingResultDto(
            totalRequests = result.totalRequests,
            successCount = result.successCount,
            failureCount = result.failureCount,
            successRate = result.successRate,
            startTime = result.startTime,
            endTime = result.endTime,
            totalDuration = result.totalDuration,
            successfulSnapshots = result.getSuccesses().map { success ->
                SnapshotDto.fromDomain(success.snapshot)
            },
            failures = result.getFailures().map { failure ->
                SnapshotFailureDto(
                    resourceId = failure.resourceId.toString(),
                    errorMessage = failure.error.toString(),
                    requestTime = failure.requestTime,
                    completionTime = failure.completionTime,
                )
            },
        )
    }
}

/**
 * DTO for snapshot failure information.
 */
data class SnapshotFailureDto(val resourceId: String, val errorMessage: String, val requestTime: Instant, val completionTime: Instant)
