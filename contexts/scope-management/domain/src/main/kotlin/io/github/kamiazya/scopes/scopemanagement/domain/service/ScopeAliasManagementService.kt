package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Domain service for managing scope aliases.
 *
 * Contains core business logic for alias operations including:
 * - Business rule enforcement (one canonical alias per scope)
 * - Alias conflict resolution
 * - Alias lifecycle management
 *
 * This is a domain service because it contains business logic that doesn't
 * naturally fit into a single entity but operates across multiple entities.
 */
class ScopeAliasManagementService(private val aliasRepository: ScopeAliasRepository, private val aliasGenerationService: AliasGenerationService) {

    /**
     * Assigns a canonical alias to a scope.
     *
     * Business Rules:
     * - Only one canonical alias per scope
     * - Alias names must be unique across all scopes
     * - If scope already has canonical alias, this replaces it
     *
     * @param scopeId The scope to assign the alias to
     * @param aliasName The alias name to assign
     * @return Either an error or the created alias
     */
    suspend fun assignCanonicalAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopesError, ScopeAlias> = either {
        // Check if alias name is already taken by another scope
        val existingAlias = aliasRepository.findByAliasName(aliasName).bind()

        ensure(existingAlias == null || existingAlias.scopeId == scopeId) {
            ScopeAliasError.DuplicateAlias(
                Clock.System.now(),
                aliasName.value,
                existingAlias!!.scopeId,
                scopeId,
            )
        }

        // If the alias already exists for this scope and is canonical, return it (idempotent)
        if (existingAlias != null && existingAlias.scopeId == scopeId && existingAlias.isCanonical()) {
            return@either existingAlias
        }

        // Remove existing canonical alias if any
        val existingCanonical = aliasRepository.findCanonicalByScopeId(scopeId).bind()

        if (existingCanonical != null && existingCanonical.aliasName != aliasName) {
            // Convert existing canonical to custom only if it's not the same alias
            val updatedAlias = existingCanonical.copy(
                aliasType = AliasType.CUSTOM,
                updatedAt = Clock.System.now(),
            )
            aliasRepository.update(updatedAlias).bind()
        }

        // If the alias already exists for this scope, update it to canonical
        if (existingAlias != null && existingAlias.scopeId == scopeId) {
            val updatedAlias = existingAlias.copy(
                aliasType = AliasType.CANONICAL,
                updatedAt = Clock.System.now(),
            )
            aliasRepository.update(updatedAlias).bind()
            return@either updatedAlias
        }

        // Create new canonical alias
        val newAlias = ScopeAlias.createCanonical(scopeId, aliasName)
        aliasRepository.save(newAlias).bind()

        newAlias
    }

