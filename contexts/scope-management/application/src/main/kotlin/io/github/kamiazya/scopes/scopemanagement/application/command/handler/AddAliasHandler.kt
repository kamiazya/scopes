package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.AddAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for adding custom aliases to scopes.
 * Uses BaseCommandHandler for common functionality and ApplicationErrorMapper
 * for error mapping to contract errors.
 */
class AddAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val applicationErrorMapper: ApplicationErrorMapper,
    transactionManager: TransactionManager,
    logger: Logger,
) : BaseCommandHandler<AddAliasCommand, Unit>(transactionManager, logger) {

    override suspend fun executeCommand(command: AddAliasCommand): Either<ScopeContractError, Unit> = either {
        logger.debug(
            "Adding alias to scope",
            mapOf(
                "existingAlias" to command.existingAlias,
                "newAlias" to command.newAlias,
            ),
        )

        // Validate existingAlias
        val existingAliasName = AliasName.create(command.existingAlias)
            .mapLeft { error ->
                logger.error(
                    "Invalid existing alias name",
                    mapOf(
                        "existingAlias" to command.existingAlias,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(
                    error,
                    ErrorMappingContext(attemptedValue = command.existingAlias),
                )
            }
            .bind()

        // Find the scope ID through application service
        val alias = ensureNotNull(
            scopeAliasService.findAliasByName(existingAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find existing alias",
                        mapOf(
                            "existingAlias" to command.existingAlias,
                            "error" to error.toString(),
                        ),
                    )
                    applicationErrorMapper.mapToContractError(error)
                }
                .bind(),
        ) {
            logger.warn(
                "Existing alias not found",
                mapOf("existingAlias" to command.existingAlias),
            )
            ScopeContractError.BusinessError.AliasNotFound(alias = command.existingAlias)
        }

        val scopeId = alias.scopeId

        // Validate newAlias
        val newAliasName = AliasName.create(command.newAlias)
            .mapLeft { error ->
                logger.error(
                    "Invalid new alias name",
                    mapOf(
                        "newAlias" to command.newAlias,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapDomainError(
                    error,
                    ErrorMappingContext(attemptedValue = command.newAlias),
                )
            }
            .bind()

        // Add alias through application service
        scopeAliasService.createCustomAlias(scopeId, newAliasName)
            .mapLeft { error ->
                logger.error(
                    "Failed to add alias",
                    mapOf(
                        "existingAlias" to command.existingAlias,
                        "newAlias" to command.newAlias,
                        "scopeId" to scopeId.value,
                        "error" to error.toString(),
                    ),
                )
                applicationErrorMapper.mapToContractError(error)
            }
            .bind()

        logger.info(
            "Successfully added alias",
            mapOf(
                "existingAlias" to command.existingAlias,
                "newAlias" to command.newAlias,
                "scopeId" to scopeId.value,
            ),
        )
    }
}
