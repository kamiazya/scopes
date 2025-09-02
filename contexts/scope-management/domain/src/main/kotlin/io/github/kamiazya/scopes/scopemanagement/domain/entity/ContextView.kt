package io.github.kamiazya.scopes.scopemanagement.domain.entity

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.service.FilterEvaluationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.Aspects
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewDescription
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewFilter
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ContextViewName
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
        fun create(key: ContextViewKey, name: ContextViewName, filter: ContextViewFilter, description: String? = null): Either<ContextError, ContextView> {
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
    fun updateDescription(newDescription: String?): Either<ContextError, ContextView> {
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
     * Check if this context view matches a given key.
     */
    fun hasKey(key: String): Boolean = this.key.value == key

    /**
     * Check if this context view matches a given name.
     */
    fun hasName(name: String): Boolean = this.name.value == name

    /**
     * Evaluate if a scope matches this context view's filter.
     * This method encapsulates the filter evaluation logic in the domain entity,
     * making ContextView a richer domain model.
     *
     * @param scope The scope to evaluate
     * @param aspectDefinitions Map of aspect definitions for type-aware comparison
     * @param filterEvaluationService The domain service for filter evaluation
     * @return Either an error or boolean indicating if the scope matches
     */
    fun evaluateScope(
        scope: Scope,
        aspectDefinitions: Map<String, AspectDefinition>,
        filterEvaluationService: FilterEvaluationService,
    ): Either<ContextError, Boolean> = filterEvaluationService.evaluateScope(filter, scope, aspectDefinitions)

    /**
     * Check if aspects match this context view's filter.
     * This is a lower-level method that works directly with aspects.
     *
     * @param aspects The aspects to evaluate
     * @param aspectDefinitions Map of aspect definitions for type-aware comparison
     * @param filterEvaluationService The domain service for filter evaluation
     * @return Either an error or boolean indicating if the aspects match
     */
    fun matchesAspects(
        aspects: Aspects,
        aspectDefinitions: Map<String, AspectDefinition>,
        filterEvaluationService: FilterEvaluationService,
    ): Either<ContextError, Boolean> = filterEvaluationService.evaluateAspects(filter, aspects, aspectDefinitions)

    /**
     * Get all scopes that match this context view's filter from a list.
     *
     * @param scopes List of scopes to filter
     * @param aspectDefinitions Map of aspect definitions for type-aware comparison
     * @param filterEvaluationService The domain service for filter evaluation
     * @return List of scopes that match the filter
     */
    fun filterScopes(
        scopes: List<Scope>,
        aspectDefinitions: Map<String, AspectDefinition>,
        filterEvaluationService: FilterEvaluationService,
    ): Either<ContextError, List<Scope>> {
        val results = mutableListOf<Scope>()

        for (scope in scopes) {
            when (val result = evaluateScope(scope, aspectDefinitions, filterEvaluationService)) {
                is Either.Left -> return result
                is Either.Right -> if (result.value) results.add(scope)
            }
        }

        return results.right()
    }
}