    /**
     * Generates and assigns a canonical alias using Haikunator pattern.
     *
     * Creates a new alias with a unique ID, then generates a deterministic name
     * based on that ID. This makes the alias self-contained and not dependent
     * on the scope ID for name generation.
     *
     * Uses an iterative approach with retry limit to avoid potential stack overflow
     * from recursive calls in case of frequent name collisions.
     *
     * @param scopeId The scope to generate an alias for
     * @param maxRetries Maximum number of attempts to generate a unique alias (default: 10)
     * @return Either an error or the created alias
     */
    suspend fun generateCanonicalAlias(scopeId: ScopeId, maxRetries: Int = 10): Either<ScopesError, ScopeAlias> = either {
        // Iterative approach to avoid stack overflow
        repeat(maxRetries) { attempt ->
            // Generate a new unique ID for the alias
            val aliasId = AliasId.generate()

            // Generate deterministic name based on the alias ID
            val aliasName = aliasGenerationService.generateCanonicalAlias(aliasId)
                .mapLeft { inputError ->
                    // Convert ScopeInputError.AliasError to ScopeAliasError
                    when (inputError) {
                        is ScopeInputError.AliasError.InvalidFormat ->
                            ScopeAliasError.AliasGenerationValidationFailed(
                                occurredAt = inputError.occurredAt,
                                scopeId = scopeId,
                                reason = "Invalid format",
                                attemptedValue = inputError.attemptedValue,
                            )
                        is ScopeInputError.AliasError.Empty ->
                            ScopeAliasError.AliasGenerationValidationFailed(
                                occurredAt = inputError.occurredAt,
                                scopeId = scopeId,
                                reason = "Empty alias",
                                attemptedValue = inputError.attemptedValue,
                            )
                        is ScopeInputError.AliasError.TooShort ->
                            ScopeAliasError.AliasGenerationValidationFailed(
                                occurredAt = inputError.occurredAt,
                                scopeId = scopeId,
                                reason = "Too short (minimum ${inputError.minimumLength} characters)",
                                attemptedValue = inputError.attemptedValue,
                            )
                        is ScopeInputError.AliasError.TooLong ->
                            ScopeAliasError.AliasGenerationValidationFailed(
                                occurredAt = inputError.occurredAt,
                                scopeId = scopeId,
                                reason = "Too long (maximum ${inputError.maximumLength} characters)",
                                attemptedValue = inputError.attemptedValue,
                            )
                    }
                }
                .bind()

            // Check if alias name is already taken
            val existingAlias = aliasRepository.findByAliasName(aliasName).bind()

            // If name is not taken, proceed to create the alias
            if (existingAlias == null) {
                // Remove existing canonical alias if any
                val existingCanonical = aliasRepository.findCanonicalByScopeId(scopeId).bind()

                if (existingCanonical != null) {
                    // Convert existing canonical to custom
                    val updatedAlias = existingCanonical.copy(
                        aliasType = AliasType.CUSTOM,
                        updatedAt = Clock.System.now(),
                    )
                    aliasRepository.update(updatedAlias).bind()
                }

                // Create new canonical alias with the generated ID
                val newAlias = ScopeAlias.createCanonicalWithId(
                    id = aliasId,
                    scopeId = scopeId,
                    aliasName = aliasName,
                )

                aliasRepository.save(newAlias).bind()

                return@either newAlias
            }

            // If name collision occurred, continue to next iteration
            // The loop will generate a new ID and try again
        }

        // If all retries exhausted, return an error
        // This is a special case where we want to always fail after the loop
        raise(
            ScopeAliasError.AliasGenerationFailed(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
                retryCount = maxRetries,
            ),
        )
    }

    /**
     * Assigns a custom alias to a scope.
     *
     * Business Rules:
     * - Multiple custom aliases allowed per scope
     * - Alias names must be unique across all scopes
     *
     * @param scopeId The scope to assign the alias to
     * @param aliasName The alias name to assign
     * @return Either an error or the created alias
     */
    suspend fun assignCustomAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopesError, ScopeAlias> = either {
        val existingAlias = aliasRepository.findByAliasName(aliasName).bind()

        ensure(existingAlias == null) {
            ScopeAliasError.DuplicateAlias(
                Clock.System.now(),
                aliasName.value,
                existingAlias!!.scopeId,
                scopeId,
            )
        }

        val newAlias = ScopeAlias.createCustom(scopeId, aliasName)
        aliasRepository.save(newAlias).bind()

        newAlias
    }

    /**
     * Removes an alias.
     *
     * Business Rules:
     * - Cannot remove canonical aliases (must replace instead)
     * - Custom aliases can be removed freely
     *
     * @param aliasName The alias name to remove
     * @return Either an error or the removed alias
     */
    suspend fun removeAlias(aliasName: AliasName): Either<ScopesError, ScopeAlias> = either {
        val alias = aliasRepository.findByAliasName(aliasName).bind()

        ensureNotNull(alias) {
            ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value)
        }

        ensure(!alias.isCanonical()) {
            ScopeAliasError.CannotRemoveCanonicalAlias(
                Clock.System.now(),
                alias.scopeId,
                aliasName.value,
            )
        }

