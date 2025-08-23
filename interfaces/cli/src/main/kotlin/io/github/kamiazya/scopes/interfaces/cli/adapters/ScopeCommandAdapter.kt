package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.interfaces.shared.facade.ScopeManagementFacade
import io.github.kamiazya.scopes.scopemanagement.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.dto.ScopeDto
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Adapter for CLI commands to interact with Scope Management
 *
 * This adapter acts as an interface layer between the CLI commands and the
 * Scope Management bounded context. It handles:
 * - Translating CLI parameters to domain operations
 * - Coordinating between multiple contexts (future)
 * - Providing CLI-specific error handling and formatting hooks
 *
 * Key responsibilities:
 * - Adapts CLI input to facade calls
 * - Manages cross-cutting concerns for CLI (logging, monitoring)
 * - Prepares for future multi-context coordination
 */
class ScopeCommandAdapter(
    private val scopeManagementFacade: ScopeManagementFacade,
    // Future: Add other context facades here
    // private val workspaceManagementFacade: WorkspaceManagementFacade?,
    // private val aiCollaborationFacade: AiCollaborationFacade?
) {
    /**
     * Creates a new scope with optional workspace initialization
     */
    suspend fun createScope(
        title: String,
        description: String? = null,
        parentId: String? = null,
        generateAlias: Boolean = true,
        customAlias: String? = null,
    ): Either<ScopesError, CreateScopeResult> {
        // Step 1: Create scope in scope management context
        return scopeManagementFacade.createScope(
            title = title,
            description = description,
            parentId = parentId,
            generateAlias = generateAlias,
            customAlias = customAlias,
        ).map { result ->
            // Future: Step 2: Initialize workspace if configured
            // workspaceManagementFacade?.initializeWorkspace(result.id)

            // Future: Step 3: Set up AI collaboration if enabled
            // aiCollaborationFacade?.notifyCreation(result.id)

            result
        }
    }

    /**
     * Updates an existing scope
     */
    suspend fun updateScope(id: String, title: String? = null, description: String? = null): Either<ScopesError, ScopeDto> = scopeManagementFacade.updateScope(
        id = id,
        title = title,
        description = description,
    )

    /**
     * Deletes a scope with cleanup
     */
    suspend fun deleteScope(id: String): Either<ScopesError, Unit> {
        // Future: Clean up workspace before deletion
        // workspaceManagementFacade?.cleanupWorkspace(id)

        return scopeManagementFacade.deleteScope(id)
    }

    /**
     * Retrieves a scope by ID
     */
    suspend fun getScopeById(id: String): Either<ScopesError, ScopeDto> = scopeManagementFacade.getScopeById(id)

    /**
     * Lists child scopes
     */
    suspend fun listChildren(parentId: String): Either<ScopesError, List<ScopeDto>> = scopeManagementFacade.getChildren(parentId)

    /**
     * Lists root scopes
     */
    suspend fun listRootScopes(): Either<ScopesError, List<ScopeDto>> = scopeManagementFacade.getRootScopes()

    /**
     * Initializes a new project (root scope with workspace)
     */
    suspend fun initializeProject(
        name: String,
        description: String? = null,
        // path: String? = null  // Future: workspace path
    ): Either<ScopesError, CreateScopeResult> {
        // Create root scope
        return scopeManagementFacade.createRootScope(
            title = name,
            description = description,
        ).map { result ->
            // Future: Initialize project workspace
            // workspaceManagementFacade?.createWorkspace(
            //     scopeId = result.id,
            //     path = path ?: "./${name.lowercase()}"
            // )

            result
        }
    }
}
