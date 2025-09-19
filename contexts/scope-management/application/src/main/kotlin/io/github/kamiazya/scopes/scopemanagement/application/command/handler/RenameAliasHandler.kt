package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RenameAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for renaming existing aliases.
 * This operation is atomic and preserves the alias type (canonical/custom).
 * The rename is performed in a single transaction to prevent data loss.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class RenameAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<RenameAliasCommand, Unit>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: RenameAliasCommand): Either<ScopeManagementApplicationError, Unit> = either {
            logger.debug(
                "Renaming alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newAliasName" to command.newAliasName,
                ),
            )

            // Validate input parameters
            val currentAliasName = validateAliasName(command.currentAlias, "currentAlias").bind()
            val newAliasName = validateAliasName(command.newAliasName, "newAliasName").bind()

            // Find the current alias
            val currentAlias = findCurrentAlias(currentAliasName, command.currentAlias).bind()

            // Perform the atomic rename operation
            performAtomicRename(currentAlias, newAliasName, command).bind()

            logger.info(
                "Successfully renamed alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newAliasName" to command.newAliasName,
                    "scopeId" to currentAlias.scopeId.value,
                ),
            )
        }
    }

    private suspend fun validateAliasName(aliasName: String, fieldName: String): Either<ScopeManagementApplicationError, AliasName> =
        AliasName.create(aliasName).mapLeft { error ->
            logger.error(
                "Invalid alias name",
                mapOf(
                    "fieldName" to fieldName,
                    "aliasName" to aliasName,
                    "error" to error.toString(),
                ),
            )
            when (error) {
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.EmptyAlias ->
                    ScopeInputError.AliasEmpty(aliasName)
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.AliasTooShort ->
                    ScopeInputError.AliasTooShort(aliasName, error.minLength)
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.AliasTooLong ->
                    ScopeInputError.AliasTooLong(aliasName, error.maxLength)
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidAliasFormat ->
                    ScopeInputError.AliasInvalidFormat(aliasName, errorPresenter.presentAliasPattern(error.expectedPattern))
            }
        }

    private suspend fun findCurrentAlias(currentAliasName: AliasName, inputAlias: String): Either<ScopeManagementApplicationError, ScopeAlias> = either {
        val currentAlias = scopeAliasService.findAliasByName(currentAliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to find current alias",
                    mapOf(
                        "currentAlias" to inputAlias,
                        "error" to error.toString(),
                    ),
                )
                error
            }
            .bind()

        if (currentAlias == null) {
            logger.error(
                "Current alias not found",
                mapOf("currentAlias" to inputAlias),
            )
            raise(ScopeInputError.AliasNotFound(inputAlias))
        }

        currentAlias
    }

    private suspend fun performAtomicRename(
        currentAlias: ScopeAlias,
        newAliasName: AliasName,
        command: RenameAliasCommand,
    ): Either<ScopeManagementApplicationError, Unit> = either {
        // Check if new alias name is already taken
        val existingNewAlias = scopeAliasService.findAliasByName(newAliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to check new alias availability",
                    mapOf(
                        "newAlias" to command.newAliasName,
                        "error" to error.toString(),
                    ),
                )
                error
            }
            .bind()

        if (existingNewAlias != null) {
            logger.error(
                "New alias already exists",
                mapOf(
                    "newAlias" to command.newAliasName,
                    "existingScopeId" to existingNewAlias.scopeId.value,
                    "attemptedScopeId" to currentAlias.scopeId.value,
                ),
            )
            raise(
                io.github.kamiazya.scopes.scopemanagement.application.error.ScopeAliasError.AliasDuplicate(
                    aliasName = command.newAliasName,
                    existingScopeId = existingNewAlias.scopeId.value,
                    attemptedScopeId = currentAlias.scopeId.value,
                ),
            )
        }

        // Delete the old alias and create new one with same type
        scopeAliasService.deleteAlias(currentAlias.id)
            .mapLeft { error ->
                logger.error(
                    "Failed to delete old alias",
                    mapOf(
                        "currentAlias" to command.currentAlias,
                        "error" to error.toString(),
                    ),
                )
                error
            }
            .bind()

        // Create new alias preserving the type
        if (currentAlias.isCanonical()) {
            scopeAliasService.assignCanonicalAlias(currentAlias.scopeId, newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to create new canonical alias",
                        mapOf(
                            "newAlias" to command.newAliasName,
                            "scopeId" to currentAlias.scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error
                }
                .bind()
        } else {
            scopeAliasService.createCustomAlias(currentAlias.scopeId, newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to create new custom alias",
                        mapOf(
                            "newAlias" to command.newAliasName,
                            "scopeId" to currentAlias.scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error
                }
                .bind()
        }
    }
}
