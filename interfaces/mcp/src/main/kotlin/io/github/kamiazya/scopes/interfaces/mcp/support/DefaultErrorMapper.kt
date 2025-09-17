package io.github.kamiazya.scopes.interfaces.mcp.support

import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.platform.observability.logging.Slf4jLogger
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * Default implementation of ErrorMapper for MCP tool error handling.
 *
 * This class is internal as it should only be used within the MCP module.
 * External modules should depend on the ErrorMapper interface.
 */
internal class DefaultErrorMapper(private val logger: Logger = Slf4jLogger("DefaultErrorMapper")) : ErrorMapper {

    private val errorMiddleware = ErrorHandlingMiddleware(logger)

    override fun mapContractError(error: ScopeContractError): CallToolResult {
        val errorResponse = errorMiddleware.mapScopeError(error)
        val errorData = buildJsonObject {
            put("code", errorResponse.code)
            put("message", errorResponse.message)
            put("userMessage", errorResponse.userMessage)
            errorResponse.details?.let { details ->
                putJsonObject("details") {
                    details.forEach { (key, value) ->
                        put(key, value.toString())
                    }
                }
            }
            // Legacy compatibility
            put("legacyCode", getErrorCode(error))
            putJsonObject("data") {
                put("type", error::class.simpleName)
                put("message", mapContractErrorMessage(error))
                when (error) {
                    is ScopeContractError.BusinessError.AliasNotFound -> {
                        put("alias", error.alias)
                    }
                    is ScopeContractError.BusinessError.DuplicateAlias -> {
                        put("alias", error.alias)
                    }
                    is ScopeContractError.BusinessError.DuplicateTitle -> {
                        put("title", error.title)
                        error.existingScopeId?.let { put("existingScopeId", it) }
                    }
                    else -> Unit
                }
            }
        }
        return CallToolResult(content = listOf(TextContent(errorData.toString())), isError = true)
    }

    override fun errorResult(message: String, code: Int?): CallToolResult {
        val errorData = buildJsonObject {
            put("code", code ?: -32000)
            put("message", message)
        }
        return CallToolResult(content = listOf(TextContent(errorData.toString())), isError = true)
    }

    private fun getErrorCode(error: ScopeContractError): Int = when (error) {
        is ScopeContractError.InputError -> -32602 // Invalid params
        is ScopeContractError.BusinessError.NotFound,
        is ScopeContractError.BusinessError.AliasNotFound,
        -> -32011 // Not found
        is ScopeContractError.BusinessError.DuplicateAlias,
        is ScopeContractError.BusinessError.DuplicateTitle,
        -> -32012 // Duplicate
        is ScopeContractError.BusinessError.HierarchyViolation -> -32013 // Hierarchy violation
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias,
        is ScopeContractError.BusinessError.AliasOfDifferentScope,
        -> -32013 // Hierarchy violation (alias constraint)
        is ScopeContractError.BusinessError.AlreadyDeleted,
        is ScopeContractError.BusinessError.ArchivedScope,
        is ScopeContractError.BusinessError.NotArchived,
        -> -32014 // State conflict
        is ScopeContractError.BusinessError.HasChildren -> -32010 // Business constraint violation
        is ScopeContractError.SystemError -> -32000 // Server error
    }

    private fun mapContractErrorMessage(error: ScopeContractError): String = when (error) {
        is ScopeContractError.BusinessError.NotFound -> "Scope not found: ${error.scopeId}"
        is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateAlias -> "Duplicate alias: ${error.alias}"
        is ScopeContractError.BusinessError.DuplicateTitle -> "Duplicate title"
        is ScopeContractError.BusinessError.AliasOfDifferentScope -> "Alias belongs to different scope: ${error.alias}"
        is ScopeContractError.InputError.InvalidId -> "Invalid id: ${error.id}"
        is ScopeContractError.InputError.InvalidAlias -> "Invalid alias: ${error.alias}"
        is ScopeContractError.SystemError.ServiceUnavailable -> "Service unavailable: ${error.service}"
        is ScopeContractError.SystemError.Timeout -> "Timeout: ${error.operation}"
        is ScopeContractError.SystemError.ConcurrentModification -> "Concurrent modification"
        is ScopeContractError.InputError.InvalidTitle -> "Invalid title"
        is ScopeContractError.InputError.InvalidDescription -> "Invalid description"
        is ScopeContractError.InputError.InvalidParentId -> "Invalid parent id"
        is ScopeContractError.BusinessError.HierarchyViolation -> "Hierarchy violation"
        is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted"
        is ScopeContractError.BusinessError.ArchivedScope -> "Archived scope"
        is ScopeContractError.BusinessError.NotArchived -> "Not archived"
        is ScopeContractError.BusinessError.HasChildren -> "Has children"
        is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias"
    }

    override fun successResult(content: String): CallToolResult = CallToolResult(content = listOf(TextContent(content)), isError = false)

    override fun mapContractErrorToResource(uri: String, error: ScopeContractError): ReadResourceResult {
        val code = getErrorCode(error)
        val message = mapContractErrorMessage(error)
        val errorType = error::class.simpleName ?: "UnknownError"

        return ResourceHelpers.createErrorResourceResult(
            uri = uri,
            code = code,
            message = message,
            errorType = errorType,
            asJson = true,
        )
    }
}
