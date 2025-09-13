package io.github.kamiazya.scopes.collaborativeversioning.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.command.RestoreToVersionCommand
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SnapshotDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.domain.repository.TrackedResourceRepository
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.VersionSnapshotService
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Handler for restoring resources to a specific version number.
 *
 * This handler manages the restoration process to a specific version,
 * creating a new snapshot with content from that version.
 */
class RestoreToVersionHandler(
    private val trackedResourceRepository: TrackedResourceRepository,
    private val snapshotService: VersionSnapshotService,
    private val logger: Logger,
) : CommandHandler<RestoreToVersionCommand, SnapshotApplicationError, SnapshotDto> {

    override suspend operator fun invoke(input: RestoreToVersionCommand): Either<SnapshotApplicationError, SnapshotDto> = either {
        logger.info(
            "Processing restore to version command",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "targetVersion" to input.targetVersion.toString(),
                "authorId" to input.authorId.toString(),
            ),
        )

        // Load the tracked resource
        val resource = trackedResourceRepository.findById(input.resourceId)
            .fold(
                { error ->
                    logger.error(
                        "Failed to load tracked resource",
                        mapOf(
                            "resourceId" to input.resourceId.toString(),
                            "error" to error.toString(),
                        ),
                    )
                    raise(SnapshotApplicationError.TrackedResourceNotFound(input.resourceId))
                },
                { it },
            )

        ensureNotNull(resource) {
            SnapshotApplicationError.TrackedResourceNotFound(input.resourceId)
        }

        // Restore to the specific version
        val restoredSnapshot = snapshotService.restoreToVersion(
            resource = resource,
            targetVersion = input.targetVersion,
            authorId = input.authorId,
            message = input.message,
            timestamp = input.timestamp,
        ).mapLeft { domainError ->
            SnapshotApplicationError.SnapshotRestorationFailed(
                resourceId = input.resourceId,
                targetSnapshotId = null,
                targetVersion = input.targetVersion,
                reason = "Failed to restore to version: $domainError",
                domainError = domainError,
            )
        }.bind()

        logger.info(
            "Resource restored to version successfully",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "targetVersion" to input.targetVersion.toString(),
                "newSnapshotId" to restoredSnapshot.id.toString(),
                "newVersionNumber" to restoredSnapshot.versionNumber.toString(),
            ),
        )

        // Convert to DTO
        SnapshotDto.fromDomain(restoredSnapshot)
    }
}
