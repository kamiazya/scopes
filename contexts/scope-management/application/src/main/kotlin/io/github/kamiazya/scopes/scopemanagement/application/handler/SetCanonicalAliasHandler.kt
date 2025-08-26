package io.github.kamiazya.scopes.scopemanagement.application.handler

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.SetCanonicalAlias
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeInputError
import io.github.kamiazya.scopes.scopemanagement.application.error.toApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
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
                    "aliasName" to command.aliasName,
                    "scopeId" to command.scopeId,
                ),
            )

            // Validate scopeId
            val scopeId = ScopeId.create(command.scopeId)
                .mapLeft { error ->
                    logger.error(
                        "Invalid scope ID",
                        mapOf(
                            "scopeId" to command.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    when (error) {
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.IdError.Blank ->
                            ScopeInputError.IdBlank(command.scopeId)
                        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeInputError.IdError.InvalidFormat ->
                            ScopeInputError.IdInvalidFormat(command.scopeId, "ULID")
                    }
                }
                .bind()

            // Validate aliasName
            val aliasName = AliasName.create(command.aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Invalid alias name",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    ScopeInputError.InvalidAlias(command.aliasName)
                }
                .bind()

            // Find the alias and verify it belongs to the scope
            val alias = aliasRepository.findByAliasName(aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to find alias",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            if (alias == null) {
                logger.error(
                    "Alias not found",
                    mapOf("aliasName" to command.aliasName),
                )
                return@either ScopeInputError.AliasNotFound(command.aliasName).left().bind()
            }

            // Verify the alias belongs to the specified scope
            if (alias.scopeId != scopeId) {
                logger.error(
                    "Alias belongs to different scope",
                    mapOf(
                        "aliasName" to command.aliasName,
                        "aliasScopeId" to alias.scopeId.value,
                        "requestedScopeId" to command.scopeId,
                    ),
                )
                return@either ScopeInputError.InvalidAlias(command.aliasName).left().bind()
            }

            // If already canonical, return success (idempotent)
            if (alias.isCanonical()) {
                logger.info(
                    "Alias is already canonical",
                    mapOf(
                        "aliasName" to command.aliasName,
                        "scopeId" to command.scopeId,
                    ),
                )
                return@either Unit
            }

            // Set as canonical (service handles the demotion logic)
            scopeAliasService.assignCanonicalAlias(scopeId, aliasName)
                .mapLeft { error ->
                    logger.error(
                        "Failed to set canonical alias",
                        mapOf(
                            "aliasName" to command.aliasName,
                            "scopeId" to command.scopeId,
                            "error" to error.toString(),
                        ),
                    )
                    error.toApplicationError()
                }
                .bind()

            logger.info(
                "Successfully set canonical alias",
                mapOf(
                    "aliasName" to command.aliasName,
                    "scopeId" to command.scopeId,
                ),
            )
        }
    }
}
