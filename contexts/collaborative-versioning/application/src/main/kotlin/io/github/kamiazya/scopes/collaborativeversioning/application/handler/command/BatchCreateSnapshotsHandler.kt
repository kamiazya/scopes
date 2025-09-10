package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.command.BatchCreateSnapshotsCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.BatchProcessingResultDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.BatchSnapshotProcessor
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Handler for batch creation of snapshots.
 *
 * This handler manages the bulk creation of snapshots for multiple resources,
 * providing efficient processing with parallel execution and error handling.
 */
class BatchCreateSnapshotsHandler(private val batchProcessor: BatchSnapshotProcessor, private val logger: Logger) :
    CommandHandler<BatchCreateSnapshotsCommand, SnapshotApplicationError, BatchProcessingResultDto> {

    override suspend fun handle(command: BatchCreateSnapshotsCommand): Either<SnapshotApplicationError, BatchProcessingResultDto> = either {
        logger.info(
            "Processing batch create snapshots command",
            mapOf(
                "resourceType" to command.resourceType.toString(),
                "authorId" to command.authorId.toString(),
                "processingOptions" to mapOf(
                    "chunkSize" to command.processingOptions.chunkSize,
                    "parallelism" to command.processingOptions.parallelism,
                ),
            ),
        )

        // Process all resources of the specified type
        val result = batchProcessor.snapshotAllByType(
            resourceType = command.resourceType,
            authorId = command.authorId,
            message = command.message,
            options = command.processingOptions,
        )

        // Check if there were any failures
        if (result.failureCount > 0 && !command.processingOptions.continueOnError) {
            raise(
                SnapshotApplicationError.BatchProcessingFailed(
                    processedCount = result.successCount,
                    failedCount = result.failureCount,
                    reason = "Batch processing failed with ${result.failureCount} failures",
                ),
            )
        }

        logger.info(
            "Batch snapshot processing completed",
            mapOf(
                "totalRequests" to result.totalRequests,
                "successCount" to result.successCount,
                "failureCount" to result.failureCount,
                "successRate" to result.successRate,
                "duration" to result.totalDuration.toString(),
            ),
        )

        // Convert to DTO
        BatchProcessingResultDto.fromDomain(result)
    }
}
