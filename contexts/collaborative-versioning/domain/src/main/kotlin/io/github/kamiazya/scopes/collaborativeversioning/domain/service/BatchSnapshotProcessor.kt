package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceType
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch.BatchProcessingOptions
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch.BatchProcessingResult
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch.SnapshotRequest
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.batch.SnapshotResult
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock

/**
 * Service for batch processing of snapshots.
 *
 * This service provides functionality to process multiple snapshots
 * efficiently, with support for chunking, parallel processing, and
 * comprehensive error handling.
 */
interface BatchSnapshotProcessor {
    /**
     * Process multiple snapshots in batch.
     *
     * @param requests The batch of snapshot requests
     * @param options Processing options
     * @return Results for each request
     */
    suspend fun processBatch(requests: List<SnapshotRequest>, options: BatchProcessingOptions = BatchProcessingOptions()): BatchProcessingResult

    /**
     * Process snapshots as a stream.
     *
     * @param requestFlow Flow of snapshot requests
     * @param options Processing options
     * @return Flow of processing results
     */
    suspend fun processStream(requestFlow: Flow<SnapshotRequest>, options: BatchProcessingOptions = BatchProcessingOptions()): Flow<SnapshotResult>

    /**
     * Create snapshots for all resources of a specific type.
     *
     * @param resourceType The type of resources to snapshot
     * @param authorId The author creating snapshots
     * @param message Message for all snapshots
     * @param options Processing options
     * @return Batch processing result
     */
    suspend fun snapshotAllByType(
        resourceType: ResourceType,
        authorId: AgentId,
        message: String,
        options: BatchProcessingOptions = BatchProcessingOptions(),
    ): BatchProcessingResult
}

/**
 * Default implementation of BatchSnapshotProcessor.
 */
