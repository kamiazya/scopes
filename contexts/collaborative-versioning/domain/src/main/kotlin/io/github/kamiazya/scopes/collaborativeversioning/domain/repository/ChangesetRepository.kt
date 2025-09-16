package io.github.kamiazya.scopes.collaborativeversioning.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Changeset
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ApplyChangesetError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ExistsChangesetError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.FindChangesetError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SaveChangesetError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ChangesetId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository interface for Changeset persistence operations.
 * Defined in domain layer following Dependency Inversion Principle.
 */
interface ChangesetRepository {

    /**
     * Find changeset by its identifier.
     */
    suspend fun findById(id: ChangesetId): Either<FindChangesetError, Changeset?>

    /**
     * Find all changesets for a resource.
     */
    suspend fun findByResourceId(resourceId: ResourceId): Either<FindChangesetError, Flow<Changeset>>

    /**
     * Find changesets by author (agent).
     */
    suspend fun findByAuthor(authorId: AgentId): Either<FindChangesetError, Flow<Changeset>>

    /**
     * Find changesets created within a time range.
     */
    suspend fun findByTimeRange(startTime: Instant, endTime: Instant): Either<FindChangesetError, Flow<Changeset>>

    /**
     * Find changesets that are part of a specific version.
     */
    suspend fun findByVersionId(versionId: VersionId): Either<FindChangesetError, Flow<Changeset>>

    /**
     * Save a changeset (create or update).
     */
    suspend fun save(changeset: Changeset): Either<SaveChangesetError, Changeset>

    /**
     * Apply a changeset to create a new version.
     */
    suspend fun applyChangeset(changesetId: ChangesetId, targetVersionId: VersionId): Either<ApplyChangesetError, VersionId>

    /**
     * Check if a changeset exists.
     */
    suspend fun existsById(id: ChangesetId): Either<ExistsChangesetError, Boolean>
}
