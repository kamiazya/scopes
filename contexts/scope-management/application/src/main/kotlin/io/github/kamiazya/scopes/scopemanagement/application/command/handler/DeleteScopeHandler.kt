package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ErrorMappingContext
import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for deleting a scope.
 *
 * Note: This handler returns contract errors directly as part of the
 * architecture simplification to eliminate duplicate error definitions.
 */
class DeleteScopeHandler(
    private val scopeRepository: ScopeRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
    private val logger: Logger,
) : CommandHandler<DeleteScopeCommand, ScopeContractError, Unit> {

    override suspend operator fun invoke(command: DeleteScopeCommand): Either<ScopeContractError, Unit> = either {
        logger.info(
            "Deleting scope",
            mapOf(
                "scopeId" to command.id,
                "cascade" to command.cascade.toString(),
            ),
        )

        transactionManager.inTransaction {
            either {
                val scopeId = ScopeId.create(command.id).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(
                        error,
                        ErrorMappingContext(attemptedValue = command.id),
                    )
                }.bind()
                validateScopeExists(scopeId).bind()
                handleChildrenDeletion(scopeId, command.cascade).bind()
                scopeRepository.deleteById(scopeId).mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }.bind()
                logger.info("Scope deleted successfully", mapOf("scopeId" to scopeId.value))
            }
        }.bind()
    }.onLeft { error ->
        logger.error(
            "Failed to delete scope",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }

    private suspend fun validateScopeExists(scopeId: ScopeId): Either<ScopeContractError, Unit> = either {
        val existingScope = scopeRepository.findById(scopeId).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error)
        }.bind()
        ensure(existingScope != null) {
            logger.warn("Scope not found for deletion", mapOf("scopeId" to scopeId.value))
            ScopeContractError.BusinessError.NotFound(scopeId = scopeId.value)
        }
    }

    private suspend fun handleChildrenDeletion(scopeId: ScopeId, cascade: Boolean): Either<ScopeContractError, Unit> = either {
        val allChildren = fetchAllChildren(scopeId).bind()

        if (allChildren.isNotEmpty()) {
            if (cascade) {
                logger.debug(
                    "Cascade deleting children",
                    mapOf(
                        "parentId" to scopeId.value,
                        "childCount" to allChildren.size.toString(),
                    ),
                )
                for (child in allChildren) {
                    deleteRecursive(child.id).bind()
                }
            } else {
                logger.warn(
                    "Cannot delete scope with children",
                    mapOf(
                        "scopeId" to scopeId.value,
                        "childCount" to allChildren.size.toString(),
                    ),
                )
                raise(
                    ScopeContractError.BusinessError.HasChildren(
                        scopeId = scopeId.value,
                        childrenCount = allChildren.size,
                    ),
                )
            }
        }
    }

    private suspend fun deleteRecursive(scopeId: ScopeId): Either<ScopeContractError, Unit> = either {
        // Find all children of this scope using proper pagination
        val allChildren = fetchAllChildren(scopeId).bind()

        // Recursively delete all children
        for (child in allChildren) {
            deleteRecursive(child.id).bind()
        }

        // Delete this scope
        scopeRepository.deleteById(scopeId).mapLeft { error ->
            applicationErrorMapper.mapDomainError(error)
        }.bind()
        logger.debug("Recursively deleted scope", mapOf("scopeId" to scopeId.value))
    }

    /**
     * Fetch all children of a scope using pagination to avoid the limit of 1000.
     * This ensures complete cascade deletion without leaving orphaned records.
     */
    private suspend fun fetchAllChildren(parentId: ScopeId): Either<ScopeContractError, List<Scope>> = either {
        val allChildren = mutableListOf<Scope>()
        var offset = 0
        val batchSize = 1000

        do {
            val batch = scopeRepository.findByParentId(parentId, offset = offset, limit = batchSize)
                .mapLeft { error ->
                    applicationErrorMapper.mapDomainError(error)
                }.bind()

            allChildren.addAll(batch)
            offset += batch.size

            logger.debug(
                "Fetched children batch",
                mapOf(
                    "parentId" to parentId.value,
                    "batchSize" to batch.size.toString(),
                    "totalSoFar" to allChildren.size.toString(),
                ),
            )
        } while (batch.size == batchSize) // Continue if we got a full batch

        allChildren
    }
}
