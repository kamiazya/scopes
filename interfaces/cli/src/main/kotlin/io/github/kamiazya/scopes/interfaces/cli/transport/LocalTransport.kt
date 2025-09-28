package io.github.kamiazya.scopes.interfaces.cli.transport

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetActiveContextCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateContextViewCommand
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
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.AliasQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ContextQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.datetime.Clock

/**
 * Local (in-process) transport implementation.
 * Directly calls the existing command and query adapters.
 */
class LocalTransport(
    private val scopeCommandAdapter: ScopeCommandAdapter,
    private val scopeQueryAdapter: ScopeQueryAdapter,
    private val aliasCommandAdapter: AliasCommandAdapter,
    private val aliasQueryAdapter: AliasQueryAdapter,
    private val contextCommandAdapter: ContextCommandAdapter,
    private val contextQueryAdapter: ContextQueryAdapter,
    private val parameterResolver: ScopeParameterResolver,
    private val logger: Logger,
) : Transport {

    // Scope Management Operations

    override suspend fun createScope(
        title: String,
        description: String?,
        parentId: String?,
        customAlias: String?,
    ): Either<ScopeContractError, CreateScopeResult> {
        logger.debug(
            "LocalTransport: createScope",
            mapOf(
                "title" to title,
                "parentId" to parentId,
                "customAlias" to customAlias,
            ) as Map<String, Any>,
        )
        return scopeCommandAdapter.createScope(title, description, parentId, customAlias)
    }

    override suspend fun getScope(id: String): Either<ScopeContractError, ScopeResult?> {
        logger.debug("LocalTransport: getScope", mapOf("id" to id) as Map<String, Any>)
        return scopeQueryAdapter.getScopeById(id)
            .map { it as ScopeResult? }
    }

    override suspend fun updateScope(id: String, title: String?, description: String?): Either<ScopeContractError, UpdateScopeResult> {
        logger.debug(
            "LocalTransport: updateScope",
            mapOf(
                "id" to id,
                "title" to title,
                "description" to description,
            ) as Map<String, Any>,
        )
        return scopeCommandAdapter.updateScope(id, title, description)
    }

    override suspend fun deleteScope(id: String, cascade: Boolean): Either<ScopeContractError, DeleteScopeResult> {
        logger.debug(
            "LocalTransport: deleteScope",
            mapOf(
                "id" to id,
                "cascade" to cascade,
            ) as Map<String, Any>,
        )
        return scopeCommandAdapter.deleteScope(id, cascade)
            .map { DeleteScopeResult(deletedScopeId = id, deletedChildrenCount = 0) }
    }

    override suspend fun getRootScopes(): Either<ScopeContractError, ScopeListResult> {
        logger.debug("LocalTransport: getRootScopes")
        return scopeQueryAdapter.listRootScopes()
    }

    override suspend fun getChildren(parentId: String, includeDescendants: Boolean): Either<ScopeContractError, ScopeListResult> {
        logger.debug(
            "LocalTransport: getChildren",
            mapOf(
                "parentId" to parentId,
                "includeDescendants" to includeDescendants,
            ) as Map<String, Any>,
        )
        return scopeQueryAdapter.listChildren(parentId)
    }

    override suspend fun listScopes(): Either<ScopeContractError, ScopeListResult> {
        logger.debug("LocalTransport: listScopes")
        // For now, return root scopes as listing all scopes is not directly supported
        return scopeQueryAdapter.listRootScopes()
    }

    // Alias Operations

    override suspend fun addAlias(scopeOrAlias: String, alias: String): Either<ScopeContractError, AddAliasResult> {
        logger.debug(
            "LocalTransport: addAlias",
            mapOf(
                "scopeOrAlias" to scopeOrAlias,
                "alias" to alias,
            ) as Map<String, Any>,
        )
        // Resolve scope ID from alias if needed
        val scopeId = parameterResolver.resolve(scopeOrAlias).fold(
            { error -> return Either.Left(error) },
            { id -> id },
        )
        return aliasCommandAdapter.addAlias(scopeId, alias)
            .map { AddAliasResult(scopeId = scopeId, alias = alias, aliasType = "custom") }
    }

    override suspend fun removeAlias(scopeId: String, alias: String): Either<ScopeContractError, RemoveAliasResult> {
        logger.debug("LocalTransport: removeAlias", mapOf("scopeId" to scopeId, "alias" to alias) as Map<String, Any>)
        return aliasCommandAdapter.removeAlias(scopeId = scopeId, aliasName = alias)
            .map { RemoveAliasResult(scopeId = scopeId, removedAlias = alias) }
    }

    override suspend fun setCanonicalAlias(scopeId: String, newCanonicalAlias: String): Either<ScopeContractError, RenameAliasResult> {
        logger.debug(
            "LocalTransport: setCanonicalAlias",
            mapOf(
                "scopeId" to scopeId,
                "newCanonicalAlias" to newCanonicalAlias,
            ) as Map<String, Any>,
        )
        // Note: aliasCommandAdapter.setCanonicalAlias still takes currentAlias as first parameter
        // We need to resolve the current canonical alias from the scopeId first
        // For now, use scopeId as currentAlias (this will need proper implementation)
        return aliasCommandAdapter.setCanonicalAlias(scopeId, newCanonicalAlias)
            .map { RenameAliasResult(scopeId = scopeId, oldCanonicalAlias = "", newCanonicalAlias = newCanonicalAlias) }
    }

    override suspend fun listAliases(scopeOrAlias: String): Either<ScopeContractError, AliasListResult> {
        logger.debug("LocalTransport: listAliases", mapOf("scopeOrAlias" to scopeOrAlias) as Map<String, Any>)
        // Resolve scope ID from alias if needed
        val scopeId = parameterResolver.resolve(scopeOrAlias).fold(
            { error -> return Either.Left(error) },
            { id -> id },
        )
        return aliasQueryAdapter.listAliases(scopeId)
    }

    override suspend fun resolveAlias(aliasOrPrefix: String): Either<ScopeContractError, ResolveAliasResult> {
        logger.debug("LocalTransport: resolveAlias", mapOf("aliasOrPrefix" to aliasOrPrefix) as Map<String, Any>)
        // Resolve alias is not directly supported in query adapter
        // For now, return an error
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "alias resolution",
            ),
        )
    }

    // Context View Operations

    override suspend fun createContextView(
        key: String,
        name: String,
        description: String?,
        filter: String,
    ): Either<ScopeContractError, CreateContextViewResult> {
        logger.debug(
            "LocalTransport: createContextView",
            mapOf(
                "key" to key,
                "name" to name,
                "filter" to filter,
            ) as Map<String, Any>,
        )
        return contextCommandAdapter.createContext(
            CreateContextViewCommand(
                key = key,
                name = name,
                description = description,
                filter = filter,
            ),
        ).map {
            CreateContextViewResult(
                key = key,
                name = name,
                description = description,
                filter = filter,
                createdAt = kotlinx.datetime.Clock.System.now(),
            )
        }
    }

    override suspend fun getContextView(key: String): Either<ScopeContractError, ContextViewResult?> {
        logger.debug("LocalTransport: getContextView", mapOf("key" to key) as Map<String, Any>)
        return contextQueryAdapter.getContextView(key)
            .map { contextView ->
                contextView?.let {
                    ContextViewResult(
                        key = it.key,
                        name = it.name,
                        description = it.description,
                        filter = it.filter,
                        createdAt = kotlinx.datetime.Clock.System.now(),
                        updatedAt = kotlinx.datetime.Clock.System.now(),
                        isActive = false,
                    )
                }
            }
    }

    override suspend fun updateContextView(
        key: String,
        name: String?,
        description: String?,
        filter: String?,
    ): Either<ScopeContractError, UpdateContextViewResult> {
        logger.debug(
            "LocalTransport: updateContextView",
            mapOf(
                "key" to key,
                "name" to name,
                "filter" to filter,
            ) as Map<String, Any>,
        )
        return contextCommandAdapter.updateContext(
            UpdateContextViewCommand(
                key = key,
                name = name,
                description = description,
                filter = filter,
            ),
        ).map {
            UpdateContextViewResult(
                key = key,
                name = name ?: "",
                description = description,
                filter = filter ?: "",
                updatedAt = kotlinx.datetime.Clock.System.now(),
            )
        }
    }

    override suspend fun deleteContextView(key: String): Either<ScopeContractError, DeleteContextViewResult> {
        logger.debug("LocalTransport: deleteContextView", mapOf("key" to key) as Map<String, Any>)
        return contextCommandAdapter.deleteContext(
            DeleteContextViewCommand(key = key),
        ).map {
            DeleteContextViewResult(
                deletedKey = key,
                wasActive = false,
            )
        }
    }

    override suspend fun listContextViews(): Either<ScopeContractError, ContextViewListResult> {
        logger.debug("LocalTransport: listContextViews")
        return contextQueryAdapter.listContextViews()
            .map { contextViews ->
                ContextViewListResult(
                    contextViews = contextViews.map { cv ->
                        ContextViewResult(
                            key = cv.key,
                            name = cv.name,
                            description = cv.description,
                            filter = cv.filter,
                            createdAt = kotlinx.datetime.Clock.System.now(),
                            updatedAt = kotlinx.datetime.Clock.System.now(),
                            isActive = false,
                        )
                    },
                    totalCount = contextViews.size,
                )
            }
    }

    override suspend fun getCurrentContextView(): Either<ScopeContractError, ContextViewResult?> {
        logger.debug("LocalTransport: getCurrentContextView")
        return contextQueryAdapter.getCurrentContext()
            .map { contextView ->
                contextView?.let {
                    ContextViewResult(
                        key = it.key,
                        name = it.name,
                        description = it.description,
                        filter = it.filter,
                        createdAt = kotlinx.datetime.Clock.System.now(),
                        updatedAt = kotlinx.datetime.Clock.System.now(),
                        isActive = true,
                    )
                }
            }
    }

    override suspend fun switchContextView(key: String): Either<ScopeContractError, SwitchContextViewResult> {
        logger.debug("LocalTransport: switchContextView", mapOf("key" to key) as Map<String, Any>)
        return contextCommandAdapter.setCurrentContext(
            SetActiveContextCommand(key = key),
        ).map {
            SwitchContextViewResult(
                previousKey = null,
                newKey = key,
                filter = "",
            )
        }
    }

    override suspend fun clearCurrentContextView(): Either<ScopeContractError, ClearContextViewResult> {
        logger.debug("LocalTransport: clearCurrentContextView")
        return contextCommandAdapter.clearCurrentContext()
            .map {
                ClearContextViewResult(
                    clearedKey = null,
                )
            }
    }

    // User Preferences

    override suspend fun getPreferences(): Either<UserPreferencesContractError, PreferenceResult> {
        logger.debug("LocalTransport: getPreferences")
        // For now, return a default preference result
        // This should be wired to a UserPreferencesQueryAdapter when available
        return Either.Right(
            PreferenceResult.HierarchyPreferences(
                maxDepth = null,
                maxChildrenPerScope = null,
            ),
        )
    }

    // Aspect Definition Operations

    override suspend fun createAspectDefinition(key: String, description: String, type: String): Either<ScopeContractError, Unit> {
        logger.debug(
            "LocalTransport: createAspectDefinition",
            mapOf(
                "key" to key,
                "description" to description,
                "type" to type,
            ) as Map<String, Any>,
        )
        // Aspect definitions are not yet implemented in local adapters
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "aspect definitions",
            ),
        )
    }

    override suspend fun getAspectDefinition(key: String): Either<ScopeContractError, AspectDefinition?> {
        logger.debug("LocalTransport: getAspectDefinition", mapOf("key" to key) as Map<String, Any>)
        // Aspect definitions are not yet implemented in local adapters
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "aspect definitions",
            ),
        )
    }

    override suspend fun updateAspectDefinition(key: String, description: String?): Either<ScopeContractError, Unit> {
        logger.debug(
            "LocalTransport: updateAspectDefinition",
            mapOf(
                "key" to key,
                "description" to description,
            ) as Map<String, Any>,
        )
        // Aspect definitions are not yet implemented in local adapters
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "aspect definitions",
            ),
        )
    }

    override suspend fun deleteAspectDefinition(key: String): Either<ScopeContractError, Unit> {
        logger.debug("LocalTransport: deleteAspectDefinition", mapOf("key" to key) as Map<String, Any>)
        // Aspect definitions are not yet implemented in local adapters
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "aspect definitions",
            ),
        )
    }

    override suspend fun listAspectDefinitions(): Either<ScopeContractError, List<AspectDefinition>> {
        logger.debug("LocalTransport: listAspectDefinitions")
        // Aspect definitions are not yet implemented in local adapters
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "aspect definitions",
            ),
        )
    }

    override suspend fun validateAspectValue(key: String, values: List<String>): Either<ScopeContractError, List<String>> {
        logger.debug(
            "LocalTransport: validateAspectValue",
            mapOf(
                "key" to key,
                "values" to values,
            ) as Map<String, Any>,
        )
        // Aspect definitions are not yet implemented in local adapters
        return Either.Left(
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "aspect definitions",
            ),
        )
    }

    // Lifecycle Operations

    override suspend fun isAvailable(): Boolean {
        // Local transport is always available
        return true
    }

    override suspend fun disconnect() {
        // Nothing to disconnect for local transport
        logger.debug("LocalTransport: disconnect (no-op)")
    }
}
