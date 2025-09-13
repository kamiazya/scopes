package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.TrackedResource
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.datetime.Instant

/**
 * Default implementation of VersionSnapshotService.
 *
 * This implementation provides comprehensive snapshot management capabilities
 * with strong error handling, validation, and performance considerations for
 * large datasets through chunked processing.
 */
class DefaultVersionSnapshotService(
    private val repository: TrackedResourceRepository,
    private val serializer: SnapshotSerializer,
    private val metadataValidator: SnapshotMetadataValidator = DefaultSnapshotMetadataValidator(),
    private val logger: Logger = ConsoleLogger("VersionSnapshotService"),
) : VersionSnapshotService {

    companion object {
        private const val MAX_SNAPSHOT_SIZE = 10_485_760L // 10MB
        private const val MAX_METADATA_KEY_LENGTH = 100
        private const val MAX_METADATA_VALUE_LENGTH = 1000
        private const val MAX_METADATA_ENTRIES = 50
        private const val BATCH_SIZE = 100
    }

    override suspend fun createSnapshot(
        resource: TrackedResource,
        content: ResourceContent,
        authorId: AgentId,
        message: String,
        metadata: Map<String, String>,
        timestamp: Instant,
    ): Either<SnapshotServiceError, Snapshot> = either {
        logger.info(
            "Creating snapshot for resource",
            mapOf(
                "resourceId" to resource.id.toString(),
                "authorId" to authorId.toString(),
                "contentSize" to content.sizeInBytes(),
            ),
        )

        // Validate the snapshot can be created
        validateSnapshot(resource, content).bind()
        validateMetadata(metadata).bind()

        // Create snapshot through the TrackedResource aggregate
        val snapshot = resource.createSnapshot(
            content = content,
            authorId = authorId,
            message = message,
            timestamp = timestamp,
        ).mapLeft { trackedResourceError ->
            SnapshotServiceError.InvalidContent(
                reason = "Failed to create snapshot: $trackedResourceError",
            )
        }.bind()

        // Add metadata to the snapshot
        val snapshotWithMetadata = snapshot.copy(
            metadata = snapshot.metadata + metadata + mapOf(
                "created_via" to "VersionSnapshotService",
                "content_size_bytes" to content.sizeInBytes().toString(),
            ),
        )

        // Save the updated resource with the new snapshot
        repository.save(resource).mapLeft { saveError ->
            SnapshotServiceError.SerializationError(
                reason = "Failed to save resource after snapshot: $saveError",
            )
        }.bind()

        logger.info(
            "Snapshot created successfully",
            mapOf(
                "resourceId" to resource.id.toString(),
                "snapshotId" to snapshotWithMetadata.id.toString(),
                "versionNumber" to snapshotWithMetadata.versionNumber.toString(),
            ),
        )

        snapshotWithMetadata
    }

    override suspend fun restoreSnapshot(
        resource: TrackedResource,
        targetSnapshotId: SnapshotId,
        authorId: AgentId,
        message: String,
        timestamp: Instant,
    ): Either<SnapshotServiceError, Snapshot> = either {
        logger.info(
            "Restoring snapshot",
            mapOf(
                "resourceId" to resource.id.toString(),
                "targetSnapshotId" to targetSnapshotId.toString(),
                "authorId" to authorId.toString(),
            ),
        )

        // Find the target snapshot
        val targetSnapshot = resource.getAllSnapshots()
            .find { it.id == targetSnapshotId }

        ensureNotNull(targetSnapshot) {
            SnapshotServiceError.SnapshotNotFound(
                resourceId = resource.id,
                snapshotId = targetSnapshotId,
            )
        }

        // Restore using the version number
        restoreToVersion(
            resource = resource,
            targetVersion = targetSnapshot.versionNumber,
            authorId = authorId,
            message = "$message (restored from snapshot $targetSnapshotId)",
            timestamp = timestamp,
        ).bind()
    }

    override suspend fun restoreToVersion(
        resource: TrackedResource,
        targetVersion: VersionNumber,
        authorId: AgentId,
        message: String,
        timestamp: Instant,
    ): Either<SnapshotServiceError, Snapshot> = either {
        logger.info(
            "Restoring to version",
            mapOf(
                "resourceId" to resource.id.toString(),
                "targetVersion" to targetVersion.toString(),
                "currentVersion" to resource.currentVersion.toString(),
                "authorId" to authorId.toString(),
            ),
        )

        // Create restoration through the TrackedResource aggregate
        val restoredSnapshot = resource.restoreToVersion(
            targetVersionNumber = targetVersion,
            authorId = authorId,
            message = message,
            timestamp = timestamp,
        ).mapLeft { trackedResourceError ->
            when (trackedResourceError) {
                is io.github.kamiazya.scopes.collaborativeversioning.domain.error.TrackedResourceError.VersionNotFound ->
                    SnapshotServiceError.VersionNotFound(
                        resourceId = resource.id,
                        versionNumber = targetVersion,
                    )
                is io.github.kamiazya.scopes.collaborativeversioning.domain.error.TrackedResourceError.InvalidRestore ->
                    SnapshotServiceError.InvalidRestoreTarget(
                        resourceId = resource.id,
                        currentVersion = resource.currentVersion,
                        targetVersion = targetVersion,
                    )
                else -> SnapshotServiceError.InvalidContent(
                    reason = "Failed to restore: $trackedResourceError",
                )
            }
        }.bind()

        // Save the updated resource
        repository.save(resource).mapLeft { saveError ->
            SnapshotServiceError.SerializationError(
                reason = "Failed to save resource after restoration: $saveError",
            )
        }.bind()

        logger.info(
            "Restoration completed successfully",
            mapOf(
                "resourceId" to resource.id.toString(),
                "newSnapshotId" to restoredSnapshot.id.toString(),
                "restoredFromVersion" to targetVersion.toString(),
                "newVersionNumber" to restoredSnapshot.versionNumber.toString(),
            ),
        )

        restoredSnapshot
    }

    override suspend fun getSnapshot(resourceId: ResourceId, snapshotId: SnapshotId): Either<SnapshotServiceError, Snapshot?> = either {
        logger.debug(
            "Getting snapshot",
            mapOf(
                "resourceId" to resourceId.toString(),
                "snapshotId" to snapshotId.toString(),
            ),
        )

        // Load the resource
        val resource = repository.findById(resourceId)
            .mapLeft { findError ->
                SnapshotServiceError.ResourceMismatch(
                    expected = resourceId,
                    actual = resourceId,
                )
            }.bind()

        // Return null if resource not found
        if (resource == null) {
            logger.debug(
                "Resource not found",
                mapOf("resourceId" to resourceId.toString()),
            )
            return@either null
        }

        // Find the snapshot in the resource
        resource.getAllSnapshots().find { it.id == snapshotId }
    }

    override suspend fun getSnapshots(resourceId: ResourceId): Either<SnapshotServiceError, List<Snapshot>> = either {
        logger.debug(
            "Getting all snapshots for resource",
            mapOf("resourceId" to resourceId.toString()),
        )

        // Load the resource
        val resource = repository.findById(resourceId)
            .mapLeft { findError ->
                SnapshotServiceError.ResourceMismatch(
                    expected = resourceId,
                    actual = resourceId,
                )
            }.bind()

        // Return empty list if resource not found
        if (resource == null) {
            logger.debug(
                "Resource not found",
                mapOf("resourceId" to resourceId.toString()),
            )
            return@either emptyList()
        }

        // Return all snapshots ordered by version
        resource.getAllSnapshots()
    }

    override fun calculateSnapshotSize(snapshot: Snapshot): Long {
        // Calculate the size including all snapshot data
        val contentSize = snapshot.content.sizeInBytes().toLong()
        val metadataSize = snapshot.metadata.entries.sumOf { (key, value) ->
            key.length.toLong() + value.length.toLong()
        }
        val overheadSize = 1024L // Estimated overhead for other fields

        return contentSize + metadataSize + overheadSize
    }

    override suspend fun validateSnapshot(resource: TrackedResource, content: ResourceContent): Either<SnapshotServiceError, Unit> = either {
        // Validate content size
        ensure(content.sizeInBytes() <= MAX_SNAPSHOT_SIZE) {
            SnapshotServiceError.StorageLimitExceeded(
                currentSize = content.sizeInBytes().toLong(),
                maxSize = MAX_SNAPSHOT_SIZE,
            )
        }

        // Validate total history size
        val currentHistorySize = resource.getHistorySizeInBytes()
        val newTotalSize = currentHistorySize + content.sizeInBytes()

        ensure(newTotalSize <= MAX_SNAPSHOT_SIZE * 100) {
            // 1GB total history limit
            SnapshotServiceError.StorageLimitExceeded(
                currentSize = newTotalSize,
                maxSize = MAX_SNAPSHOT_SIZE * 100,
            )
        }
    }

    private fun validateMetadata(metadata: Map<String, String>): Either<SnapshotServiceError, Unit> = either {
        ensure(metadata.size <= MAX_METADATA_ENTRIES) {
            SnapshotServiceError.MetadataValidationError(
                key = "",
                value = "",
                reason = "Too many metadata entries: ${metadata.size}, maximum is $MAX_METADATA_ENTRIES",
            )
        }

        metadata.forEach { (key, value) ->
            metadataValidator.validate(key, value).bind()
        }
    }
}

