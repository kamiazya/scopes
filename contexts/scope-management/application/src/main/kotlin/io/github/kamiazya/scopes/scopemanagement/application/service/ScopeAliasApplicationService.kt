package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.aggregate.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.ScopeAliasPolicy
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasOperation
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Application service for managing scope aliases.
 *
 * This service orchestrates between the domain policy and the repository,
 * handling all I/O operations while delegating business rules to the domain layer.
 */
class ScopeAliasApplicationService(
    private val aliasRepository: ScopeAliasRepository,
    private val aliasGenerationService: AliasGenerationService,
    private val aliasPolicy: ScopeAliasPolicy = ScopeAliasPolicy(),
) {

    /**
     * Assigns a canonical alias to a scope.
     *
     * @param scopeId The scope to assign the alias to
     * @param aliasName The alias name to assign
     * @return Either an error or the created/updated alias
     */
    suspend fun assignCanonicalAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopeManagementApplicationError, ScopeAlias> = either {
        // Fetch necessary data from repository
        val existingAliasWithName = aliasRepository.findByAliasName(aliasName).mapLeft { it.toGenericApplicationError() }.bind()
        val existingCanonicalForScope = aliasRepository.findCanonicalByScopeId(scopeId).mapLeft { it.toGenericApplicationError() }.bind()

        // Use pure domain service to determine operation
        val operation = aliasPolicy.determineCanonicalAliasOperation(
            scopeId = scopeId,
            aliasName = aliasName,
            existingAliasWithName = existingAliasWithName,
            existingCanonicalForScope = existingCanonicalForScope,
        )

        // Execute the operation
        when (operation) {
            is AliasOperation.Create -> {
                aliasRepository.save(operation.alias).mapLeft { it.toGenericApplicationError() }.bind()
                operation.alias
            }
            is AliasOperation.Replace -> {
                // Demote old canonical to custom using domain method
                val demotedAlias = operation.oldAlias.demoteToCustom(Clock.System.now())
                aliasRepository.update(demotedAlias).mapLeft { it.toGenericApplicationError() }.bind()

                // Save new canonical
                aliasRepository.save(operation.newAlias).mapLeft { it.toGenericApplicationError() }.bind()
                operation.newAlias
            }
            is AliasOperation.Promote -> {
                // Promote existing custom alias to canonical
                val promotedAlias = operation.existingAlias.promoteToCanonical(Clock.System.now())
                aliasRepository.update(promotedAlias).mapLeft { it.toGenericApplicationError() }.bind()

                // If there's an existing canonical, demote it to custom
                if (existingCanonicalForScope != null) {
                    val demotedAlias = existingCanonicalForScope.demoteToCustom(Clock.System.now())
                    aliasRepository.update(demotedAlias).mapLeft { it.toGenericApplicationError() }.bind()
                }

                promotedAlias
            }
            is AliasOperation.NoChange -> {
                // Return existing alias
                requireNotNull(existingCanonicalForScope) { "Expected existing alias for NoChange operation" }
            }
            is AliasOperation.Failure -> {
                raise(operation.error.toGenericApplicationError())
            }
        }
    }

    /**
     * Creates a custom alias for a scope.
     *
     * @param scopeId The scope to create alias for
     * @param aliasName The alias name to create
     * @return Either an error or the created alias
     */
    suspend fun createCustomAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopeManagementApplicationError, ScopeAlias> = either {
        // Check if alias name is available
        val existingAliasWithName = aliasRepository.findByAliasName(aliasName).mapLeft { it.toGenericApplicationError() }.bind()

        // Use pure domain service to validate
        val newAlias = aliasPolicy.validateCustomAliasCreation(
            scopeId = scopeId,
            aliasName = aliasName,
            existingAliasWithName = existingAliasWithName,
        ).mapLeft { it.toGenericApplicationError() }.bind()

        // Save to repository
        aliasRepository.save(newAlias).mapLeft { it.toGenericApplicationError() }.bind()
        newAlias
    }

    /**
     * Generates and assigns a canonical alias using Haikunator pattern.
     *
     * @param scopeId The scope to generate alias for
     * @param maxRetries Maximum number of retry attempts
     * @return Either an error or the generated alias
     */
    suspend fun generateCanonicalAlias(scopeId: ScopeId, maxRetries: Int = 10): Either<ScopeManagementApplicationError, ScopeAlias> = either {
        var retryCount = 0

        while (retryCount < maxRetries) {
            // Generate a new alias name
            val generatedName = aliasGenerationService.generateRandomAlias().mapLeft { it.toGenericApplicationError() }.bind()

            // Always fetch existing alias and canonical to properly evaluate all cases
            val existingAliasWithName = aliasRepository.findByAliasName(generatedName).mapLeft { it.toGenericApplicationError() }.bind()
            val existingCanonical = aliasRepository.findCanonicalByScopeId(scopeId).mapLeft { it.toGenericApplicationError() }.bind()

            // Let the policy decide the appropriate operation with full information
            val operation = aliasPolicy.determineCanonicalAliasOperation(
                scopeId = scopeId,
                aliasName = generatedName,
                existingAliasWithName = existingAliasWithName,
                existingCanonicalForScope = existingCanonical,
            )

            when (operation) {
                is AliasOperation.Create -> {
                    // Terminal case: create new alias
                    aliasRepository.save(operation.alias).mapLeft { it.toGenericApplicationError() }.bind()
                    return@either operation.alias
                }
                is AliasOperation.Replace -> {
                    // Terminal case: replace existing canonical
                    val demotedAlias = operation.oldAlias.demoteToCustom(Clock.System.now())
                    aliasRepository.update(demotedAlias).mapLeft { it.toGenericApplicationError() }.bind()
                    aliasRepository.save(operation.newAlias).mapLeft { it.toGenericApplicationError() }.bind()
                    return@either operation.newAlias
                }
                is AliasOperation.Promote -> {
                    // Terminal case: promote existing custom alias to canonical
                    val promotedAlias = operation.existingAlias.promoteToCanonical(Clock.System.now())
                    aliasRepository.update(promotedAlias).mapLeft { it.toGenericApplicationError() }.bind()

                    if (existingCanonical != null) {
                        val demotedAlias = existingCanonical.demoteToCustom(Clock.System.now())
                        aliasRepository.update(demotedAlias).mapLeft { it.toGenericApplicationError() }.bind()
                    }

                    return@either promotedAlias
                }
                is AliasOperation.NoChange -> {
                    // Terminal case: generated name matches existing canonical
                    return@either requireNotNull(existingCanonical) {
                        "Expected existing canonical alias for NoChange operation"
                    }
                }
                is AliasOperation.Failure -> {
                    // Check if this is a cross-scope conflict (retry case)
                    if (existingAliasWithName != null && existingAliasWithName.scopeId != scopeId) {
                        // Cross-scope conflict: retry with a new name
                        retryCount++
                        continue
                    }
                    // Other failures are terminal
                    raise(operation.error.toGenericApplicationError())
                }
            }
        }

        raise(
            ScopeAliasError.AliasGenerationFailed(
                scopeId = scopeId.toString(),
                retryCount = maxRetries,
            ),
        )
    }

    /**
     * Deletes an alias.
     *
     * @param aliasId The ID of the alias to delete
     * @return Either an error or Unit
     */
    suspend fun deleteAlias(aliasId: AliasId): Either<ScopeManagementApplicationError, Unit> = either {
        val alias = ensureNotNull(aliasRepository.findById(aliasId).mapLeft { it.toGenericApplicationError() }.bind()) {
            ScopeAliasError.AliasNotFound(
                aliasName = "ID:${aliasId.value}",
            )
        }

        // Check if this is a canonical alias - they cannot be deleted
        ensure(!alias.isCanonical()) {
            ScopeAliasError.CannotRemoveCanonicalAlias(
                scopeId = alias.scopeId.toString(),
                aliasName = alias.aliasName.value,
            )
        }

        // Delete from repository and ensure it was actually removed
        val removed = aliasRepository.removeById(alias.id).mapLeft { it.toGenericApplicationError() }.bind()
        ensure(removed) {
            ScopeAliasError.AliasNotFound(
                aliasName = alias.aliasName.value,
            )
        }
    }

    /**
     * Lists all aliases for a scope.
     *
     * @param scopeId The scope ID to list aliases for
     * @return Either an error or list of aliases
     */
    suspend fun listAliasesForScope(scopeId: ScopeId): Either<ScopeManagementApplicationError, List<ScopeAlias>> =
        aliasRepository.findByScopeId(scopeId).mapLeft { it.toGenericApplicationError() }

    /**
     * Finds an alias by name.
     *
     * @param aliasName The alias name to search for
     * @return Either an error or the alias (if found)
     */
    suspend fun findAliasByName(aliasName: AliasName): Either<ScopeManagementApplicationError, ScopeAlias?> =
        aliasRepository.findByAliasName(aliasName).mapLeft { it.toGenericApplicationError() }
}
