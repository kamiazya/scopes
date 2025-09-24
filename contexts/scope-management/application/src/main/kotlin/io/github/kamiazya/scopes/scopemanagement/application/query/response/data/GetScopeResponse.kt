package io.github.kamiazya.scopes.scopemanagement.application.query.response.data

import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult

/**
 * Response data for a single scope query.
 *
 * Contains the scope information along with optional metadata and formatting options
 * that control how the data should be presented in different output formats.
 *
 * @property scope The main scope result data from the query
 * @property aliases Optional list of all aliases associated with the scope
 * @property includeDebug When true, includes internal identifiers and debug information
 * @property includeTemporalFields When true, includes created/updated timestamps in output
 */
data class GetScopeResponse(
    val scope: ScopeResult,
    val aliases: List<AliasInfo>? = null,
    val includeDebug: Boolean = false,
    val includeTemporalFields: Boolean = true,
)