class DefaultBatchSnapshotProcessor(
    private val repository: TrackedResourceRepository,
    private val snapshotService: VersionSnapshotService,
    private val logger: Logger = ConsoleLogger("BatchSnapshotProcessor"),
) : BatchSnapshotProcessor {

    override suspend fun processBatch(requests: List<SnapshotRequest>, options: BatchProcessingOptions): BatchProcessingResult = coroutineScope {
        val startTime = Clock.System.now()
        logger.info(
            "Starting batch processing",
            mapOf(
                "totalRequests" to requests.size,
                "chunkSize" to options.chunkSize,
                "parallelism" to options.parallelism,
            ),
        )

        val results = mutableListOf<SnapshotResult>()
        val chunks = requests.chunked(options.chunkSize)

        // Process chunks with controlled parallelism
        val semaphore = Semaphore(options.parallelism)

        chunks.forEach { chunk ->
            val chunkResults = chunk.map { request ->
                async {
                    semaphore.withPermit {
                        processWithRetry(request, options)
                    }
                }
            }.awaitAll()

            results.addAll(chunkResults)

            // Log progress
            logger.info(
                "Chunk processed",
                mapOf(
                    "processedSoFar" to results.size,
                    "totalRequests" to requests.size,
                ),
            )
        }

        val endTime = Clock.System.now()
        val successCount = results.count { it is SnapshotResult.Success }
        val failureCount = results.count { it is SnapshotResult.Failure }

        logger.info(
            "Batch processing completed",
            mapOf(
                "totalRequests" to requests.size,
                "successCount" to successCount,
                "failureCount" to failureCount,
                "duration" to (endTime - startTime).toString(),
            ),
        )

        BatchProcessingResult(
            totalRequests = requests.size,
            successCount = successCount,
            failureCount = failureCount,
            results = results,
            startTime = startTime,
            endTime = endTime,
        )
    }

    override suspend fun processStream(requestFlow: Flow<SnapshotRequest>, options: BatchProcessingOptions): Flow<SnapshotResult> = flow {
        val semaphore = Semaphore(options.parallelism)

        requestFlow
            .buffer(options.chunkSize)
            .collect { request ->
                semaphore.withPermit {
                    val result = processWithRetry(request, options)
                    emit(result)
                }
            }
    }

    override suspend fun snapshotAllByType(
        resourceType: ResourceType,
        authorId: AgentId,
        message: String,
        options: BatchProcessingOptions,
    ): BatchProcessingResult {
        val startTime = Clock.System.now()

        return either<SnapshotServiceError, BatchProcessingResult> {
            logger.info(
                "Creating snapshots for all resources of type",
                mapOf(
                    "resourceType" to resourceType.toString(),
                    "authorId" to authorId.toString(),
                ),
            )

            // Get all resources of the specified type
            val resourcesFlow = repository.findByType(resourceType)
                .mapLeft { error ->
                    SnapshotServiceError.SerializationError(
                        reason = "Failed to find resources: $error",
                    )
                }.bind()

            // Convert resources to snapshot requests
            val requests = mutableListOf<SnapshotRequest>()
            resourcesFlow.collect { resource ->
                resource.getCurrentSnapshot()?.let { currentSnapshot ->
                    requests.add(
                        SnapshotRequest(
                            resourceId = resource.id,
                            content = currentSnapshot.content,
                            authorId = authorId,
                            message = "$message (batch snapshot of $resourceType)",
                            metadata = mapOf(
                                "batch_operation" to "snapshot_all_by_type",
                                "resource_type" to resourceType.toString(),
                            ),
                        ),
                    )
                }
            }

            // Process the batch
            processBatch(requests, options)
        }.fold(
            ifLeft = { error ->
                BatchProcessingResult(
                    totalRequests = 0,
                    successCount = 0,
                    failureCount = 1,
                    results = emptyList(),
                    startTime = startTime,
                    endTime = Clock.System.now(),
                )
            },
            ifRight = { it },
        )
    }

    private suspend fun processWithRetry(request: SnapshotRequest, options: BatchProcessingOptions): SnapshotResult {
        val requestTime = Clock.System.now()
        var lastError: SnapshotServiceError? = null
        var attempt = 0

        while (attempt < options.maxRetries) {
            val result = processSingleSnapshot(request)

            when (result) {
                is Either.Right -> {
                    return SnapshotResult.Success(
                        resourceId = request.resourceId,
                        snapshot = result.value,
                        requestTime = requestTime,
                        completionTime = Clock.System.now(),
                    )
                }
                is Either.Left -> {
                    lastError = result.value
                    attempt++

                    if (attempt < options.maxRetries) {
                        logger.warn(
                            "Snapshot failed, retrying",
                            mapOf(
                                "resourceId" to request.resourceId.toString(),
                                "attempt" to attempt,
                                "error" to lastError.toString(),
                            ),
                        )
                        delay(options.retryDelay)
                    }
                }
            }
        }

        // All retries exhausted
        return SnapshotResult.Failure(
            resourceId = request.resourceId,
            error = lastError ?: SnapshotServiceError.InvalidContent(
                reason = "Failed after ${options.maxRetries} attempts",
            ),
            requestTime = requestTime,
            completionTime = Clock.System.now(),
        )
    }

    private suspend fun processSingleSnapshot(request: SnapshotRequest): Either<SnapshotServiceError, Snapshot> = either {
        // Load the resource
        val resource = repository.findById(request.resourceId)
            .mapLeft { error ->
                SnapshotServiceError.ResourceMismatch(
                    expected = request.resourceId,
                    actual = request.resourceId,
                )
            }.bind()

        ensure(resource != null) {
            SnapshotServiceError.ResourceMismatch(
                expected = request.resourceId,
                actual = request.resourceId,
            )
        }

        // Create the snapshot
        snapshotService.createSnapshot(
            resource = resource,
            content = request.content,
            authorId = request.authorId,
            message = request.message,
            metadata = request.metadata,
            timestamp = Clock.System.now(),
        ).bind()
    }
}

/**
 * Extension function to process snapshots in parallel batches.
 */
suspend fun Flow<SnapshotRequest>.processBatchedSnapshots(
    processor: BatchSnapshotProcessor,
    options: BatchProcessingOptions = BatchProcessingOptions(),
): Flow<SnapshotResult> = processor.processStream(this, options)
