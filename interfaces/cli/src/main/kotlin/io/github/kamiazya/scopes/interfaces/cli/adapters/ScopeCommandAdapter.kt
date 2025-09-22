package io.github.kamiazya.scopes.interfaces.cli.adapters

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult

/**
 * Adapter for CLI commands to interact with Scope Management write operations.
 *
 * Following CQRS principles, this adapter handles only commands (write operations)
 * that modify the state of the Scope Management bounded context.
 *
 * Key responsibilities:
 * - Adapts CLI command input to contract command port calls
 * - Coordinates write operations between multiple contexts (future)
 * - Manages cross-cutting concerns for write operations (logging, monitoring)
 * - Provides transaction boundaries for command execution
 */
class ScopeCommandAdapter(
    private val scopeManagementCommandPort: ScopeManagementCommandPort,
    // Future: Add other context command ports here
    // private val workspaceManagementCommandPort: WorkspaceManagementCommandPort?,
    // private val aiCollaborationCommandPort: AiCollaborationCommandPort?
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
        return scopeManagementCommandPort.createScope(command).map { result ->
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
    suspend fun updateScope(id: String, title: String? = null, description: String? = null): Either<ScopeContractError, UpdateScopeResult> {
        val command = UpdateScopeCommand(
            id = id,
            title = title,
            description = description,
        )
        return scopeManagementCommandPort.updateScope(command)
    }

    /**
     * Deletes a scope with cleanup
     */
    suspend fun deleteScope(id: String, cascade: Boolean = false): Either<ScopeContractError, Unit> {
        // Future: Clean up workspace before deletion
        // workspaceManagementPort?.cleanupWorkspace(id)

        val command = DeleteScopeCommand(id = id, cascade = cascade)
        return scopeManagementCommandPort.deleteScope(command)
    }

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
        return scopeManagementCommandPort.createScope(command).map { result ->
            // Future: Initialize project workspace
            // workspaceManagementPort?.createWorkspace(
            //     scopeId = result.id,
            //     path = path ?: "./${name.lowercase()}"
            // )

            result
        }
    }
}
