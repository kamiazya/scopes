package io.github.kamiazya.scopes.collaborativeversioning.application.handler.query

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.collaborativeversioning.application.dto.SnapshotDiffDto
import io.github.kamiazya.scopes.collaborativeversioning.application.error.SnapshotApplicationError
import io.github.kamiazya.scopes.collaborativeversioning.application.query.GetSnapshotDiffQuery
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SnapshotDiffer
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.VersionSnapshotService
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.platform.observability.logging.Logger

/**
 * Handler for calculating differences between snapshots.
 *
 * This handler computes the changes between two snapshots and
 * returns a detailed diff for presentation layer consumption.
 */
class GetSnapshotDiffHandler(private val snapshotService: VersionSnapshotService, private val snapshotDiffer: SnapshotDiffer, private val logger: Logger) :
    QueryHandler<GetSnapshotDiffQuery, SnapshotApplicationError, SnapshotDiffDto> {

    override suspend fun handle(query: GetSnapshotDiffQuery): Either<SnapshotApplicationError, SnapshotDiffDto> = either {
        logger.debug(
            "Processing get snapshot diff query",
            mapOf(
                "resourceId" to query.resourceId.toString(),
                "fromSnapshotId" to query.fromSnapshotId.toString(),
                "toSnapshotId" to query.toSnapshotId.toString(),
            ),
        )

        // Get the "from" snapshot
        val fromSnapshot = snapshotService.getSnapshot(
            resourceId = query.resourceId,
            snapshotId = query.fromSnapshotId,
        ).mapLeft { domainError ->
            SnapshotApplicationError.RepositoryOperationFailed(
                operation = "getSnapshot",
                reason = "Failed to retrieve from snapshot: $domainError",
            )
        }.bind()

        ensureNotNull(fromSnapshot) {
            SnapshotApplicationError.SnapshotRestorationFailed(
                resourceId = query.resourceId,
                targetSnapshotId = query.fromSnapshotId,
                targetVersion = null,
                reason = "From snapshot not found",
                domainError = SnapshotServiceError.SnapshotNotFound(
                    resourceId = query.resourceId,
                    snapshotId = query.fromSnapshotId,
                ),
            )
        }

        // Get the "to" snapshot
        val toSnapshot = snapshotService.getSnapshot(
            resourceId = query.resourceId,
            snapshotId = query.toSnapshotId,
        ).mapLeft { domainError ->
            SnapshotApplicationError.RepositoryOperationFailed(
                operation = "getSnapshot",
                reason = "Failed to retrieve to snapshot: $domainError",
            )
        }.bind()

        ensureNotNull(toSnapshot) {
            SnapshotApplicationError.SnapshotRestorationFailed(
                resourceId = query.resourceId,
                targetSnapshotId = query.toSnapshotId,
                targetVersion = null,
                reason = "To snapshot not found",
                domainError = SnapshotServiceError.SnapshotNotFound(
                    resourceId = query.resourceId,
                    snapshotId = query.toSnapshotId,
                ),
            )
        }

        // Calculate the diff
        val diff = snapshotDiffer.calculateDiff(
            fromSnapshot = fromSnapshot,
            toSnapshot = toSnapshot,
        ).mapLeft { domainError ->
            SnapshotApplicationError.RepositoryOperationFailed(
                operation = "calculateDiff",
                reason = "Failed to calculate snapshot diff: $domainError",
            )
        }.bind()

        logger.debug(
            "Calculated snapshot diff",
            mapOf(
                "resourceId" to query.resourceId.toString(),
                "hasChanges" to diff.hasChanges(),
                "changeCount" to diff.contentDiff.changeCount(),
            ),
        )

        // Convert to DTO
        SnapshotDiffDto.fromDomain(diff)
    }
}
