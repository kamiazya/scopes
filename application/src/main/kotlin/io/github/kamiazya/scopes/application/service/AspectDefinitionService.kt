package io.github.kamiazya.scopes.application.service

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.service.AspectDefinitionDefaults
import io.github.kamiazya.scopes.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.domain.error.PersistenceError
import io.github.kamiazya.scopes.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.domain.valueobject.AspectKey

/**
 * Application service for managing aspect definitions.
 * Handles default definitions and user customizations in a local-first environment.
 */
class AspectDefinitionService(
    private val repository: AspectDefinitionRepository
) {
    /**
     * Get all available aspect definitions, including defaults and user customizations.
     */
    suspend fun getAllDefinitions(): Either<PersistenceError, List<AspectDefinition>> = either {
        val userDefinitions = repository.findAll().bind()
        val defaults = getDefaultDefinitions()

        // Merge user definitions with defaults (user definitions override defaults)
        val userKeys = userDefinitions.map { it.key }.toSet()
        val finalDefinitions = userDefinitions + defaults.filter { it.key !in userKeys }

        finalDefinitions
    }

    /**
     * Get a specific aspect definition by key.
     * Checks user definitions first, then falls back to defaults.
     */
    suspend fun getDefinition(key: AspectKey): Either<PersistenceError, AspectDefinition?> = either {
        val userDefinition = repository.findByKey(key).bind()
        if (userDefinition != null) {
            userDefinition
        } else {
            getDefaultDefinitions().find { it.key == key }
        }
    }

    /**
     * Save or update a user-defined aspect definition.
     */
    suspend fun saveDefinition(definition: AspectDefinition): Either<PersistenceError, AspectDefinition> =
        repository.save(definition)

    /**
     * Delete a user-defined aspect definition.
     * Note: This only removes user customizations, default definitions cannot be deleted.
     */
    suspend fun deleteDefinition(key: AspectKey): Either<PersistenceError, Boolean> =
        repository.deleteByKey(key)

    /**
     * Reset an aspect definition to its default value.
     * This removes any user customization for the given key.
     */
    suspend fun resetToDefault(key: AspectKey): Either<PersistenceError, AspectDefinition?> = either {
        repository.deleteByKey(key).bind()
        getDefaultDefinitions().find { it.key == key }
    }

    /**
     * Check if an aspect definition exists (either user-defined or default).
     */
    suspend fun hasDefinition(key: AspectKey): Either<PersistenceError, Boolean> = either {
        val userExists = repository.existsByKey(key).bind()
        if (userExists) {
            true
        } else {
            getDefaultDefinitions().any { it.key == key }
        }
    }

    /**
     * Get all default aspect definitions provided by the system.
     */
    fun getDefaultDefinitions(): List<AspectDefinition> = AspectDefinitionDefaults.all()
}
