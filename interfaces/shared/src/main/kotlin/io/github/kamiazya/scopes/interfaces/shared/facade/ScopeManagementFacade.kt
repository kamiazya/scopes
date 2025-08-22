package io.github.kamiazya.scopes.interfaces.shared.facade

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.application.command.CreateScope
import io.github.kamiazya.scopes.scopemanagement.application.command.DeleteScope
import io.github.kamiazya.scopes.scopemanagement.application.command.UpdateScope
import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.application.handler.CreateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.DeleteScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetChildrenHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetRootScopesHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.GetScopeByIdHandler
import io.github.kamiazya.scopes.scopemanagement.application.handler.UpdateScopeHandler
import io.github.kamiazya.scopes.scopemanagement.application.query.GetChildren
import io.github.kamiazya.scopes.scopemanagement.application.query.GetRootScopes
import io.github.kamiazya.scopes.scopemanagement.application.query.GetScopeById
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Facade for Scope Management Context
 *
 * This facade provides a unified interface to the Scope Management bounded context.
 * It encapsulates all use case handlers and provides a simplified API for external interfaces.
 *
 * Key responsibilities:
 * - Aggregates all scope-related operations in a single interface
 * - Simplifies error handling for interface adapters
 * - Provides a stable API even if internal handlers change
 * - Prepares for future cross-context coordination
 */
class ScopeManagementFacade(
    private val createScopeHandler: CreateScopeHandler,
    private val updateScopeHandler: UpdateScopeHandler,
    private val deleteScopeHandler: DeleteScopeHandler,
    private val getScopeByIdHandler: GetScopeByIdHandler,
    private val getChildrenHandler: GetChildrenHandler,
    private val getRootScopesHandler: GetRootScopesHandler,
) {
    /**
     * Creates a new scope
     */
    suspend fun createScope(
        title: String,
        description: String? = null,
        parentId: String? = null,
        generateAlias: Boolean = true,
        customAlias: String? = null,
    ): Either<ScopesError, CreateScopeResult> {
        val command = CreateScope(
            title = title,
            description = description,
            parentId = parentId,
            generateAlias = generateAlias,
            customAlias = customAlias,
        )
        return createScopeHandler(command)
    }

    /**
     * Updates an existing scope
     */
    suspend fun updateScope(id: String, title: String? = null, description: String? = null): Either<ScopesError, ScopeDto> {
        val command = UpdateScope(
            id = id,
            title = title,
            description = description,
        )
        return updateScopeHandler(command)
    }

    /**
     * Deletes a scope
     */
    suspend fun deleteScope(id: String): Either<ScopesError, Unit> {
        val command = DeleteScope(id = id)
        return deleteScopeHandler(command)
    }

    /**
     * Retrieves a scope by ID
     */
    suspend fun getScopeById(id: String): Either<ScopesError, ScopeDto> {
        val query = GetScopeById(id = id)
        return getScopeByIdHandler(query)
    }

    /**
     * Retrieves child scopes of a parent
     */
    suspend fun getChildren(parentId: String): Either<ScopesError, List<ScopeDto>> {
        val query = GetChildren(parentId = parentId)
        return getChildrenHandler(query)
    }

    /**
     * Retrieves all root scopes
     */
    suspend fun getRootScopes(): Either<ScopesError, List<ScopeDto>> {
        val query = GetRootScopes()
        return getRootScopesHandler(query)
    }

    /**
     * Creates a root scope (convenience method)
     */
    suspend fun createRootScope(title: String, description: String? = null): Either<ScopesError, CreateScopeResult> = createScope(
        title = title,
        description = description,
        parentId = null,
    )
}
