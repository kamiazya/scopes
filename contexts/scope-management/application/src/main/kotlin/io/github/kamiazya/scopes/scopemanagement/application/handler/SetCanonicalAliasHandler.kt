package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.SetCanonicalAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.application.error.ApplicationError as ScopesError

/**
 * Handler for setting a canonical alias for a scope.
 * Automatically demotes the previous canonical alias to a custom alias.
 */
class SetCanonicalAliasHandler(
    private val scopeAliasService: ScopeAliasManagementService,
    private val aliasRepository: ScopeAliasRepository,
    private val transactionManager: TransactionManager,
    private val logger: Logger,
) : UseCase<SetCanonicalAlias, ScopesError, Unit> {

    override suspend operator fun invoke(command: SetCanonicalAlias): Either<ScopesError, Unit> = transactionManager.inTransaction {
        either {
            logger.debug(
                "Setting canonical alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newCanonicalAlias" to command.newCanonicalAlias,
                ),
            )

            // Validate currentAlias
            val currentAliasName = AliasName.create(command.currentAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid current alias name",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(command.currentAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(command.currentAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(command.currentAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(command.currentAlias, error.expectedPattern)
                    }
                }
                .bind()

            // Find the current alias to identify the scope
            val currentAliasEntity = aliasRepository.findByAliasName(currentAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find current alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (currentAliasEntity == null) {
                logger.error(
                    "Current alias not found",
                    mapOf("currentAlias" to command.currentAlias),
                )
                raise(ScopeInputError.AliasNotFound(command.currentAlias))
            }

            val scopeId = currentAliasEntity.scopeId

            // Validate newCanonicalAlias
            val newCanonicalAliasName = AliasName.create(command.newCanonicalAlias)
                .mapLeft { error ->
                    logger.error(
                        "Invalid new canonical alias name",
                        mapOf(
                            "newCanonicalAlias" to command.newCanonicalAlias,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.Empty ->
                            ScopeInputError.AliasEmpty(command.newCanonicalAlias)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooShort ->
                            ScopeInputError.AliasTooShort(command.newCanonicalAlias, error.minimumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.TooLong ->
                            ScopeInputError.AliasTooLong(command.newCanonicalAlias, error.maximumLength)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.AliasError.InvalidFormat ->
                            ScopeInputError.AliasInvalidFormat(command.newCanonicalAlias, error.expectedPattern)
                    }
                }
                .bind()

            // Find the new canonical alias and verify it belongs to the same scope
            val newCanonicalAliasEntity = aliasRepository.findByAliasName(newCanonicalAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find new canonical alias",
                        mapOf(
                            "newCanonicalAlias" to command.newCanonicalAlias,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (newCanonicalAliasEntity == null) {
                logger.error(
                    "New canonical alias not found",
                    mapOf("newCanonicalAlias" to command.newCanonicalAlias),
                )
                raise(ScopeInputError.AliasNotFound(command.newCanonicalAlias))
            }

            // Verify the new canonical alias belongs to the same scope
            if (newCanonicalAliasEntity.scopeId != scopeId) {
                logger.error(
                    "New canonical alias belongs to different scope",
                    mapOf(
                        "currentAlias" to command.currentAlias,
                        "newCanonicalAlias" to command.newCanonicalAlias,
                        "currentAliasScope" to scopeId.value,
                        "newCanonicalAliasScope" to newCanonicalAliasEntity.scopeId.value,
                    ),
                )
                raise(ScopeInputError.InvalidAlias(command.newCanonicalAlias))
            }

            // If already canonical, return success (idempotent)
            if (newCanonicalAliasEntity.isCanonical()) {
                logger.info(
                    "Alias is already canonical",
                    mapOf(
                        "newCanonicalAlias" to command.newCanonicalAlias,
                        "scopeId" to scopeId.value,
                    ),
                )
                return@either Unit
            }

            // Set as canonical (service handles the demotion logic)
            scopeAliasService.assignCanonicalAlias(scopeId, newCanonicalAliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to set canonical alias",
                        mapOf(
                            "currentAlias" to command.currentAlias,
                            "newCanonicalAlias" to command.newCanonicalAlias,
                            "scopeId" to scopeId.value,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            logger.info(
                "Successfully set canonical alias",
                mapOf(
                    "currentAlias" to command.currentAlias,
                    "newCanonicalAlias" to command.newCanonicalAlias,
                    "scopeId" to scopeId.value,
                ),
            )
        }
    }
}
