package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
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
) : UseCase<AddAlias, ScopesError, Unit> {

    override suspend operator fun invoke(input: AddAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Adding alias to scope",
                mapOf(
                    "existingAlias" to input.existingAlias,
                    "newAlias" to input.newAlias,
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
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(input.existingAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(input.existingAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(input.existingAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(input.existingAlias, error.expectedPattern)
                    }
                }
                .bind()

            // Find the scope ID through application service
            val alias = scopeAliasService.findAliasByName(existingAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find existing alias",
                        mapOf(
                            "existingAlias" to input.existingAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toGenericApplicationError()
                }
                .bind()

            if (alias == null) {
                logger.warn(
                    "Existing alias not found",
                    mapOf("existingAlias" to input.existingAlias),
                )
                raise(ScopeInputError.AliasNotFound(input.existingAlias))
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
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(input.newAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(input.newAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(input.newAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(input.newAlias, error.expectedPattern)
                    }
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
                    error.toGenericApplicationError()
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
