package io.github.kamiazya.scopes.interfaces.mcp.support

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeByAliasQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.interfaces.mcp.tools.ToolContext
import io.modelcontextprotocol.kotlin.sdk.CallToolResult

/**
 * Small extensions that encapsulate common ToolContext operations.
 */
fun ToolContext.argString(key: String, required: Boolean = false): String? = services.codec.getString(args, key, required)

fun ToolContext.argBoolean(key: String, default: Boolean = false): Boolean = services.codec.getBoolean(args, key, default)

suspend fun ToolContext.withIdempotency(toolName: String, idempotencyKey: String?, compute: suspend () -> CallToolResult): CallToolResult =
    services.idempotency.getOrCompute(toolName, args, idempotencyKey) { compute() }

/**
 * Resolve a scope by alias or map the domain error to a CallToolResult.
 */
suspend fun ToolContext.getScopeByAliasOrFail(alias: String): Either<CallToolResult, ScopeResult> =
    when (val res = ports.query.getScopeByAlias(GetScopeByAliasQuery(alias))) {
        is arrow.core.Either.Left -> Either.Left(services.errors.mapContractError(res.value))
        is arrow.core.Either.Right -> Either.Right(res.value)
    }
