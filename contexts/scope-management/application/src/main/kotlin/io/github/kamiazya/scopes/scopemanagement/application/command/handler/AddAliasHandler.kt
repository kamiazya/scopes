package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.AddAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName

/**
 * Handler for adding custom aliases to scopes.
 *
 * Note: This handler returns contract errors directly as part of the
 * architecture simplification to eliminate duplicate error definitions.
 */
class AddAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<AddAliasCommand, ScopeContractError, Unit> {

    override suspend operator fun invoke(input: AddAliasCommand): Either<ScopeContractError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Adding alias to scope",
                mapOf(
                    "existingAlias".to(input.existingAlias),
                    "newAlias".to(input.newAlias),
                ),
            )

            // Validate existingAlias
            val existingAliasName = AliasName.create(input.existingAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid existing alias name",
                        mapOf(
                            "existingAlias" to input.existingAlias,
                            "error" to error.toString(),
                        ),
                    )
                    applicationErrorMapper.mapDomainError(
                        error,
                        ErrorMappingContext(attemptedValue = input.existingAlias),
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
                                "existingAlias" to input.existingAlias,
                                "error" to error.toString(),
                            ),
                        )
                        applicationErrorMapper.mapToContractError(error)
                    }
                    .bind(),
            ) {
                logger.warn(
                    "Existing alias not found",
                    mapOf("existingAlias" to input.existingAlias),
                )
                ScopeContractError.BusinessError.AliasNotFound(alias = input.existingAlias)
            }

            val scopeId = alias.scopeId

            // Validate newAlias
            val newAliasName = AliasName.create(input.newAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid new alias name",
                        mapOf(
                            "newAlias" to input.newAlias,
                            "error" to error.toString(),
                        ),
                    )
                    applicationErrorMapper.mapDomainError(
                        error,
                        ErrorMappingContext(attemptedValue = input.newAlias),
                    )
                }
                .bind()

            // Add alias through application service
            scopeAliasService.createCustomAlias(scopeId, newAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to add alias",
                        mapOf(
                            "existingAlias" to input.existingAlias,
                            "newAlias" to input.newAlias,
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
                    "existingAlias" to input.existingAlias,
                    "newAlias" to input.newAlias,
                    "scopeId" to scopeId.value,
                ),
            )
        }
    }
}
