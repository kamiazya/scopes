package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.RenameAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.command.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for renaming existing aliases.
 * This operation is atomic and preserves the alias type (canonical/custom).
 * The rename is performed in a single transaction to prevent data loss.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 */
class RenameAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<RenameAliasCommand, Unit>(transactionManager, logger) {

    override suspend fun executeCommand(command: RenameAliasCommand): Either<ScopeContractError, Unit> = either {
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

    private suspend fun validateAliasName(aliasName: String, fieldName: String): Either<ScopeContractError, AliasName> =
        AliasName.create(aliasName).mapLeft { error ->
            logger.error(
                "Invalid alias name",
                mapOf(
                    "fieldName" to fieldName,
                    "aliasName" to aliasName,
                    "error" to error.toString(),
                ),
            )
            applicationErrorMapper.mapDomainError(
                error,
                ErrorMappingContext(attemptedValue = aliasName),
            )
        }

    private suspend fun findCurrentAlias(currentAliasName: AliasName, inputAlias: String): Either<ScopeContractError, ScopeAlias> = either {
        val currentAlias = scopeAliasService.findAliasByName(currentAliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to find current alias",
                    mapOf(
                        "currentAlias" to inputAlias,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapToContractError(error)
            }
            .bind()

        if (currentAlias == null) {
            logger.error(
                "Current alias not found",
                mapOf("currentAlias" to inputAlias),
            )
            raise(ScopeContractError.BusinessError.AliasNotFound(inputAlias))
        }

        currentAlias
    }

    private suspend fun performAtomicRename(currentAlias: ScopeAlias, newAliasName: AliasName, command: RenameAliasCommand): Either<ScopeContractError, Unit> =
        either {
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
                    applicationErrorMapper.mapToContractError(error)
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
                    ScopeContractError.BusinessError.DuplicateAlias(
                        alias = command.newAliasName,
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
                    applicationErrorMapper.mapToContractError(error)
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
                        applicationErrorMapper.mapToContractError(error)
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
                        applicationErrorMapper.mapToContractError(error)
                    }
                    .bind()
            }
        }
}
