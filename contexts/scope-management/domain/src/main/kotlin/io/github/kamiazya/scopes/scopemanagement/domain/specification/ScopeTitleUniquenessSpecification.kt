package io.github.kamiazya.scopes.scopemanagement.domain.specification

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle

/**
 * Specification for validating title uniqueness within the same parent scope.
 *
 * This encapsulates the business rule that sibling scopes must have unique titles.
 * Pure domain specification without any I/O dependencies.
 */
class ScopeTitleUniquenessSpecification {

    /**
     * Checks if a title would be unique among siblings.
     *
     * @param newTitle The title to validate
     * @param parentId The parent scope ID (null for root scopes)
     * @param currentScopeId The ID of the scope being updated (null for new scopes)
     * @param currentTitle The current title of the scope being updated (for update scenarios)
     * @param titleExistsChecker Function that returns the existing scope ID if title exists, null if unique
     * @return Either an error if duplicate exists, or Unit if unique
     */
    suspend fun isSatisfiedBy(
        newTitle: ScopeTitle,
        parentId: ScopeId?,
        currentScopeId: ScopeId?,
        currentTitle: ScopeTitle?,
        titleExistsChecker: suspend (ScopeTitle, ScopeId?) -> ScopeId?,
    ): Either<ScopesError, Unit> = either {
        // If updating and the title hasn't changed, it's valid
        if (currentTitle != null && currentTitle == newTitle) {
            return@either
        }

        val existingScopeId = titleExistsChecker(newTitle, parentId)

        // If existingScopeId is not null, it means a duplicate exists
        // If it's the same as currentScopeId (for updates), it's the same scope so it's okay
        if (existingScopeId != null && existingScopeId != currentScopeId) {
            raise(
                ScopeUniquenessError.DuplicateTitle(
                    title = newTitle.value,
                    parentScopeId = parentId,
                    existingScopeId = existingScopeId, // Now we have the actual conflicting scope ID
                ),
            )
        }
    }

    /**
     * Simplified version for new scope creation.
     */
    suspend fun isSatisfiedByForCreation(
        title: ScopeTitle,
        parentId: ScopeId?,
        titleExistsChecker: suspend (ScopeTitle, ScopeId?) -> ScopeId?,
    ): Either<ScopesError, Unit> = isSatisfiedBy(title, parentId, null, null, titleExistsChecker)

    /**
     * Simplified version for scope update.
     */
    suspend fun isSatisfiedByForUpdate(
        newTitle: ScopeTitle,
        currentTitle: ScopeTitle,
        parentId: ScopeId?,
        scopeId: ScopeId,
        titleExistsChecker: suspend (ScopeTitle, ScopeId?) -> ScopeId?,
    ): Either<ScopesError, Unit> = isSatisfiedBy(newTitle, parentId, scopeId, currentTitle, titleExistsChecker)
}
