package io.github.kamiazya.scopes.application.usecase.handler

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.application.dto.ScopeAliasResult
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.mapper.ScopeAliasMapper
import io.github.kamiazya.scopes.application.logging.Logger
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.usecase.UseCase
import io.github.kamiazya.scopes.application.usecase.command.AssignCanonicalAlias
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.valueobject.AliasName
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.ScopeAliasError as DomainScopeAliasError

/**
 * Handler for assigning a canonical alias to a scope.
 *
 * Implements the UseCase interface following Clean Architecture principles.
 * Each handler has a single responsibility and a single invoke method.
 */
class AssignCanonicalAliasHandler(
    private val aliasManagementService: ScopeAliasManagementService,
    private val transactionManager: TransactionManager,
    private val logger: Logger
) : UseCase<AssignCanonicalAlias, ApplicationError, ScopeAliasResult> {

    override suspend operator fun invoke(input: AssignCanonicalAlias): Either<ApplicationError, ScopeAliasResult> = either {
        logger.info("Assigning canonical alias", mapOf(
            "scopeId" to input.scopeId,
            "aliasName" to input.aliasName
        ))

        transactionManager.inTransaction {
            either {
                // Parse and validate scope ID
                logger.debug("Parsing scope ID", mapOf("scopeId" to input.scopeId))
                val scopeId = ScopeId.create(input.scopeId)
                    .mapLeft { idError ->
                        logger.warn("Invalid scope ID", mapOf(
                            "scopeId" to input.scopeId,
                            "error" to (idError::class.simpleName ?: "Unknown")
                        ))
                        when(idError) {
                            is ScopeInputError.IdError.Blank -> ApplicationError.ScopeInputError.IdBlank(idError.attemptedValue)
                            is ScopeInputError.IdError.InvalidFormat -> ApplicationError.ScopeInputError.IdInvalidFormat(idError.attemptedValue, "ULID")
                        }
                    }.bind()

                // Parse and validate alias name
                logger.debug("Parsing alias name", mapOf("aliasName" to input.aliasName))
                val aliasName = AliasName.create(input.aliasName)
                    .mapLeft { aliasError ->
                        logger.warn("Invalid alias name", mapOf(
                            "aliasName" to input.aliasName,
                            "error" to (aliasError::class.simpleName ?: "Unknown")
                        ))
                        when(aliasError) {
                            is ScopeInputError.AliasError.Empty ->
                                ApplicationError.ScopeInputError.AliasEmpty(aliasError.attemptedValue)
                            is ScopeInputError.AliasError.TooShort ->
                                ApplicationError.ScopeInputError.AliasTooShort(aliasError.attemptedValue, aliasError.minimumLength)
                            is ScopeInputError.AliasError.TooLong ->
                                ApplicationError.ScopeInputError.AliasTooLong(aliasError.attemptedValue, aliasError.maximumLength)
                            is ScopeInputError.AliasError.InvalidFormat ->
                                ApplicationError.ScopeInputError.AliasInvalidFormat(aliasError.attemptedValue, aliasError.expectedPattern)
                        }
                    }.bind()

                // Assign canonical alias
                logger.debug("Assigning canonical alias", mapOf(
                    "scopeId" to scopeId.value,
                    "aliasName" to aliasName.value
                ))
                val alias = aliasManagementService.assignCanonicalAlias(scopeId, aliasName)
                    .mapLeft { aliasServiceError ->
                        logger.error("Failed to assign canonical alias", mapOf(
                            "scopeId" to scopeId.value,
                            "aliasName" to aliasName.value,
                            "error" to (aliasServiceError::class.simpleName ?: "Unknown")
                        ))
                        when (aliasServiceError) {
                            is DomainScopeAliasError.DuplicateAlias ->
                                ApplicationError.ScopeAliasError.DuplicateAlias(
                                    aliasServiceError.aliasName,
                                    aliasServiceError.existingScopeId.value,
                                    aliasServiceError.attemptedScopeId.value
                                )
                            is DomainScopeAliasError.AliasNotFound ->
                                ApplicationError.ScopeAliasError.AliasNotFound(
                                    aliasServiceError.aliasName
                                )
                            is DomainScopeAliasError.CannotRemoveCanonicalAlias ->
                                ApplicationError.ScopeAliasError.CannotRemoveCanonicalAlias(
                                    aliasServiceError.scopeId.value,
                                    aliasServiceError.canonicalAlias
                                )
                            is DomainScopeAliasError.CanonicalAliasAlreadyExists ->
                                ApplicationError.ScopeAliasError.DuplicateAlias(
                                    aliasServiceError.existingCanonicalAlias,
                                    aliasServiceError.scopeId.value,
                                    aliasServiceError.scopeId.value
                                )
                        }
                    }.bind()

                logger.info("Canonical alias assigned successfully", mapOf(
                    "scopeId" to scopeId.value,
                    "aliasId" to alias.id.value,
                    "aliasName" to alias.aliasName.value
                ))

                ScopeAliasMapper.toDto(alias)
            }
        }.bind()
    }.onLeft { error ->
        logger.error("Failed to assign canonical alias", mapOf(
            "error" to (error::class.simpleName ?: "Unknown"),
            "message" to error.toString()
        ))
    }
}

