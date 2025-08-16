package io.github.kamiazya.scopes.domain.entity

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextName
import io.github.kamiazya.scopes.domain.valueobject.ContextFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextDescription
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Entity representing a named context view for filtering scopes.
 * Context views provide persistent, named filter definitions that can be applied
 * to scope lists to show only relevant scopes for different work contexts.
 * 
 * Business rules:
 * - Context name must be unique
 * - Filter must be valid and evaluable
 * - Description is optional but recommended for clarity
 */
data class ContextView(
    val id: ContextViewId,
    val name: ContextName,
    val filter: ContextFilter,
    val description: ContextDescription? = null,
    val createdAt: Instant,
    val updatedAt: Instant
) {
    
    companion object {
        /**
         * Create a new context view with validation.
         */
        fun create(
            name: ContextName,
            filter: ContextFilter,
            description: String? = null
        ): Either<String, ContextView> {
            // Create optional ContextDescription
            val contextDescription = when (val result = ContextDescription.createOptional(description)) {
                is Either.Left -> return result.value.left()
                is Either.Right -> result.value
            }
            
            val now = Clock.System.now()
            return ContextView(
                id = ContextViewId.generate(),
                name = name,
                filter = filter,
                description = contextDescription,
                createdAt = now,
                updatedAt = now
            ).right()
        }
    }
    
    /**
     * Update the filter for this context view.
     * Returns a new instance with updated filter and timestamp.
     */
    fun updateFilter(newFilter: ContextFilter): ContextView = copy(
        filter = newFilter,
        updatedAt = Clock.System.now()
    )
    
    /**
     * Update the description for this context view.
     * Returns Either.Left if description is too long.
     */
    fun updateDescription(newDescription: String?): Either<String, ContextView> {
        // Create optional ContextDescription
        val contextDescription = when (val result = ContextDescription.createOptional(newDescription)) {
            is Either.Left -> return result.value.left()
            is Either.Right -> result.value
        }
        
        return copy(
            description = contextDescription,
            updatedAt = Clock.System.now()
        ).right()
    }
    
    /**
     * Check if this context view matches the given name (case-insensitive).
     */
    fun matchesName(searchName: String): Boolean = 
        name.normalized() == searchName.lowercase()
}