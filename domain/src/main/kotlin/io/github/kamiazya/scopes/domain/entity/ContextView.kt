package io.github.kamiazya.scopes.domain.entity

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.domain.valueobject.ContextViewName
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Entity representing a named context view for filtering scopes.
 * Context views provide persistent, named filter definitions that can be applied
 * to scope lists to show only relevant scopes for different work contexts.
 *
 * Business rules:
 * - Context key must be unique (used for programmatic access)
 * - Context name is for display purposes and can contain spaces
 * - Filter must be valid and evaluable
 * - Description is optional but recommended for clarity
 */
data class ContextView(
    val id: ContextViewId,
    val key: ContextViewKey,
    val name: ContextViewName,
    val filter: ContextViewFilter,
    val description: ContextViewDescription? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {

    companion object {
        /**
         * Create a new context view with validation.
         */
        fun create(
            key: ContextViewKey,
            name: ContextViewName,
            filter: ContextViewFilter,
            description: String? = null,
        ): Either<String, ContextView> {
            // Create optional ContextViewDescription
            val contextDescription = if (description.isNullOrBlank()) {
                null
            } else {
                when (val result = ContextViewDescription.create(description)) {
                    is Either.Left -> return result.value.left()
                    is Either.Right -> result.value
                }
            }

            val now = Clock.System.now()
            return ContextView(
                id = ContextViewId.generate(),
                key = key,
                name = name,
                filter = filter,
                description = contextDescription,
                createdAt = now,
                updatedAt = now,
            ).right()
        }
    }

    /**
     * Update the filter for this context view.
     * Returns a new instance with updated filter and timestamp.
     */
    fun updateFilter(newFilter: ContextViewFilter): ContextView = copy(
        filter = newFilter,
        updatedAt = Clock.System.now(),
    )

    /**
     * Update the description for this context view.
     * Returns Either.Left if description is too long.
     */
    fun updateDescription(newDescription: String?): Either<String, ContextView> {
        // Create optional ContextViewDescription
        val contextDescription = if (newDescription.isNullOrBlank()) {
            null
        } else {
            when (val result = ContextViewDescription.create(newDescription)) {
                is Either.Left -> return result.value.left()
                is Either.Right -> result.value
            }
        }

        return copy(
            description = contextDescription,
            updatedAt = Clock.System.now(),
        ).right()
    }

    /**
     * Update the name for this context view.
     * Returns a new instance with updated name and timestamp.
     */
    fun updateName(newName: ContextViewName): ContextView = copy(
        name = newName,
        updatedAt = Clock.System.now(),
    )

    /**
     * Check if this context view matches the given key.
     */
    fun matchesKey(searchKey: String): Boolean = key.value == searchKey
}
