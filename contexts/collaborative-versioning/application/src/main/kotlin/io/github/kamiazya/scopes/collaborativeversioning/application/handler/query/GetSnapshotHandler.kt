package io.github.kamiazya.scopes.collaborativeversioning.application.handler.query

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SnapshotDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.application.query.GetSnapshotQuery
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.VersionSnapshotService
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Handler for retrieving a specific snapshot.
 *
 * This handler fetches a single snapshot and converts it to a DTO
 * for presentation layer consumption.
 */
class GetSnapshotHandler(private val snapshotService: VersionSnapshotService, private val logger: Logger) :
    QueryHandler<GetSnapshotQuery, SnapshotApplicationError, SnapshotDto?> {

    override suspend operator fun invoke(input: GetSnapshotQuery): Either<SnapshotApplicationError, SnapshotDto?> = either {
        logger.debug(
            "Processing get snapshot query",
            mapOf(
                "resourceId" to input.resourceId.toString(),
                "snapshotId" to input.snapshotId.toString(),
            ),
        )

        // Get the snapshot
        val snapshot = snapshotService.getSnapshot(
            resourceId = input.resourceId,
            snapshotId = input.snapshotId,
        ).mapLeft { domainError ->
            SnapshotApplicationError.RepositoryOperationFailed(
                operation = "getSnapshot",
                reason = "Failed to retrieve snapshot: $domainError",
            )
        }.bind()

        // Convert to DTO if found
        snapshot?.let { SnapshotDto.fromDomain(it) }
    }
}
