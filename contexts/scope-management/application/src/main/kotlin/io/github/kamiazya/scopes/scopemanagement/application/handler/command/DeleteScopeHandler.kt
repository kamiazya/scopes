package io.github.kamiazya.scopes.scopemanagement.application.handler.command

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.scope.DeleteScopeCommand
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeHierarchyError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopeNotFoundError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.ScopeId
import kotlinx.datetime.Clock

/**
 * Handler for deleting a scope.
 */
class DeleteScopeHandler(private val scopeRepository: ScopeRepository, private val transactionManager: TransactionManager, private val logger: Logger) :
    CommandHandler<DeleteScopeCommand, ScopesError, Unit> {

    override suspend operator fun invoke(command: DeleteScopeCommand): Either<ScopesError, Unit> = either {
        logger.info(
            "Deleting scope",
            mapOf(
                "scopeId" to command.id,
                "cascade" to command.cascade.toString(),
            ),
        )

        transactionManager.inTransaction {
            either {
                val scopeId = ScopeId.create(command.id).bind()
                validateScopeExists(scopeId).bind()
                handleChildrenDeletion(scopeId, command.cascade).bind()
                deleteScopeById(scopeId).bind()
            }
        }.bind()
    }

    private suspend fun validateScopeExists(scopeId: ScopeId): Either<ScopesError, ScopeAggregate> = either {
        val existingScope = scopeRepository.findById(scopeId).bind()
        ensure(existingScope != null) {
            logger.warn("Scope not found for deletion", mapOf("scopeId" to scopeId.value))
            ScopeNotFoundError(
                scopeId = scopeId,
                occurredAt = Clock.System.now(),
            )
        }
        existingScope
    }

    private suspend fun handleChildrenDeletion(scopeId: ScopeId, cascade: Boolean): Either<ScopesError, Unit> = either {
        val children = scopeRepository.findByParentId(scopeId, offset = 0, limit = 1000).bind()
        
        if (children.isNotEmpty()) {
            if (cascade) {
                cascadeDeleteChildren(scopeId, children).bind()
            } else {
                preventDeletionWithChildren(scopeId, children).bind()
            }
        }
    }

    private suspend fun cascadeDeleteChildren(scopeId: ScopeId, children: List<ScopeAggregate>): Either<ScopesError, Unit> = either {
        logger.debug(
            "Cascade deleting children",
            mapOf(
                "parentId" to scopeId.value,
                "childCount" to children.size.toString(),
            ),
        )

        for (child in children) {
            deleteRecursive(child.id)
        }
    }

    private fun preventDeletionWithChildren(scopeId: ScopeId, children: List<ScopeAggregate>): Either<ScopesError, Unit> {
        logger.warn(
            "Cannot delete scope with children",
            mapOf(
                "scopeId" to scopeId.value,
                "childCount" to children.size.toString(),
            ),
        )
        return ScopeHierarchyError.HasChildren(
            scopeId = scopeId,
            occurredAt = Clock.System.now(),
        ).left()
    }

    private suspend fun deleteScopeById(scopeId: ScopeId): Either<ScopesError, Unit> = either {
        scopeRepository.deleteById(scopeId).bind()
        logger.info("Scope deleted successfully", mapOf("scopeId" to scopeId.value))
    }.onLeft { error ->
        logger.error(
            "Failed to delete scope",
            mapOf(
                "error" to (error::class.qualifiedName ?: error::class.simpleName ?: "UnknownError"),
                "message" to error.toString(),
            ),
        )
    }

    private suspend fun deleteRecursive(scopeId: ScopeId): Either<ScopesError, Unit> = either {
        // Find children of this scope
        val children = scopeRepository.findByParentId(scopeId, offset = 0, limit = 1000).bind()

        // Recursively delete all children
        for (child in children) {
            deleteRecursive(child.id).bind()
        }

        // Delete this scope
        scopeRepository.deleteById(scopeId).bind()
        logger.debug("Recursively deleted scope", mapOf("scopeId" to scopeId.value))
    }
}