/**
 * Validator for snapshot metadata.
 */
interface SnapshotMetadataValidator {
    fun validate(key: String, value: String): Either<SnapshotServiceError, Unit>
}

/**
 * Default implementation of metadata validator.
 */
class DefaultSnapshotMetadataValidator : SnapshotMetadataValidator {
    companion object {
        private const val MAX_KEY_LENGTH = 100
        private const val MAX_VALUE_LENGTH = 1000
        private val VALID_KEY_PATTERN = Regex("^[a-zA-Z0-9_.-]+$")
    }

    override fun validate(key: String, value: String): Either<SnapshotServiceError, Unit> = either {
        ensure(key.isNotBlank()) {
            SnapshotServiceError.MetadataValidationError(
                key = key,
                value = value,
                reason = "Metadata key cannot be blank",
            )
        }

        ensure(key.length <= MAX_KEY_LENGTH) {
            SnapshotServiceError.MetadataValidationError(
                key = key,
                value = value,
                reason = "Metadata key too long: ${key.length}, maximum is $MAX_KEY_LENGTH",
            )
        }

        ensure(VALID_KEY_PATTERN.matches(key)) {
            SnapshotServiceError.MetadataValidationError(
                key = key,
                value = value,
                reason = "Metadata key contains invalid characters. Only alphanumeric, underscore, dot, and hyphen are allowed",
            )
        }

        ensure(value.length <= MAX_VALUE_LENGTH) {
            SnapshotServiceError.MetadataValidationError(
                key = key,
                value = value,
                reason = "Metadata value too long: ${value.length}, maximum is $MAX_VALUE_LENGTH",
            )
        }
    }
}
