package io.github.kamiazya.scopes.scopemanagement.application.services

import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.scopemanagement.application.query.response.builders.GetScopeResponseBuilder
import io.github.kamiazya.scopes.scopemanagement.application.query.response.builders.ListScopesResponseBuilder
import io.github.kamiazya.scopes.scopemanagement.application.query.response.data.GetScopeResponse
import io.github.kamiazya.scopes.scopemanagement.application.query.response.data.ListScopesResponse

/**
 * Application service that provides response formatting using ResponseBuilders.
 * This service acts as a facade for the interface layer to access response builders.
 */
class ResponseFormatterService {

    private val listScopesBuilder = ListScopesResponseBuilder()
    private val getScopeBuilder = GetScopeResponseBuilder()

    /**
     * Format root scopes for MCP response.
     */
    fun formatRootScopesForMcp(result: ScopeListResult): Map<String, Any> {
        val response = ListScopesResponse(
            scopes = result.scopes,
            totalCount = null,
            hasMore = null,
            includeAliases = false,
            includeDebug = false,
            isRootScopes = true,
        )
        return listScopesBuilder.buildMcpResponse(response)
    }

    /**
     * Format paginated scopes for MCP response.
     */
    fun formatPagedScopesForMcp(result: ScopeListResult): Map<String, Any> {
        val response = ListScopesResponse(
            scopes = result.scopes,
            totalCount = result.totalCount.toLong(),
            hasMore = result.totalCount > result.offset + result.limit,
            includeAliases = false,
            includeDebug = false,
        )
        return listScopesBuilder.buildMcpResponse(response)
    }

    /**
     * Format single scope for MCP response.
     */
    fun formatScopeForMcp(scope: ScopeResult, aliases: List<AliasInfo>? = null): Map<String, Any> {
        val response = GetScopeResponse(
            scope = scope,
            aliases = aliases,
            includeDebug = false,
            includeTemporalFields = true,
        )
        return getScopeBuilder.buildMcpResponse(response)
    }

    /**
     * Format root scopes for CLI response.
     */
    fun formatRootScopesForCli(result: ScopeListResult, includeDebug: Boolean = false, includeAliases: Boolean = false): String {
        val response = ListScopesResponse(
            scopes = result.scopes,
            totalCount = null,
            hasMore = null,
            includeAliases = includeAliases,
            includeDebug = includeDebug,
        )
        return listScopesBuilder.buildCliResponse(response)
    }

    /**
     * Format paginated scopes for CLI response.
     */
    fun formatPagedScopesForCli(result: ScopeListResult, includeDebug: Boolean = false, includeAliases: Boolean = false): String {
        val response = ListScopesResponse(
            scopes = result.scopes,
            totalCount = result.totalCount.toLong(),
            hasMore = result.totalCount > result.offset + result.limit,
            includeAliases = includeAliases,
            includeDebug = includeDebug,
        )
        return listScopesBuilder.buildCliResponse(response)
    }

    /**
     * Format single scope for CLI response.
     */
    fun formatScopeForCli(scope: ScopeResult, aliases: List<AliasInfo>? = null, includeDebug: Boolean = false, includeTemporalFields: Boolean = true): String {
        val response = GetScopeResponse(
            scope = scope,
            aliases = aliases,
            includeDebug = includeDebug,
            includeTemporalFields = includeTemporalFields,
        )
        return getScopeBuilder.buildCliResponse(response)
    }
}
