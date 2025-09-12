package io.github.kamiazya.scopes.collaborativeversioning.application.handler.query

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SnapshotDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.application.query.GetSnapshotsQuery
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.VersionSnapshotService
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Handler for retrieving all snapshots for a resource.
 *
 * This handler fetches all snapshots for a given resource and
 * converts them to DTOs for presentation layer consumption.
 */
class GetSnapshotsHandler(private val snapshotService: VersionSnapshotService, private val logger: Logger) :
    QueryHandler<GetSnapshotsQuery, SnapshotApplicationError, List<SnapshotDto>> {

    override suspend operator fun invoke(input: GetSnapshotsQuery): Either<SnapshotApplicationError, List<SnapshotDto>> = either {
        logger.debug(
            "Processing get snapshots query",
            mapOf(
                "resourceId" to input.resourceId.toString(),
            ),
        )

        // Get all snapshots for the resource
        val snapshots = snapshotService.getSnapshots(input.resourceId)
            .fold(
                { domainError ->
                    logger.error(
                        "Failed to retrieve snapshots",
                        mapOf(
                            "resourceId" to input.resourceId.toString(),
                            "error" to domainError.toString(),
                        ),
                    )
                    raise(
                        SnapshotApplicationError.SnapshotCreationFailed(
                            resourceId = input.resourceId,
                            reason = "Failed to retrieve snapshots: $domainError",
                            domainError = domainError,
                        ),
                    )
                },
                { it },
            )

        logger.debug(
            "Retrieved snapshots",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "snapshotCount" to snapshots.size,
            ),
        )

        // Convert to DTOs
        snapshots.map { SnapshotDto.fromDomain(it) }
    }
}
