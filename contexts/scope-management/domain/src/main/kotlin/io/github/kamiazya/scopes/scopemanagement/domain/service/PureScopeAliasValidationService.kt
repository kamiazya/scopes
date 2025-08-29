package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasOperation
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ConflictResolution
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Pure domain service for scope alias validation and business logic.
 *
 * This service contains only business logic without any I/O operations,
 * making it easy to test and reason about. All repository operations
 * are handled by the application layer.
 */
class PureScopeAliasValidationService {

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
        // Rule: Alias name must not be taken by another scope
        ensure(existingAliasWithName == null || existingAliasWithName.scopeId == scopeId) {
            ScopeAliasError.DuplicateAlias(
                occurredAt = Clock.System.now(),
                aliasName = aliasName.value,
                existingScopeId = existingAliasWithName!!.scopeId,
                attemptedScopeId = scopeId,
            )
        }

        // Check if we're updating to the same alias name
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
        { AliasOperation.Error(it) },
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
                occurredAt = Clock.System.now(),
                aliasName = aliasName.value,
                existingScopeId = existingAliasWithName!!.scopeId,
                attemptedScopeId = scopeId,
            )
        }

        ScopeAlias.createCustom(
            scopeId = scopeId,
            aliasName = aliasName,
            timestamp = Clock.System.now(),
        )
    }

    /**
     * Validates generated alias creation.
     *
     * Business Rules:
     * - Generated aliases must be unique
     * - Only one generated alias per scope
     *
     * @param scopeId The scope to create alias for
     * @param aliasName The generated alias name
     * @param existingAliasWithName Existing alias with this name (if any)
     * @param existingGeneratedForScope Existing generated alias for this scope (if any)
     * @return Either an error or the operation to perform
     */
    fun validateGeneratedAliasCreation(
        scopeId: ScopeId,
        aliasName: AliasName,
        existingAliasWithName: ScopeAlias?,
        existingGeneratedForScope: ScopeAlias?,
    ): Either<ScopesError, AliasOperation> = either {
        ensure(existingAliasWithName == null) {
            ScopeAliasError.DuplicateAlias(
                occurredAt = Clock.System.now(),
                aliasName = aliasName.value,
                existingScopeId = existingAliasWithName!!.scopeId,
                attemptedScopeId = scopeId,
            )
        }

        val newAlias = ScopeAlias.createCustom(
            scopeId = scopeId,
            aliasName = aliasName,
            timestamp = Clock.System.now(),
        )

        if (existingGeneratedForScope != null) {
            AliasOperation.Replace(existingGeneratedForScope, newAlias)
        } else {
            AliasOperation.Create(newAlias)
        }
    }

    /**
     * Determines if an alias type transition is valid.
     *
     * Business Rules:
     * - Canonical aliases cannot be downgraded to custom
     * - Custom aliases can be upgraded to canonical
     * - Generated aliases can transition to any type
     *
     * @param fromType Current alias type (null if no existing alias)
     * @param toType Desired alias type
     * @return true if transition is allowed
     */
    fun isValidAliasTypeTransition(fromType: AliasType?, toType: AliasType): Boolean = when {
        fromType == null -> true // Can create any type from nothing
        fromType == toType -> true // Same type is always valid
        fromType == AliasType.CANONICAL && toType == AliasType.CUSTOM -> false // Cannot downgrade
        fromType == AliasType.CUSTOM && toType == AliasType.CANONICAL -> true // Can upgrade
        else -> false
    }

    /**
     * Validates that an alias can be deleted.
     *
     * @param alias The alias to validate for deletion
     * @param isLastAlias Whether this is the last alias for the scope
     * @return Either an error or Unit if valid
     */
    fun validateAliasDeletion(alias: ScopeAlias, isLastAlias: Boolean): Either<ScopesError, Unit> = either {
        // Current business rule: All aliases can be deleted
        // Future consideration: May want to prevent deletion of last alias
        Unit
    }

    /**
     * Determines conflict resolution when multiple alias operations are requested.
     *
     * @param requestedAliases List of alias names requested
     * @param existingAliases List of existing aliases for the scope
     * @return Resolution strategy
     */
    fun resolveAliasConflicts(requestedAliases: List<AliasName>, existingAliases: List<ScopeAlias>): ConflictResolution {
        val existingNames = existingAliases.map { it.aliasName }.toSet()
        val newAliases = requestedAliases.filter { it !in existingNames }
        val duplicates = requestedAliases.filter { it in existingNames }

        return ConflictResolution(
            toCreate = newAliases,
            alreadyExist = duplicates,
            toKeep = existingAliases.filter { it.aliasName !in requestedAliases },
        )
    }
}
