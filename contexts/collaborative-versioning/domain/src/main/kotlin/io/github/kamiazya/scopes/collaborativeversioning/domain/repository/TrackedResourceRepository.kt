package io.github.kamiazya.scopes.collaborativeversioning.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.DeleteTrackedResourceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ExistsTrackedResourceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.FindTrackedResourceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SaveTrackedResourceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.TrackedResource
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceType
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for TrackedResource persistence operations.
 *
 * This repository manages the storage and retrieval of version-tracked resources,
 * including their complete history of snapshots and changes. Each operation returns
 * specific error types to provide detailed failure information.
 */
interface TrackedResourceRepository {

    /**
     * Find a tracked resource by its identifier.
     *
     * @param id The resource identifier
     * @return Either an error or the tracked resource (null if not found)
     */
    suspend fun findById(id: ResourceId): Either<FindTrackedResourceError, TrackedResource?>

    /**
     * Find all tracked resources of a specific type.
     *
     * @param resourceType The type of resources to find
     * @return Either an error or a flow of tracked resources
     */
    suspend fun findByType(resourceType: ResourceType): Either<FindTrackedResourceError, Flow<TrackedResource>>

    /**
     * Find tracked resources that have been modified by a specific author.
     *
     * @param authorId The author identifier
     * @return Either an error or a flow of tracked resources
     */
    suspend fun findByAuthor(authorId: String): Either<FindTrackedResourceError, Flow<TrackedResource>>

    /**
     * Get all tracked resources in the system.
     *
     * @return Either an error or a flow of all tracked resources
     */
    suspend fun findAll(): Either<FindTrackedResourceError, Flow<TrackedResource>>

    /**
     * Save a tracked resource (create or update).
     *
     * @param resource The tracked resource to save
     * @return Either an error or the saved resource
     */
    suspend fun save(resource: TrackedResource): Either<SaveTrackedResourceError, TrackedResource>

    /**
     * Save a new snapshot for a resource.
     *
     * This is a convenience method for updating just the snapshot data
     * without loading the entire TrackedResource aggregate.
     *
     * @param resourceId The resource identifier
     * @param snapshot The snapshot to save
     * @return Either an error or the saved snapshot
     */
    suspend fun saveSnapshot(resourceId: ResourceId, snapshot: Snapshot): Either<SaveTrackedResourceError, Snapshot>

    /**
     * Delete a tracked resource and all its history.
     *
     * @param id The resource identifier
     * @return Either an error or Unit on success
     */
    suspend fun deleteById(id: ResourceId): Either<DeleteTrackedResourceError, Unit>

    /**
     * Check if a tracked resource exists.
     *
     * @param id The resource identifier
     * @return Either an error or boolean indicating existence
     */
    suspend fun existsById(id: ResourceId): Either<ExistsTrackedResourceError, Boolean>

    /**
     * Count tracked resources by type.
     *
     * @param resourceType The type of resources to count
     * @return Either an error or the count
     */
    suspend fun countByType(resourceType: ResourceType): Either<FindTrackedResourceError, Long>

    /**
     * Get the latest version number for a resource.
     *
     * This is useful for checking the current version without loading
     * the entire TrackedResource aggregate.
     *
     * @param id The resource identifier
     * @return Either an error or the latest version number (null if not found)
     */
    suspend fun getLatestVersion(id: ResourceId): Either<FindTrackedResourceError, VersionNumber?>

    /**
     * Get the total storage size used by a resource and all its history.
     *
     * @param id The resource identifier
     * @return Either an error or the size in bytes
     */
    suspend fun getStorageSize(id: ResourceId): Either<FindTrackedResourceError, Long>
}
