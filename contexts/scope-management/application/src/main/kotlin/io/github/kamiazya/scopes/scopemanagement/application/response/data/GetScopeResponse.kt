package io.github.kamiazya.scopes.scopemanagement.application.response.data

import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult

data class GetScopeResponse(
    val scope: ScopeResult,
    val aliases: List<AliasInfo>? = null,
    val includeDebug: Boolean = false,
    val includeTimestamps: Boolean = true,
)
