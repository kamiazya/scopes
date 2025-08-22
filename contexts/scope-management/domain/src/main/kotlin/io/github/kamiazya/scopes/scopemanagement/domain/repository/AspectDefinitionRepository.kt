package io.github.kamiazya.scopes.scopemanagement.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Repository interface for AspectDefinition operations.
 * Manages aspect definitions that define the metadata about aspects including their types and constraints.
 */
interface AspectDefinitionRepository {
    /**
     * Save an aspect definition (create or update).
     */
    suspend fun save(definition: AspectDefinition): Either<Any, AspectDefinition>

    /**
     * Find an aspect definition by key.
     */
    suspend fun findByKey(key: AspectKey): Either<Any, AspectDefinition?>

    /**
     * Check if an aspect definition exists by key.
     */
    suspend fun existsByKey(key: AspectKey): Either<Any, Boolean>

    /**
     * Find all aspect definitions.
     */
    suspend fun findAll(): Either<Any, List<AspectDefinition>>

    /**
     * Delete an aspect definition by key.
     */
    suspend fun deleteByKey(key: AspectKey): Either<Any, Boolean>
}
