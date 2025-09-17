package io.github.kamiazya.scopes.scopemanagement.domain.service.alias

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasOperation
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Domain policy that encapsulates business rules for scope alias management.
 *
 * This policy defines and enforces the business rules for:
 * - Canonical alias assignment and replacement
 * - Custom alias creation and validation
 * - Alias type transitions
 * - Conflict resolution strategies
 * - Alias operation validation
 *
 * The policy ensures consistency and integrity of alias operations
 * according to the domain's business requirements.
 */
class ScopeAliasPolicy(private val conflictResolutionStrategy: AliasConflictResolutionStrategy = AliasConflictResolutionStrategy.FAIL_FAST) {

    /**
     * Determines the operation for canonical alias assignment.
     *
     * Business Rules:
     * - Only one canonical alias per scope
     * - Alias names must be unique across all scopes
     * - If scope already has canonical alias, this replaces it
     *
     * @param scopeId The scope to assign the alias to
     * @param aliasName The proposed alias name
     * @param existingAliasWithName Existing alias with this name (if any)
     * @param existingCanonicalForScope Existing canonical alias for this scope (if any)
     * @return The operation to perform
     */
    fun determineCanonicalAliasOperation(
        scopeId: ScopeId,
        aliasName: AliasName,
        existingAliasWithName: ScopeAlias?,
        existingCanonicalForScope: ScopeAlias?,
    ): AliasOperation = either<ScopesError, AliasOperation> {
        // Check if there's an existing alias with this name
        if (existingAliasWithName != null) {
            if (existingAliasWithName.scopeId != scopeId) {
                // Rule: Alias name must not be taken by another scope
                raise(
                    ScopeAliasError.DuplicateAlias(
                        alias = aliasName.value,
                        scopeId = existingAliasWithName.scopeId,
                    ),
                )
            } else if (existingAliasWithName.isCustom()) {
                // If this scope already has a custom alias with this name, promote it
                return@either AliasOperation.Promote(existingAliasWithName)
            } else if (existingAliasWithName.isCanonical()) {
                // Already canonical with this name
                return@either AliasOperation.NoChange("Canonical alias already set to this name")
            }
        }

        // Check if we're updating to the same alias name (shouldn't happen after above checks)
        if (existingCanonicalForScope?.aliasName == aliasName) {
            return@either AliasOperation.NoChange("Canonical alias already set to this name")
        }

        val newAlias = ScopeAlias.createCanonical(
            scopeId = scopeId,
            aliasName = aliasName,
            timestamp = Clock.System.now(),
        )

        if (existingCanonicalForScope != null) {
            AliasOperation.Replace(existingCanonicalForScope, newAlias)
        } else {
            AliasOperation.Create(newAlias)
        }
    }.fold(
        { AliasOperation.Failure(it) },
        { it },
    )

    /**
     * Validates custom alias creation.
     *
     * Business Rules:
     * - Alias names must be unique across all scopes
     * - Multiple custom aliases allowed per scope
     *
     * @param scopeId The scope to create alias for
     * @param aliasName The proposed alias name
     * @param existingAliasWithName Existing alias with this name (if any)
     * @return Either an error or the alias to create
     */
    fun validateCustomAliasCreation(scopeId: ScopeId, aliasName: AliasName, existingAliasWithName: ScopeAlias?): Either<ScopesError, ScopeAlias> = either {
        ensure(existingAliasWithName == null) {
            ScopeAliasError.DuplicateAlias(
                alias = aliasName.value,
                scopeId = existingAliasWithName!!.scopeId,
            )
        }

        ScopeAlias.createCustom(
            scopeId = scopeId,
            aliasName = aliasName,
            timestamp = Clock.System.now(),
        )
    }

    /**
     * Validates canonical alias creation during scope creation.
     *
     * This is used when creating a scope's initial canonical alias, which can be either:
     * - User-specified alias provided at scope creation time
     * - Auto-generated alias (e.g., from Haikunator) when user doesn't specify one
     *
     * Business Rules:
     * - Canonical aliases must have unique names
     * - Only one canonical alias per scope
     *
     * @param scopeId The scope to create canonical alias for
     * @param aliasName The alias name (either user-specified or auto-generated)
     * @param existingAliasWithName Existing alias with this name (if any)
     * @param existingCanonicalForScope Existing canonical alias for this scope (if any)
     * @return Either an error or the operation to perform
     */
    fun validateInitialCanonicalAliasCreation(
        scopeId: ScopeId,
        aliasName: AliasName,
        existingAliasWithName: ScopeAlias?,
        existingCanonicalForScope: ScopeAlias?,
    ): Either<ScopesError, AliasOperation> = either {
        // Ensure the alias name is not already taken
        ensure(existingAliasWithName == null) {
            ScopeAliasError.DuplicateAlias(
                alias = aliasName.value,
                scopeId = existingAliasWithName!!.scopeId,
            )
        }

        // Create a canonical alias for this scope's initial alias
        val newAlias = ScopeAlias.createCanonical(
            scopeId = scopeId,
            aliasName = aliasName,
            timestamp = Clock.System.now(),
        )

        // If there's an existing canonical (shouldn't happen for new scopes), replace it
        if (existingCanonicalForScope != null) {
            AliasOperation.Replace(existingCanonicalForScope, newAlias)
        } else {
            AliasOperation.Create(newAlias)
        }
    }
}
