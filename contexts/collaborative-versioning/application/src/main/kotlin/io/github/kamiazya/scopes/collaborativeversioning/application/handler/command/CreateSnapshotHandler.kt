package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.CreateSnapshotCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SnapshotDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.VersionSnapshotService
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Handler for creating snapshots of tracked resources.
 *
 * This handler orchestrates the snapshot creation process, ensuring
 * proper validation, error handling, and transaction boundaries.
 */
class CreateSnapshotHandler(
    private val trackedResourceRepository: TrackedResourceRepository,
    private val snapshotService: VersionSnapshotService,
    private val logger: Logger,
) : CommandHandler<CreateSnapshotCommand, SnapshotApplicationError, SnapshotDto> {

    override suspend fun handle(command: CreateSnapshotCommand): Either<SnapshotApplicationError, SnapshotDto> = either {
        logger.info(
            "Processing create snapshot command",
            mapOf(
                "resourceId" to command.resourceId.toString(),
                "authorId" to command.authorId.toString(),
                "contentSize" to command.content.sizeInBytes(),
            ),
        )

        // Load the tracked resource
        val resource = trackedResourceRepository.findById(command.resourceId)
            .mapLeft { error ->
                SnapshotApplicationError.RepositoryOperationFailed(
                    operation = "findById",
                    reason = "Failed to load tracked resource: $error",
                )
            }.bind()

        ensureNotNull(resource) {
            SnapshotApplicationError.TrackedResourceNotFound(command.resourceId)
        }

        // Create the snapshot through the service
        val snapshot = snapshotService.createSnapshot(
            resource = resource,
            content = command.content,
            authorId = command.authorId,
            message = command.message,
            metadata = command.metadata,
            timestamp = command.timestamp,
        ).mapLeft { domainError ->
            SnapshotApplicationError.SnapshotCreationFailed(
                resourceId = command.resourceId,
                reason = "Failed to create snapshot: $domainError",
                domainError = domainError,
            )
        }.bind()

        logger.info(
            "Snapshot created successfully",
            mapOf(
                "resourceId" to command.resourceId.toString(),
                "snapshotId" to snapshot.id.toString(),
                "versionNumber" to snapshot.versionNumber.toString(),
            ),
        )

        // Convert to DTO
        SnapshotDto.fromDomain(snapshot)
    }
}
