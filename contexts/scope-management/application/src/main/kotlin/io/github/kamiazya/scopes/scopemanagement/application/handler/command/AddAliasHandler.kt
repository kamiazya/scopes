package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.AddAliasCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputErrorPresenter
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasApplicationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for adding custom aliases to scopes.
 */
class AddAliasHandler(
    private val scopeAliasService: ScopeAliasApplicationService,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : CommandHandler<AddAliasCommand, ScopesError, Unit> {
    private val errorPresenter = ScopeInputErrorPresenter()

    override suspend operator fun invoke(command: AddAliasCommand): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
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
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(command.existingAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(command.existingAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(command.existingAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(command.existingAlias, errorPresenter.presentAliasPattern(error.patternType))
                    }
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
                        error.toGenericApplicationError()
                    }
                    .bind(),
            ) {
                logger.warn(
                    "Existing alias not found",
                    mapOf("existingAlias" to command.existingAlias),
                )
                ScopeInputError.AliasNotFound(command.existingAlias)
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
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(command.newAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(command.newAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(command.newAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(command.newAlias, errorPresenter.presentAliasPattern(error.patternType))
                    }
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
                    error.toGenericApplicationError()
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
}