        val removed = aliasRepository.removeByAliasName(aliasName).bind()
        ensure(removed) {
            ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value)
        }

        alias
    }

    /**
     * Resolves an alias to a scope ID.
     *
     * @param aliasName The alias name to resolve
     * @return Either an error or the scope ID
     */
    suspend fun resolveAlias(aliasName: AliasName): Either<ScopesError, ScopeId> = either {
        val alias = aliasRepository.findByAliasName(aliasName).bind()

        ensureNotNull(alias) {
            ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value)
        }

        alias.scopeId
    }

    /**
     * Gets all aliases for a scope.
     *
     * @param scopeId The scope ID
     * @return Either an error or the list of aliases
     */
    suspend fun getAliasesForScope(scopeId: ScopeId): Either<ScopesError, List<ScopeAlias>> = aliasRepository.findByScopeId(scopeId)

    /**
     * Finds aliases that start with a given prefix.
     * Used for tab completion and partial matching.
     *
     * @param prefix The prefix to match
     * @param limit Maximum number of results
     * @return Either an error or the list of matching aliases
     */
    suspend fun findAliasesByPrefix(prefix: String, limit: Int = 50): Either<ScopesError, List<ScopeAlias>> =
        aliasRepository.findByAliasNamePrefix(prefix, limit)

    /**
     * Atomically renames an alias.
     *
     * This method ensures that the rename operation is atomic - either both the removal
     * of the old alias and creation of the new alias succeed, or neither happens.
     * This prevents data loss in case of failures.
     *
     * Business Rules:
     * - The old alias must exist
     * - The new alias name must not already exist (unless it belongs to the same scope)
     * - Canonical aliases remain canonical after rename
     * - Custom aliases remain custom after rename
     *
     * @param oldAliasName The current alias name to rename from
     * @param newAliasName The new alias name to rename to
     * @return Either an error or the renamed alias
     */
    suspend fun renameAlias(oldAliasName: AliasName, newAliasName: AliasName): Either<ScopesError, ScopeAlias> = either {
        // First, find the existing alias to get its details
        val existingAlias = aliasRepository.findByAliasName(oldAliasName).bind()

        ensureNotNull(existingAlias) {
            ScopeAliasError.AliasNotFound(Clock.System.now(), oldAliasName.value)
        }

        val scopeId = existingAlias.scopeId
        val oldAliasType = existingAlias.aliasType

        // Check if the new alias name is already taken by a different scope
        val existingNewAlias = aliasRepository.findByAliasName(newAliasName).bind()

        if (existingNewAlias != null && existingNewAlias.scopeId != scopeId) {
            raise(
                ScopeAliasError.DuplicateAlias(
                    Clock.System.now(),
                    newAliasName.value,
                    existingNewAlias.scopeId,
                    scopeId,
                ),
            )
        }

        // If the new alias already exists for the same scope, preserve the old alias's type
        // (this handles the edge case where renaming to an existing alias of the same scope)
        if (existingNewAlias != null && existingNewAlias.scopeId == scopeId) {
            // Update the existing alias to preserve the old alias's type if needed
            val updatedAlias = if (existingNewAlias.aliasType != oldAliasType) {
                val aliasWithPreservedType = existingNewAlias.copy(
                    aliasType = oldAliasType,
                    updatedAt = Clock.System.now(),
                )
                val updated = aliasRepository.update(aliasWithPreservedType).bind()
                ensure(updated) {
                    ScopeAliasError.AliasNotFound(Clock.System.now(), newAliasName.value)
                }
                aliasWithPreservedType
            } else {
                existingNewAlias
            }

            // Remove the old alias
            val removed = aliasRepository.removeByAliasName(oldAliasName).bind()
            ensure(removed) {
                ScopeAliasError.AliasNotFound(Clock.System.now(), oldAliasName.value)
            }

            return@either updatedAlias
        }

        // Perform atomic rename by updating the existing alias
        // This approach is more atomic than remove-then-create
        val renamedAlias = existingAlias.copy(
            aliasName = newAliasName,
            updatedAt = Clock.System.now(),
        )

        // Update the alias in the repository
        // Most repositories should support atomic updates
        val updated = aliasRepository.update(renamedAlias).bind()
        ensure(updated) {
            ScopeAliasError.AliasNotFound(Clock.System.now(), oldAliasName.value)
        }

        renamedAlias
    }
}
