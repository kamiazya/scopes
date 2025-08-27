package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult

/**
 * Adapter for CLI commands to interact with Scope Management
 *
 * This adapter acts as an interface layer between the CLI commands and the
 * Scope Management bounded context. It handles:
 * - Translating CLI parameters to contract commands/queries
 * - Coordinating between multiple contexts (future)
 * - Providing CLI-specific error handling and formatting hooks
 *
 * Key responsibilities:
 * - Adapts CLI input to contract port calls
 * - Manages cross-cutting concerns for CLI (logging, monitoring)
 * - Prepares for future multi-context coordination
 */
class ScopeCommandAdapter(
    private val scopeManagementPort: ScopeManagementPort,
    // Future: Add other context ports here
    // private val workspaceManagementPort: WorkspaceManagementPort?,
    // private val aiCollaborationPort: AiCollaborationPort?
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
    ): Either<ScopeContractError, CreateScopeResult> {
        // Step 1: Create scope in scope management context
        val command = CreateScopeCommand(
            title = title,
            description = description,
            parentId = parentId,
            generateAlias = generateAlias,
            customAlias = customAlias,
        )
        return scopeManagementPort.createScope(command).map { result ->
            // Future: Step 2: Initialize workspace if configured
            // workspaceManagementPort?.initializeWorkspace(result.id)

            // Future: Step 3: Set up AI collaboration if enabled
            // aiCollaborationPort?.notifyCreation(result.id)

            result
        }
    }

    /**
     * Updates an existing scope
     */
    suspend fun updateScope(id: String, title: String? = null, description: String? = null): Either<ScopeContractError, ScopeResult> {
        val command = UpdateScopeCommand(
            id = id,
            title = title,
            description = description,
        )
        return scopeManagementPort.updateScope(command).map { result ->
            ScopeResult(
                id = result.id,
                title = result.title,
                description = result.description,
                parentId = result.parentId,
                canonicalAlias = result.canonicalAlias,
                createdAt = result.createdAt,
                updatedAt = result.updatedAt,
            )
        }
    }

    /**
     * Deletes a scope with cleanup
     */
    suspend fun deleteScope(id: String): Either<ScopeContractError, Unit> {
        // Future: Clean up workspace before deletion
        // workspaceManagementPort?.cleanupWorkspace(id)

        val command = DeleteScopeCommand(id = id)
        return scopeManagementPort.deleteScope(command)
    }

    /**
     * Retrieves a scope by ID
     */
    suspend fun getScopeById(id: String): Either<ScopeContractError, ScopeResult> {
        val query = GetScopeQuery(id = id)
        return scopeManagementPort.getScope(query).fold(
            { error -> Either.Left(error) },
            { result -> result?.let { Either.Right(it) } ?: Either.Left(ScopeContractError.BusinessError.NotFound(id)) },
        )
    }

    /**
     * Lists child scopes
     */
    suspend fun listChildren(parentId: String): Either<ScopeContractError, List<ScopeResult>> {
        val query = GetChildrenQuery(parentId = parentId)
        return scopeManagementPort.getChildren(query)
    }

    /**
     * Lists root scopes
     */
    suspend fun listRootScopes(): Either<ScopeContractError, List<ScopeResult>> = scopeManagementPort.getRootScopes()

    /**
     * Lists all aliases for a specific scope
     */
    suspend fun listAliases(scopeId: String): Either<ScopeContractError, AliasListResult> = scopeManagementPort.listAliases(ListAliasesQuery(scopeId = scopeId))

    /**
     * Initializes a new project (root scope with workspace)
     */
    suspend fun initializeProject(
        name: String,
        description: String? = null,
        // path: String? = null  // Future: workspace path
    ): Either<ScopeContractError, CreateScopeResult> {
        // Create root scope
        val command = CreateScopeCommand(
            title = name,
            description = description,
            parentId = null,
        )
        return scopeManagementPort.createScope(command).map { result ->
            // Future: Initialize project workspace
            // workspaceManagementPort?.createWorkspace(
            //     scopeId = result.id,
            //     path = path ?: "./${name.lowercase()}"
            // )

            result
        }
    }
}
