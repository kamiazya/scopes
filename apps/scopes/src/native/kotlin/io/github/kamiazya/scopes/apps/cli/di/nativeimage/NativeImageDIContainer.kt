package io.github.kamiazya.scopes.apps.cli.di.nativeimage

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.formatters.ScopeOutputFormatter
import io.github.kamiazya.scopes.interfaces.cli.resolvers.ScopeParameterResolver

/**
 * Minimal Native Image compatible DI container for Scopes CLI.
 *
 * This manual DI implementation replaces Koin for Native Image builds
 * where reflection-based dependency injection may not work reliably.
 *
 * For Native Image compatibility, this provides only minimal stub implementations
 * to allow the CLI to compile and run basic commands.
 */
class NativeImageDIContainer private constructor() {

    companion object {
        @Volatile
        private var instance: NativeImageDIContainer? = null

        fun getInstance(): NativeImageDIContainer = instance ?: synchronized(this) {
            instance ?: NativeImageDIContainer().also { instance = it }
        }
    }

    private val instances = mutableMapOf<String, Any>()

    // Lazy initialization with thread safety
    private fun <T : Any> lazy(key: String, factory: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        synchronized(instances) {
            @Suppress("UNCHECKED_CAST")
            return instances.getOrPut(key) { factory() } as T
        }
    }

    // Minimal stub implementation of ScopeManagementCommandPort for Native Image
    fun scopeManagementCommandPort(): ScopeManagementCommandPort = lazy("scopeManagementCommandPort") {
        object : ScopeManagementCommandPort {
            override suspend fun createScope(command: CreateScopeCommand): Either<ScopeContractError, CreateScopeResult> {
                // Minimal stub implementation - just return success with dummy data
                return CreateScopeResult(
                    id = "01ARZ3NDEKTSV4RRFFQ69G5FAV", // Dummy ULID
                    title = command.title,
                    description = command.description,
                    parentId = command.parentId,
                    canonicalAlias = command.title.lowercase().replace(" ", "-"),
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now(),
                ).right()
            }

            override suspend fun updateScope(
                command: io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand,
            ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult> =
                ScopeContractError.SystemError.ServiceUnavailable("update-not-implemented").left()

            override suspend fun deleteScope(
                command: io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand,
            ): Either<ScopeContractError, Unit> = ScopeContractError.SystemError.ServiceUnavailable("delete-not-implemented").left()

            override suspend fun addAlias(
                command: io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand,
            ): Either<ScopeContractError, Unit> = ScopeContractError.SystemError.ServiceUnavailable("add-alias-not-implemented").left()

            override suspend fun removeAlias(
                command: io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand,
            ): Either<ScopeContractError, Unit> = ScopeContractError.SystemError.ServiceUnavailable("remove-alias-not-implemented").left()

            override suspend fun setCanonicalAlias(
                command: io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand,
            ): Either<ScopeContractError, Unit> = ScopeContractError.SystemError.ServiceUnavailable("set-canonical-alias-not-implemented").left()

            override suspend fun renameAlias(
                command: io.github.kamiazya.scopes.contracts.scopemanagement.commands.RenameAliasCommand,
            ): Either<ScopeContractError, Unit> = ScopeContractError.SystemError.ServiceUnavailable("rename-alias-not-implemented").left()
        }
    }

    // Minimal stub query port for parameter resolver
    private fun scopeManagementQueryPort(): io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort = lazy("scopeManagementQueryPort") {
        object : io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort {
            override suspend fun getScope(
                query: io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery,
            ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult?> =
                ScopeContractError.SystemError.ServiceUnavailable("query-not-implemented").left()

            override suspend fun getScopeByAlias(
                query: io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery,
            ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult> {
                // For Native Image stub, just assume the alias is the ID
                return io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult(
                    id = query.aliasName, // Use alias as ID for simplicity
                    title = query.aliasName,
                    description = null,
                    parentId = null,
                    canonicalAlias = query.aliasName,
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now(),
                ).right()
            }

            override suspend fun getRootScopes(
                query: io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery,
            ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> =
                io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                    scopes = emptyList(),
                    totalCount = 0,
                    offset = 0,
                    limit = 100,
                ).right()

            override suspend fun getChildren(
                query: io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery,
            ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult> =
                io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult(
                    scopes = emptyList(),
                    totalCount = 0,
                    offset = 0,
                    limit = 100,
                ).right()

            override suspend fun listAliases(
                query: io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery,
            ): Either<ScopeContractError, io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasListResult> =
                ScopeContractError.SystemError.ServiceUnavailable("list-aliases-not-implemented").left()

            override suspend fun listScopesWithAspect(
                query: io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithAspectQuery,
            ): Either<ScopeContractError, List<io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult>> =
                emptyList<io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult>().right()

            override suspend fun listScopesWithQuery(
                query: io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListScopesWithQueryQuery,
            ): Either<ScopeContractError, List<io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult>> =
                emptyList<io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult>().right()
        }
    }

    // CLI components
    fun scopeCommandAdapter(): ScopeCommandAdapter = lazy("scopeCommandAdapter") {
        ScopeCommandAdapter(
            scopeManagementCommandPort = scopeManagementCommandPort(),
        )
    }

    fun scopeOutputFormatter(): ScopeOutputFormatter = lazy("scopeOutputFormatter") {
        ScopeOutputFormatter()
    }

    // Use the real ScopeParameterResolver with stub query port
    fun scopeParameterResolver(): ScopeParameterResolver = lazy("scopeParameterResolver") {
        ScopeParameterResolver(scopeManagementQueryPort())
    }

    // Stub implementation for query adapter
    fun scopeQueryAdapter(): io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeQueryAdapter = lazy("scopeQueryAdapter") {
        io.github.kamiazya.scopes.interfaces.cli.adapters.ScopeQueryAdapter(
            scopeManagementQueryPort(),
        )
    }
}
