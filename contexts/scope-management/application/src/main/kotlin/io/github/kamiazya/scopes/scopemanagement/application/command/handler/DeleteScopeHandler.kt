package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeHierarchyApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for deleting a scope.
 */
class DeleteScopeHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    CommandHandler<DeleteScopeCommand, ScopeManagementApplicationError, Unit> {

    override suspend operator fun invoke(command: DeleteScopeCommand): Either<ScopeManagementApplicationError, Unit> = either {
        logger.info(
            "Deleting scope",
            mapOf(
                "scopeId" to command.id,
                "cascade" to command.cascade.toString(),
            ),
        )

        transactionManager.inTransaction {
            either {
                val scopeId = ScopeId.create(command.id).mapLeft { it.toGenericApplicationError() }.bind()
                validateScopeExists(scopeId).bind()
                handleChildrenDeletion(scopeId, command.cascade).bind()
                scopeRepository.deleteById(scopeId).mapLeft { it.toGenericApplicationError() }.bind()
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

    private suspend fun validateScopeExists(scopeId: ScopeId): Either<ScopeManagementApplicationError, Unit> = either {
        val existingScope = scopeRepository.findById(scopeId).mapLeft { it.toGenericApplicationError() }.bind()
        ensure(existingScope != null) {
            logger.warn("Scope not found for deletion", mapOf("scopeId" to scopeId.value))
            ScopeNotFoundError(scopeId = scopeId).toGenericApplicationError()
        }
    }

    private suspend fun handleChildrenDeletion(scopeId: ScopeId, cascade: Boolean): Either<ScopeManagementApplicationError, Unit> = either {
        val children = scopeRepository.findByParentId(scopeId, offset = 0, limit = 1000)
            .mapLeft { it.toGenericApplicationError() }.bind()

        if (children.isNotEmpty()) {
            if (cascade) {
                logger.debug(
                    "Cascade deleting children",
                    mapOf(
                        "parentId" to scopeId.value,
                        "childCount" to children.size.toString(),
                    ),
                )
                for (child in children) {
                    deleteRecursive(child.id).bind()
                }
            } else {
                logger.warn(
                    "Cannot delete scope with children",
                    mapOf(
                        "scopeId" to scopeId.value,
                        "childCount" to children.size.toString(),
                    ),
                )
                raise(
                    ScopeHierarchyApplicationError.HasChildren(
                        scopeId = scopeId.value,
                        childCount = children.size,
                    ),
                )
            }
        }
    }

    private suspend fun deleteRecursive(scopeId: ScopeId): Either<ScopeManagementApplicationError, Unit> = either {
        // Find children of this scope
        val children = scopeRepository.findByParentId(scopeId, offset = 0, limit = 1000).mapLeft { it.toGenericApplicationError() }.bind()

        // Recursively delete all children
        for (child in children) {
            deleteRecursive(child.id).bind()
        }

        // Delete this scope
        scopeRepository.deleteById(scopeId).mapLeft { it.toGenericApplicationError() }.bind()
        logger.debug("Recursively deleted scope", mapOf("scopeId" to scopeId.value))
    }
}
