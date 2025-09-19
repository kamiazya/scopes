package io.github.kamiazya.scopes.scopemanagement.application.command.handler

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeHierarchyApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.handler.BaseCommandHandler
import io.github.kamiazya.scopes.scopemanagement.application.service.error.CentralizedErrorMappingService
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId

/**
 * Handler for deleting a scope.
 * Uses BaseCommandHandler for common functionality and centralized error mapping.
 */
class DeleteScopeHandler(private val scopeRepository: ScopeRepository, transactionManager: TransactionManager, logger: Logger) :
    BaseCommandHandler<DeleteScopeCommand, Unit>(transactionManager, logger) {

    private val errorMappingService = CentralizedErrorMappingService()

    override suspend fun executeCommand(command: DeleteScopeCommand): Either<ScopeManagementApplicationError, Unit> = either {
        val scopeId = ScopeId.create(command.id).mapLeft {
            errorMappingService.mapScopeIdError(it, command.id, "delete-scope")
        }.bind()

        validateScopeExists(scopeId).bind()
        handleChildrenDeletion(scopeId, command.cascade).bind()
        scopeRepository.deleteById(scopeId).mapLeft {
            errorMappingService.mapRepositoryError(it, "delete-scope-final")
        }.bind()

        logger.info("Scope deleted successfully", mapOf("scopeId" to scopeId.value))
    }

    private suspend fun validateScopeExists(scopeId: ScopeId): Either<ScopeManagementApplicationError, Unit> = either {
        val existingScope = scopeRepository.findById(scopeId).mapLeft {
            errorMappingService.mapRepositoryError(it, "delete-scope-validation")
        }.bind()
        ensure(existingScope != null) {
            logger.warn("Scope not found for deletion", mapOf("scopeId" to scopeId.value))
            errorMappingService.mapScopeNotFoundError(scopeId, "delete-scope")
        }
    }

    private suspend fun handleChildrenDeletion(scopeId: ScopeId, cascade: Boolean): Either<ScopeManagementApplicationError, Unit> = either {
        val children = scopeRepository.findByParentId(scopeId, offset = 0, limit = 1000)
            .mapLeft { errorMappingService.mapRepositoryError(it, "delete-scope-find-children") }.bind()

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
        val children = scopeRepository.findByParentId(scopeId, offset = 0, limit = 1000).mapLeft {
            errorMappingService.mapRepositoryError(it, "delete-scope-recursive-find-children")
        }.bind()

        // Recursively delete all children
        for (child in children) {
            deleteRecursive(child.id).bind()
        }

        // Delete this scope
        scopeRepository.deleteById(scopeId).mapLeft {
            errorMappingService.mapRepositoryError(it, "delete-scope-recursive")
        }.bind()
        logger.debug("Recursively deleted scope", mapOf("scopeId" to scopeId.value))
    }
}
