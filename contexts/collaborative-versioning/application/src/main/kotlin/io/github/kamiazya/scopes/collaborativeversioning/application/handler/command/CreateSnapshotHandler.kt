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

    override suspend operator fun invoke(input: CreateSnapshotCommand): Either<SnapshotApplicationError, SnapshotDto> = either {
        logger.info(
            "Processing create snapshot command",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "authorId" to input.authorId.toString(),
                "contentSize" to input.content.sizeInBytes(),
            ),
        )

        // Load the tracked resource
        val resource = trackedResourceRepository.findById(input.resourceId)
            .fold(
                { error ->
                    raise(
                        SnapshotApplicationError.RepositoryOperationFailed(
                            operation = "findById",
                            reason = "Failed to load tracked resource: $error",
                        ),
                    )
                },
                { it },
            )

        ensureNotNull(resource) {
            SnapshotApplicationError.TrackedResourceNotFound(input.resourceId)
        }

        // Create the snapshot through the service
        val snapshot = snapshotService.createSnapshot(
            resource = resource,
            content = input.content,
            authorId = input.authorId,
            message = input.message,
            metadata = input.metadata,
            timestamp = input.timestamp,
        ).fold(
            { domainError ->
                raise(
                    SnapshotApplicationError.SnapshotCreationFailed(
                        resourceId = input.resourceId,
                        reason = "Failed to create snapshot: $domainError",
                        domainError = domainError,
                    ),
                )
            },
            { it },
        )

        logger.info(
            "Snapshot created successfully",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "snapshotId" to snapshot.id.toString(),
                "versionNumber" to snapshot.versionNumber.toString(),
            ),
        )

        // Convert to DTO
        SnapshotDto.fromDomain(snapshot)
    }
}
