package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Domain service for validating scope uniqueness constraints.
 *
 * This service encapsulates business rules related to scope title uniqueness
 * across different contexts and hierarchies.
 */
class ScopeUniquenessValidationService(private val scopeRepository: ScopeRepository) {

    /**
     * Validates that a scope title is unique across specified contexts.
     *
     * Business Rule: Scope titles must be unique within their context hierarchy.
     * This ensures clear identification and prevents confusion in the domain.
     *
     * @param title The scope title to validate
     * @param contextIds List of context IDs where uniqueness must be checked
     * @return Either a domain error or Unit on success
     */
    suspend fun validateCrossContextUniqueness(title: String, contextIds: List<ScopeId>): Either<ContextError, Unit> = either {
        // Business rule: Title must be unique across all specified contexts
        contextIds.forEach { contextId ->
            val existsInContext = scopeRepository.existsByParentIdAndTitle(contextId, title)
                .mapLeft { ContextError.DuplicateScope(title, contextId.value, "Failed to check uniqueness") }
                .bind()

            ensure(!existsInContext) {
                ContextError.DuplicateScope(
                    title = title,
                    contextId = contextId.value,
                    reason = "Scope title already exists in this context",
                )
            }
        }
    }

    /**
     * Validates that a scope title is unique within its parent context.
     *
     * @param title The scope title to validate
     * @param parentId The parent scope ID (null for root scopes)
     * @param excludeId Optional scope ID to exclude from uniqueness check (for updates)
     * @return Either a domain error or Unit on success
     */
    suspend fun validateTitleUniquenessInContext(title: String, parentId: ScopeId?, excludeId: ScopeId? = null): Either<ContextError, Unit> = either {
        val existingScopes = scopeRepository.findByParentId(parentId)
            .mapLeft { ContextError.DuplicateScope(title, parentId?.value, "Failed to check existing scopes") }
            .bind()

        // Check if any existing scope has the same title (excluding the scope being updated)
        val hasConflict = existingScopes.any { scope ->
            scope.title.value == title && scope.id != excludeId
        }

        ensure(!hasConflict) {
            ContextError.DuplicateScope(
                title = title,
                contextId = parentId?.value,
                reason = "Scope with this title already exists in the same context",
            )
        }
    }

    /**
     * Validates global uniqueness of a scope title.
     * Used for special cases where global uniqueness is required.
     *
     * This method checks all scopes in the system for title conflicts.
     * Since the repository doesn't have a direct existsByTitle method,
     * we use findAll and filter in-memory.
     *
     * @param title The scope title to validate
     * @param excludeId Optional scope ID to exclude from uniqueness check
     * @return Either a domain error or Unit on success
     */
    suspend fun validateGlobalUniqueness(title: String, excludeId: ScopeId? = null): Either<ContextError, Unit> = either {
        val allScopes = scopeRepository.findAll()
            .mapLeft { ContextError.DuplicateScope(title, null, "Failed to check global uniqueness") }
            .bind()

        val exists = allScopes.any { scope ->
            scope.title.value == title && scope.id != excludeId
        }

        ensure(!exists) {
            ContextError.DuplicateScope(
                title = title,
                contextId = null,
                reason = "Scope title must be globally unique",
            )
        }
    }
}
