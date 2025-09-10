package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.RestoreSnapshotCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SnapshotDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.VersionSnapshotService
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Handler for restoring resources from a specific snapshot.
 *
 * This handler manages the restoration process, creating a new snapshot
 * with content from the target snapshot while maintaining version history.
 */
class RestoreSnapshotHandler(
    private val trackedResourceRepository: TrackedResourceRepository,
    private val snapshotService: VersionSnapshotService,
    private val logger: Logger,
) : CommandHandler<RestoreSnapshotCommand, SnapshotApplicationError, SnapshotDto> {

    override suspend operator fun invoke(input: RestoreSnapshotCommand): Either<SnapshotApplicationError, SnapshotDto> = either {
        logger.info(
            "Processing restore snapshot command",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "targetSnapshotId" to input.targetSnapshotId.toString(),
                "authorId" to input.authorId.toString(),
            ),
        )

        // Load the tracked resource
        val resource = trackedResourceRepository.findById(input.resourceId)
            .mapLeft { error ->
                SnapshotApplicationError.RepositoryOperationFailed(
                    operation = "findById",
                    reason = "Failed to load tracked resource: $error",
                )
            }.bind()

        ensureNotNull(resource) {
            SnapshotApplicationError.TrackedResourceNotFound(input.resourceId)
        }

        // Restore from the snapshot
        val restoredSnapshot = snapshotService.restoreSnapshot(
            resource = resource,
            targetSnapshotId = input.targetSnapshotId,
            authorId = input.authorId,
            message = input.message,
            timestamp = input.timestamp,
        ).mapLeft { domainError ->
            SnapshotApplicationError.SnapshotRestorationFailed(
                resourceId = input.resourceId,
                targetSnapshotId = input.targetSnapshotId,
                targetVersion = null,
                reason = "Failed to restore snapshot: $domainError",
                domainError = domainError,
            )
        }.bind()

        logger.info(
            "Snapshot restored successfully",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "targetSnapshotId" to input.targetSnapshotId.toString(),
                "newSnapshotId" to restoredSnapshot.id.toString(),
                "newVersionNumber" to restoredSnapshot.versionNumber.toString(),
            ),
        )

        // Convert to DTO
        SnapshotDto.fromDomain(restoredSnapshot)
    }
}
