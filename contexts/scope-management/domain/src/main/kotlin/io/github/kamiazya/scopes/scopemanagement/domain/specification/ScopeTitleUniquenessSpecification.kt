package io.github.kamiazya.scopes.scopemanagement.domain.specification

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeUniquenessError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeTitle
import kotlinx.datetime.Clock

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
     * @param titleExistsChecker Function to check if a title exists among siblings and return the existing scope ID
     * @return Either an error if duplicate exists, or Unit if unique
     */
    fun isSatisfiedBy(
        newTitle: ScopeTitle,
        parentId: ScopeId?,
        currentScopeId: ScopeId?,
        currentTitle: ScopeTitle?,
        titleExistsChecker: suspend (ScopeTitle, ScopeId?) -> Boolean,
    ): suspend (Unit) -> Either<ScopesError, Unit> = {
        either {
            // If updating and the title hasn't changed, it's valid
            if (currentTitle != null && currentTitle == newTitle) {
                return@either
            }

            val titleExists = titleExistsChecker(newTitle, parentId)

            ensure(!titleExists) {
                ScopeUniquenessError.DuplicateTitle(
                    occurredAt = Clock.System.now(),
                    title = newTitle.value,
                    parentScopeId = parentId,
                    existingScopeId = currentScopeId ?: ScopeId.generate(), // Placeholder for unknown existing scope
                )
            }
        }
    }

    /**
     * Simplified version for new scope creation.
     */
    fun isSatisfiedByForCreation(
        title: ScopeTitle,
        parentId: ScopeId?,
        titleExistsChecker: suspend (ScopeTitle, ScopeId?) -> Boolean,
    ): suspend (Unit) -> Either<ScopesError, Unit> = isSatisfiedBy(title, parentId, null, null, titleExistsChecker)

    /**
     * Simplified version for scope update.
     */
    fun isSatisfiedByForUpdate(
        newTitle: ScopeTitle,
        currentTitle: ScopeTitle,
        parentId: ScopeId?,
        scopeId: ScopeId,
        titleExistsChecker: suspend (ScopeTitle, ScopeId?) -> Boolean,
    ): suspend (Unit) -> Either<ScopesError, Unit> = isSatisfiedBy(newTitle, parentId, scopeId, currentTitle, titleExistsChecker)
}
