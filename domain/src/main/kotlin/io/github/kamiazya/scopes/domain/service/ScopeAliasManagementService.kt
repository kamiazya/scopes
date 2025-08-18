package io.github.kamiazya.scopes.domain.service

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.domain.valueobject.AliasId
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.AliasType
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Domain service for managing scope aliases.
 *
 * Encapsulates business rules and complex operations related to scope aliases,
 * including validation, conflict resolution, and alias lifecycle management.
 */
class ScopeAliasManagementService(
    private val aliasRepository: ScopeAliasRepository,
    private val aliasGenerationService: AliasGenerationService
) {

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
    suspend fun assignCanonicalAlias(
        scopeId: ScopeId,
        aliasName: AliasName
    ): Either<ScopeAliasError, ScopeAlias> {
        // Check if alias name is already taken by another scope
        return aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .flatMap { existingAlias ->
                when {
                    existingAlias != null && existingAlias.scopeId != scopeId -> {
                        ScopeAliasError.DuplicateAlias(
                            Clock.System.now(),
                            aliasName.value,
                            existingAlias.scopeId,
                            scopeId
                        ).left()
                    }
                    else -> {
                        // Remove existing canonical alias if any
                        aliasRepository.findCanonicalByScopeId(scopeId)
                            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                            .flatMap { existingCanonical ->
                                if (existingCanonical != null) {
                                    // Convert existing canonical to custom
                                    val updatedAlias = existingCanonical.copy(
                                        aliasType = AliasType.CUSTOM,
                                        updatedAt = Clock.System.now()
                                    )
                                    aliasRepository.update(updatedAlias)
                                        .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                                } else {
                                    Unit.right()
                                }
                            }
                            .flatMap {
                                // Create new canonical alias
                                val newAlias = ScopeAlias.createCanonical(scopeId, aliasName)
                                aliasRepository.save(newAlias)
                                    .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                                    .map { newAlias }
                            }
                    }
                }
            }
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
    suspend fun generateCanonicalAlias(
        scopeId: ScopeId,
        maxRetries: Int = 10
    ): Either<ScopeAliasError, ScopeAlias> {
        // Iterative approach to avoid stack overflow
        repeat(maxRetries) { attempt ->
            // Generate a new unique ID for the alias
            val aliasId = AliasId.generate()

            // Generate deterministic name based on the alias ID
            val nameResult = aliasGenerationService.generateCanonicalAlias(aliasId)
                .mapLeft { inputError ->
                    // Convert ScopeInputError.AliasError to ScopeAliasError
                    when (inputError) {
                        is ScopeInputError.AliasError.InvalidFormat ->
                            ScopeAliasError.DuplicateAlias(
                                occurredAt = inputError.occurredAt,
                                aliasName = inputError.attemptedValue,
                                existingScopeId = scopeId,  // Generation failed, use same scope ID
                                attemptedScopeId = scopeId
                            )
                        else ->
                            ScopeAliasError.AliasNotFound(Clock.System.now(), "generation-failed")
                    }
                }

            // If name generation failed, return the error
            if (nameResult.isLeft()) {
                return nameResult.leftOrNull()!!.left()
            }

            val aliasName = nameResult.getOrNull()!!

            // Check if alias name is already taken
            val existingAliasResult = aliasRepository.findByAliasName(aliasName)
                .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }

            if (existingAliasResult.isLeft()) {
                return existingAliasResult.leftOrNull()!!.left()
            }

            val existingAlias = existingAliasResult.getOrNull()

            // If name is not taken, proceed to create the alias
            if (existingAlias == null) {
                // Remove existing canonical alias if any
                val removeResult = aliasRepository.findCanonicalByScopeId(scopeId)
                    .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                    .flatMap { existingCanonical ->
                        if (existingCanonical != null) {
                            // Convert existing canonical to custom
                            val updatedAlias = existingCanonical.copy(
                                aliasType = AliasType.CUSTOM,
                                updatedAt = Clock.System.now()
                            )
                            aliasRepository.update(updatedAlias)
                                .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                        } else {
                            Unit.right()
                        }
                    }

                if (removeResult.isLeft()) {
                    return removeResult.leftOrNull()!!.left()
                }

                // Create new canonical alias with the generated ID
                val newAlias = ScopeAlias.createCanonicalWithId(
                    id = aliasId,
                    scopeId = scopeId,
                    aliasName = aliasName
                )

                return aliasRepository.save(newAlias)
                    .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                    .map { newAlias }
            }

            // If name collision occurred, continue to next iteration
            // The loop will generate a new ID and try again
        }

        // If all retries exhausted, return an error
        return ScopeAliasError.DuplicateAlias(
            occurredAt = Clock.System.now(),
            aliasName = "Could not generate unique alias after $maxRetries attempts",
            existingScopeId = scopeId,
            attemptedScopeId = scopeId
        ).left()
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
    suspend fun assignCustomAlias(
        scopeId: ScopeId,
        aliasName: AliasName
    ): Either<ScopeAliasError, ScopeAlias> {
        return aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .flatMap { existingAlias ->
                when {
                    existingAlias != null -> {
                        ScopeAliasError.DuplicateAlias(
                            Clock.System.now(),
                            aliasName.value,
                            existingAlias.scopeId,
                            scopeId
                        ).left()
                    }
                    else -> {
                        val newAlias = ScopeAlias.createCustom(scopeId, aliasName)
                        aliasRepository.save(newAlias)
                            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                            .map { newAlias }
                    }
                }
            }
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
    suspend fun removeAlias(aliasName: AliasName): Either<ScopeAliasError, ScopeAlias> {
        return aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .flatMap { alias ->
                when {
                    alias == null -> {
                        ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value).left()
                    }
                    alias.isCanonical() -> {
                        ScopeAliasError.CannotRemoveCanonicalAlias(
                            Clock.System.now(),
                            alias.scopeId,
                            aliasName.value
                        ).left()
                    }
                    else -> {
                        aliasRepository.removeByAliasName(aliasName)
                            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
                            .map { alias }
                    }
                }
            }
    }

    /**
     * Resolves an alias to a scope ID.
     *
     * @param aliasName The alias name to resolve
     * @return Either an error or the scope ID
     */
    suspend fun resolveAlias(aliasName: AliasName): Either<ScopeAliasError, ScopeId> {
        return aliasRepository.findByAliasName(aliasName)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value) }
            .flatMap { alias ->
                when (alias) {
                    null -> ScopeAliasError.AliasNotFound(Clock.System.now(), aliasName.value).left()
                    else -> alias.scopeId.right()
                }
            }
    }

    /**
     * Gets all aliases for a scope.
     *
     * @param scopeId The scope ID
     * @return Either an error or the list of aliases
     */
    suspend fun getAliasesForScope(scopeId: ScopeId): Either<ScopeAliasError, List<ScopeAlias>> {
        return aliasRepository.findByScopeId(scopeId)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), scopeId.value) }
    }

    /**
     * Finds aliases that start with a given prefix.
     * Used for tab completion and partial matching.
     *
     * @param prefix The prefix to match
     * @param limit Maximum number of results
     * @return Either an error or the list of matching aliases
     */
    suspend fun findAliasesByPrefix(prefix: String, limit: Int = 50): Either<ScopeAliasError, List<ScopeAlias>> {
        return aliasRepository.findByAliasNamePrefix(prefix, limit)
            .mapLeft { ScopeAliasError.AliasNotFound(Clock.System.now(), prefix) }
    }
}

