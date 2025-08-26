package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.AddAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for adding custom aliases to scopes.
 */
class AddAliasHandler(
    private val scopeAliasService: ScopeAliasManagementService,
    private val aliasRepository: ScopeAliasRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<AddAlias, ScopesError, Unit> {

    override suspend operator fun invoke(command: AddAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
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
                            ScopeInputError.AliasInvalidFormat(command.existingAlias, error.expectedPattern)
                    }
                }
                .bind()

            // Find the existing alias to identify the scope
            val existingAliasEntity = aliasRepository.findByAliasName(existingAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find existing alias",
                        mapOf(
                            "existingAlias" to command.existingAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (existingAliasEntity == null) {
                logger.error(
                    "Existing alias not found",
                    mapOf("existingAlias" to command.existingAlias),
                )
                raise(ScopeInputError.AliasNotFound(command.existingAlias))
            }

            val scopeId = existingAliasEntity.scopeId

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
                            ScopeInputError.AliasInvalidFormat(command.newAlias, error.expectedPattern)
                    }
                }
                .bind()

            // Add alias through domain service
            scopeAliasService.assignCustomAlias(scopeId, newAliasName)
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
                    error.toApplicationError()
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
