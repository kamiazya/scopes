package io.github.kamiazya.scopes.scopemanagement.application.service

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeAliasError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.PureScopeAliasValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasId
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasOperation
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Application service for managing scope aliases.
 *
 * This service orchestrates between the pure domain service and the repository,
 * handling all I/O operations while delegating business logic to the domain layer.
 */
class ScopeAliasApplicationService(
    private val aliasRepository: ScopeAliasRepository,
    private val aliasGenerationService: AliasGenerationService,
    private val validationService: PureScopeAliasValidationService = PureScopeAliasValidationService(),
) {

    /**
     * Assigns a canonical alias to a scope.
     *
     * @param scopeId The scope to assign the alias to
     * @param aliasName The alias name to assign
     * @return Either an error or the created/updated alias
     */
    suspend fun assignCanonicalAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopesError, ScopeAlias> = either {
        // Fetch necessary data from repository
        val existingAliasWithName = aliasRepository.findByAliasName(aliasName).bind()
        val existingCanonicalForScope = aliasRepository.findCanonicalByScopeId(scopeId).bind()

        // Use pure domain service to determine operation
        val operation = validationService.determineCanonicalAliasOperation(
            scopeId = scopeId,
            aliasName = aliasName,
            existingAliasWithName = existingAliasWithName,
            existingCanonicalForScope = existingCanonicalForScope,
        )

        // Execute the operation
        when (operation) {
            is AliasOperation.Create -> {
                aliasRepository.save(operation.alias).bind()
                operation.alias
            }
            is AliasOperation.Replace -> {
                // Convert old canonical to custom
                val updatedOld = operation.oldAlias.copy(
                    aliasType = AliasType.CUSTOM,
                    updatedAt = Clock.System.now(),
                )
                aliasRepository.update(updatedOld).bind()

                // Save new canonical
                aliasRepository.save(operation.newAlias).bind()
                operation.newAlias
            }
            is AliasOperation.NoChange -> {
                // Return existing alias
                existingCanonicalForScope!!
            }
            is AliasOperation.Error -> {
                raise(operation.error)
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
    suspend fun createCustomAlias(scopeId: ScopeId, aliasName: AliasName): Either<ScopesError, ScopeAlias> = either {
        // Check if alias name is available
        val existingAliasWithName = aliasRepository.findByAliasName(aliasName).bind()

        // Use pure domain service to validate
        val newAlias = validationService.validateCustomAliasCreation(
            scopeId = scopeId,
            aliasName = aliasName,
            existingAliasWithName = existingAliasWithName,
        ).bind()

        // Save to repository
        aliasRepository.save(newAlias).bind()
        newAlias
    }

    /**
     * Generates and assigns a canonical alias using Haikunator pattern.
     *
     * @param scopeId The scope to generate alias for
     * @param maxRetries Maximum number of retry attempts
     * @return Either an error or the generated alias
     */
    suspend fun generateCanonicalAlias(scopeId: ScopeId, maxRetries: Int = 10): Either<ScopesError, ScopeAlias> = either {
        var retryCount = 0

        while (retryCount < maxRetries) {
            // Generate a new alias name
            val generatedName = aliasGenerationService.generateRandomAlias().bind()

            // Check if it's available
            val existingAliasWithName = aliasRepository.findByAliasName(generatedName).bind()

            if (existingAliasWithName == null) {
                // Name is available, create the alias
                val existingCanonical = aliasRepository.findCanonicalByScopeId(scopeId).bind()

                val operation = validationService.determineCanonicalAliasOperation(
                    scopeId = scopeId,
                    aliasName = generatedName,
                    existingAliasWithName = null,
                    existingCanonicalForScope = existingCanonical,
                )

                return@either when (operation) {
                    is AliasOperation.Create -> {
                        aliasRepository.save(operation.alias).bind()
                        operation.alias
                    }
                    is AliasOperation.Replace -> {
                        // Delete old canonical
                        aliasRepository.removeByAliasName(operation.oldAlias.aliasName).bind()
                        // Save new canonical
                        aliasRepository.save(operation.newAlias).bind()
                        operation.newAlias
                    }
                    else -> {
                        raise(
                            ScopeAliasError.AliasGenerationFailed(
                                occurredAt = Clock.System.now(),
                                scopeId = scopeId,
                                retryCount = retryCount,
                            ),
                        )
                    }
                }
            }

            retryCount++
        }

        raise(
            ScopeAliasError.AliasGenerationFailed(
                occurredAt = Clock.System.now(),
                scopeId = scopeId,
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
    suspend fun deleteAlias(aliasId: AliasId): Either<ScopesError, Unit> = either {
        val alias = aliasRepository.findById(aliasId).bind()

        if (alias == null) {
            raise(
                ScopeAliasError.AliasNotFound(
                    occurredAt = Clock.System.now(),
                    aliasName = aliasId.value,
                ),
            )
        }

        // Check if this is the last alias for the scope
        val allAliasesForScope = aliasRepository.findByScopeId(alias.scopeId).bind()
        val isLastAlias = allAliasesForScope.size == 1

        // Validate deletion
        validationService.validateAliasDeletion(alias, isLastAlias).bind()

        // Delete from repository
        aliasRepository.removeByAliasName(alias.aliasName).bind()
    }

    /**
     * Lists all aliases for a scope.
     *
     * @param scopeId The scope ID to list aliases for
     * @return Either an error or list of aliases
     */
    suspend fun listAliasesForScope(scopeId: ScopeId): Either<ScopesError, List<ScopeAlias>> = aliasRepository.findByScopeId(scopeId)

    /**
     * Finds an alias by name.
     *
     * @param aliasName The alias name to search for
     * @return Either an error or the alias (if found)
     */
    suspend fun findAliasByName(aliasName: AliasName): Either<ScopesError, ScopeAlias?> = aliasRepository.findByAliasName(aliasName)
}
