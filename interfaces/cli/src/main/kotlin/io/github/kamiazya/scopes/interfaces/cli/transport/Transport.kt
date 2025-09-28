package io.github.kamiazya.scopes.interfaces.cli.transport

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AddAliasResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ClearContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ContextViewListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.DeleteContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.DeleteScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.RemoveAliasResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.RenameAliasResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ResolveAliasResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.SwitchContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition
import io.github.kamiazya.scopes.contracts.userpreferences.errors.UserPreferencesContractError
import io.github.kamiazya.scopes.contracts.userpreferences.results.PreferenceResult

/**
 * Transport abstraction for CLI operations.
 * Allows switching between local (in-process) and remote (gRPC) execution.
 */
interface Transport {

    // Scope Management Operations

    /**
     * Creates a new scope.
     */
    suspend fun createScope(
        title: String,
        description: String? = null,
        parentId: String? = null,
        customAlias: String? = null,
    ): Either<ScopeContractError, CreateScopeResult>

    /**
     * Gets a scope by ID.
     */
    suspend fun getScope(id: String): Either<ScopeContractError, ScopeResult?>

    /**
     * Updates an existing scope.
     */
    suspend fun updateScope(id: String, title: String? = null, description: String? = null): Either<ScopeContractError, UpdateScopeResult>

    /**
     * Deletes a scope.
     */
    suspend fun deleteScope(id: String, cascade: Boolean = false): Either<ScopeContractError, DeleteScopeResult>

    /**
     * Lists root scopes (no parent).
     */
    suspend fun getRootScopes(): Either<ScopeContractError, ScopeListResult>

    /**
     * Gets children of a scope.
     */
    suspend fun getChildren(parentId: String, includeDescendants: Boolean = false): Either<ScopeContractError, ScopeListResult>

    /**
     * Lists all scopes.
     */
    suspend fun listScopes(): Either<ScopeContractError, ScopeListResult>

    // Alias Operations

    /**
     * Adds an alias to a scope.
     *
     * @param scopeOrAlias The scope ID or existing alias to add the new alias to
     * @param alias The new alias to add
     */
    suspend fun addAlias(scopeOrAlias: String, alias: String): Either<ScopeContractError, AddAliasResult>

    /**
     * Removes an alias from a scope.
     *
     * @param scopeId The scope ID to remove the alias from
     * @param alias The alias to remove
     */
    suspend fun removeAlias(scopeId: String, alias: String): Either<ScopeContractError, RemoveAliasResult>

    /**
     * Sets the canonical alias for a scope.
     *
     * @param scopeId The scope ID to set the canonical alias for
     * @param newCanonicalAlias The new canonical alias to set
     */
    suspend fun setCanonicalAlias(scopeId: String, newCanonicalAlias: String): Either<ScopeContractError, RenameAliasResult>

    /**
     * Lists all aliases for a scope.
     *
     * @param scopeOrAlias The scope ID or existing alias to list aliases for
     */
    suspend fun listAliases(scopeOrAlias: String): Either<ScopeContractError, AliasListResult>

    /**
     * Resolves an alias to scope information.
     */
    suspend fun resolveAlias(aliasOrPrefix: String): Either<ScopeContractError, ResolveAliasResult>

    // Context View Operations

    /**
     * Creates a new context view.
     */
    suspend fun createContextView(key: String, name: String, description: String? = null, filter: String): Either<ScopeContractError, CreateContextViewResult>

    /**
     * Gets a context view by key.
     */
    suspend fun getContextView(key: String): Either<ScopeContractError, ContextViewResult?>

    /**
     * Updates a context view.
     */
    suspend fun updateContextView(
        key: String,
        name: String? = null,
        description: String? = null,
        filter: String? = null,
    ): Either<ScopeContractError, UpdateContextViewResult>

    /**
     * Deletes a context view.
     */
    suspend fun deleteContextView(key: String): Either<ScopeContractError, DeleteContextViewResult>

    /**
     * Lists all context views.
     */
    suspend fun listContextViews(): Either<ScopeContractError, ContextViewListResult>

    /**
     * Gets the current context view.
     */
    suspend fun getCurrentContextView(): Either<ScopeContractError, ContextViewResult?>

    /**
     * Switches to a different context view.
     */
    suspend fun switchContextView(key: String): Either<ScopeContractError, SwitchContextViewResult>

    /**
     * Clears the current context view.
     */
    suspend fun clearCurrentContextView(): Either<ScopeContractError, ClearContextViewResult>

    // Aspect Definition Operations

    /**
     * Creates a new aspect definition.
     */
    suspend fun createAspectDefinition(key: String, description: String, type: String): Either<ScopeContractError, Unit>

    /**
     * Gets an aspect definition by key.
     */
    suspend fun getAspectDefinition(key: String): Either<ScopeContractError, AspectDefinition?>

    /**
     * Updates an aspect definition.
     */
    suspend fun updateAspectDefinition(key: String, description: String? = null): Either<ScopeContractError, Unit>

    /**
     * Deletes an aspect definition.
     */
    suspend fun deleteAspectDefinition(key: String): Either<ScopeContractError, Unit>

    /**
     * Lists all aspect definitions.
     */
    suspend fun listAspectDefinitions(): Either<ScopeContractError, List<AspectDefinition>>

    /**
     * Validates aspect values against their definitions.
     */
    suspend fun validateAspectValue(key: String, values: List<String>): Either<ScopeContractError, List<String>>

    // User Preferences

    /**
     * Gets user preferences.
     */
    suspend fun getPreferences(): Either<UserPreferencesContractError, PreferenceResult>

    // Lifecycle Operations

    /**
     * Checks if the transport is available.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Disconnects/cleans up resources.
     */
    suspend fun disconnect()
}

/**
 * Transport type enumeration.
 */
enum class TransportType {
    LOCAL,
    GRPC,
}

/**
 * Configuration for transport selection.
 */
data class TransportConfig(val type: TransportType = TransportType.LOCAL, val grpcEndpoint: String? = null)
