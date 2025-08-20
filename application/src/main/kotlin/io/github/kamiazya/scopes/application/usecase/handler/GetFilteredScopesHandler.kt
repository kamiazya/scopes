package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.application.dto.ContextViewResult
import io.github.kamiazya.scopes.application.dto.FilteredScopesResult
import io.github.kamiazya.scopes.application.dto.ScopeResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.error.ContextError
import io.github.kamiazya.scopes.application.error.toApplicationError
import io.github.kamiazya.scopes.application.logging.Logger
import io.github.kamiazya.scopes.application.service.ActiveContextService
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.GetFilteredScopesQuery
import io.github.kamiazya.scopes.domain.entity.Scope
import io.github.kamiazya.scopes.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.valueobject.ContextViewKey

/**
 * Handler for getting filtered scopes based on a context view.
 *
 * This handler:
 * 1. Resolves the context view (by name or active context)
 * 2. Parses the filter expression
 * 3. Retrieves scopes matching the filter
 * 4. Returns the filtered results with metadata
 */
class GetFilteredScopesHandler(
    private val contextViewRepository: ContextViewRepository,
    private val scopeRepository: ScopeRepository,
    private val activeContextService: ActiveContextService,
    private val logger: Logger,
) : UseCase<GetFilteredScopesQuery, ApplicationError, FilteredScopesResult> {

    override suspend operator fun invoke(
        input: GetFilteredScopesQuery,
    ): Either<ApplicationError, FilteredScopesResult> = either {
        logger.info(
            "Getting filtered scopes",
            mapOf(
                "contextKey" to (input.contextKey ?: "active"),
                "offset" to input.offset,
                "limit" to input.limit,
            ),
        )

        // Resolve the context view
        val contextView = if (input.contextKey != null) {
            // Find by key
            logger.debug("Finding context by key", mapOf("key" to input.contextKey))
            val contextKey = ContextViewKey.create(input.contextKey).mapLeft { errorMessage ->
                logger.warn(
                    "Invalid context key format",
                    mapOf(
                        "key" to input.contextKey,
                        "error" to errorMessage,
                    ),
                )
                ContextError.KeyInvalidFormat(
                    attemptedKey = input.contextKey,
                )
            }.bind()

            val foundContext = contextViewRepository.findByKey(contextKey).mapLeft { error ->
                logger.error(
                    "Failed to find context by key",
                    mapOf(
                        "key" to contextKey.value,
                        "error" to (error::class.simpleName ?: "Unknown"),
                    ),
                )
                error.toApplicationError()
            }.bind()

            ensure(foundContext != null) {
                logger.warn("Context not found", mapOf("key" to input.contextKey))
                ContextError.StateNotFound(
                    contextId = input.contextKey,
                )
            }
            foundContext
        } else {
            // Use active context
            logger.debug("Using active context")
            val activeContext = activeContextService.getCurrentContext()
            ensure(activeContext != null) {
                logger.warn("No active context found")
                ContextError.StateNotFound(
                    contextId = "active",
                )
            }
            activeContext
        }

        logger.debug(
            "Context resolved",
            mapOf(
                "id" to contextView.id.value,
                "name" to contextView.name.value,
                "filter" to contextView.filter.value,
            ),
        )

        // Get all scopes (for total count)
        logger.debug("Fetching all scopes")
        val allScopes = scopeRepository.findAll().mapLeft { error ->
            logger.error(
                "Failed to fetch scopes",
                mapOf(
                    "error" to (error::class.simpleName ?: "Unknown"),
                ),
            )
            error.toApplicationError()
        }.bind()

        logger.debug("Total scopes found", mapOf("count" to allScopes.size))

        // Parse and apply the filter expression
        val filteredScopes = filterScopes(allScopes, contextView.filter.value)
        logger.debug(
            "Scopes filtered",
            mapOf(
                "total" to allScopes.size,
                "filtered" to filteredScopes.size,
            ),
        )

        // Apply pagination
        val paginatedScopes = filteredScopes
            .drop(input.offset)
            .take(input.limit)

        logger.debug(
            "Pagination applied",
            mapOf(
                "offset" to input.offset,
                "limit" to input.limit,
                "returned" to paginatedScopes.size,
            ),
        )

        // Map to DTOs
        val scopeResults = paginatedScopes.map { scope ->
            // Convert Aspects to Map<String, List<String>>
            val aspectsMap = mutableMapOf<String, List<String>>()
            scope.aspects.toMap().forEach { (key, values) ->
                aspectsMap[key.value] = values.map { it.value }
            }

            ScopeResult(
                id = scope.id.value,
                title = scope.title.value,
                description = scope.description?.value,
                parentId = scope.parentId?.value,
                aspects = aspectsMap,
                createdAt = scope.createdAt,
                updatedAt = scope.updatedAt,
            )
        }

        val contextViewResult = ContextViewResult(
            id = contextView.id.value,
            key = contextView.key.value,
            name = contextView.name.value,
            filterExpression = contextView.filter.value,
            description = contextView.description?.value,
            isActive = activeContextService.getCurrentContext()?.id == contextView.id,
            createdAt = contextView.createdAt,
            updatedAt = contextView.updatedAt,
        )

        logger.info(
            "Filtered scopes retrieved successfully",
            mapOf(
                "contextId" to contextView.id.value,
                "totalScopes" to allScopes.size,
                "filteredScopes" to filteredScopes.size,
                "returnedScopes" to scopeResults.size,
            ),
        )

        FilteredScopesResult(
            scopes = scopeResults,
            appliedContext = contextViewResult,
            totalCount = allScopes.size,
            filteredCount = filteredScopes.size,
        )
    }.onLeft { error ->
        logger.error(
            "Failed to get filtered scopes",
            mapOf(
                "error" to (error::class.simpleName ?: "Unknown"),
                "message" to error.toString(),
            ),
        )
    }

    /**
     * Simple filter parser and evaluator.
     * Supports basic filters like:
     * - status:active
     * - type:task
     * - priority:high
     * - Combined with AND/OR
     */
    private fun filterScopes(scopes: List<Scope>, filterExpression: String): List<Scope> {
        if (filterExpression.isBlank() || filterExpression == "all") {
            return scopes
        }

        // Parse simple key:value filters
        val filters = filterExpression.split(" AND ", " OR ", ",")
            .map { it.trim() }
            .filter { it.contains(':') }
            .map { filter ->
                val parts = filter.split(':')
                if (parts.size == 2) {
                    parts[0].trim() to parts[1].trim()
                } else {
                    null
                }
            }
            .filterNotNull()

        if (filters.isEmpty()) {
            return scopes
        }

        // Apply filters
        return scopes.filter { scope ->
            filters.any { (key, value) ->
                // Check if the scope has this aspect with the specified value
                val aspectKey = scope.aspects.keys().find { it.value == key }
                if (aspectKey != null) {
                    val aspectValues = scope.aspects.get(aspectKey)
                    aspectValues?.any { it.value.equals(value, ignoreCase = true) } == true
                } else {
                    // Check special properties
                    when (key.lowercase()) {
                        "title" -> scope.title.value.contains(value, ignoreCase = true)
                        "description" -> scope.description?.value?.contains(value, ignoreCase = true) == true
                        "has_parent" -> (value == "true" && scope.parentId != null) ||
                            (value == "false" && scope.parentId == null)
                        else -> false
                    }
                }
            }
        }
    }
}
