package io.github.kamiazya.scopes.scopemanagement.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Domain service for managing scope aliases.
 *
 * Encapsulates business rules and complex operations related to scope aliases,
 * including validation, conflict resolution, and alias lifecycle management.
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
    suspend fun assignCanonicalAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopeAliasError, ScopeAlias> = either {
        // Check if alias name is already taken by another scope
        val existingAlias = aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

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
        val existingCanonical = aliasRepository.findCanonicalByScopeId(scopeId)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

        if (existingCanonical != null && existingCanonical.aliasName != aliasName) {
            // Convert existing canonical to custom only if it's not the same alias
            val updatedAlias = existingCanonical.copy(
                aliasType = AliasType.CUSTOM,
                updatedAt = Clock.System.now(),
            )
            aliasRepository.update(updatedAlias)
                .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                .bind()
        }

        // If the alias already exists for this scope, update it to canonical
        if (existingAlias != null && existingAlias.scopeId == scopeId) {
            val updatedAlias = existingAlias.copy(
                aliasType = AliasType.CANONICAL,
                updatedAt = Clock.System.now(),
            )
            aliasRepository.update(updatedAlias)
                .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                .bind()
            return@either updatedAlias
        }

        // Create new canonical alias
        val newAlias = ScopeAlias.createCanonical(scopeId, aliasName)
        aliasRepository.save(newAlias)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

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
    suspend fun generateCanonicalAlias(scopeId: ScopeId, maxRetries: Int = 10): Either<ScopeAliasError, ScopeAlias> = either {
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
                            ScopeAliasError.DuplicateAlias(
                                occurredAt = inputError.occurredAt,
                                aliasName = inputError.attemptedValue,
                                existingScopeId = scopeId, // Generation failed, use same scope ID
                                attemptedScopeId = scopeId,
                            )
                        is ScopeInputError.AliasError.Empty ->
                            ScopeAliasError.DuplicateAlias(
                                occurredAt = inputError.occurredAt,
                                aliasName = inputError.attemptedValue,
                                existingScopeId = scopeId,
                                attemptedScopeId = scopeId,
                            )
                        is ScopeInputError.AliasError.TooShort ->
                            ScopeAliasError.DuplicateAlias(
                                occurredAt = inputError.occurredAt,
                                aliasName = inputError.attemptedValue,
                                existingScopeId = scopeId,
                                attemptedScopeId = scopeId,
                            )
                        is ScopeInputError.AliasError.TooLong ->
                            ScopeAliasError.DuplicateAlias(
                                occurredAt = inputError.occurredAt,
                                aliasName = inputError.attemptedValue,
                                existingScopeId = scopeId,
                                attemptedScopeId = scopeId,
                            )
                    }
                }
                .bind()

            // Check if alias name is already taken
            val existingAlias = aliasRepository.findByAliasName(aliasName)
                .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                .bind()

            // If name is not taken, proceed to create the alias
            if (existingAlias == null) {
                // Remove existing canonical alias if any
                val existingCanonical = aliasRepository.findCanonicalByScopeId(scopeId)
                    .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                    .bind()

                if (existingCanonical != null) {
                    // Convert existing canonical to custom
                    val updatedAlias = existingCanonical.copy(
                        aliasType = AliasType.CUSTOM,
                        updatedAt = Clock.System.now(),
                    )
                    aliasRepository.update(updatedAlias)
                        .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                        .bind()
                }

                // Create new canonical alias with the generated ID
                val newAlias = ScopeAlias.createCanonicalWithId(
                    id = aliasId,
                    scopeId = scopeId,
                    aliasName = aliasName,
                )

                aliasRepository.save(newAlias)
                    .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                    .bind()

                return@either newAlias
            }

            // If name collision occurred, continue to next iteration
            // The loop will generate a new ID and try again
        }

        // If all retries exhausted, return an error
        // This is a special case where we want to always fail after the loop
        raise(
            ScopeAliasError.DuplicateAlias(
                occurredAt = Clock.System.now(),
                aliasName = "Could not generate unique alias after $maxRetries attempts",
                existingScopeId = scopeId,
                attemptedScopeId = scopeId,
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
    suspend fun assignCustomAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopeAliasError, ScopeAlias> = either {
        val existingAlias = aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

        ensure(existingAlias == null) {
            ScopeAliasError.DuplicateAlias(
                Clock.System.now(),
                aliasName.value,
                existingAlias!!.scopeId,
                scopeId,
            )
        }

        val newAlias = ScopeAlias.createCustom(scopeId, aliasName)
        aliasRepository.save(newAlias)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

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
    suspend fun removeAlias(aliasName: AliasName): Either<ScopeAliasError, ScopeAlias> = either {
        val alias = aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

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

        aliasRepository.removeByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

        alias
    }

    /**
     * Resolves an alias to a scope ID.
     *
     * @param aliasName The alias name to resolve
     * @return Either an error or the scope ID
     */
    suspend fun resolveAlias(aliasName: AliasName): Either<ScopeAliasError, ScopeId> = either {
        val alias = aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .bind()

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
    suspend fun getAliasesForScope(scopeId: ScopeId): Either<ScopeAliasError, List<ScopeAlias>> = aliasRepository.findByScopeId(scopeId)
        .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), scopeId.value) }

    /**
     * Finds aliases that start with a given prefix.
     * Used for tab completion and partial matching.
     *
     * @param prefix The prefix to match
     * @param limit Maximum number of results
     * @return Either an error or the list of matching aliases
     */
    suspend fun findAliasesByPrefix(prefix: String, limit: Int = 50): Either<ScopeAliasError, List<ScopeAlias>> =
        aliasRepository.findByAliasNamePrefix(prefix, limit)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), prefix) }
}
