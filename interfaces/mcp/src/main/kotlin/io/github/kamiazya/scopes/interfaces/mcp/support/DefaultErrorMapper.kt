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
    private val errorCodeMapper = ErrorCodeMapper()
    private val errorMessageMapper = ErrorMessageMapper()
    private val errorDataExtractor = ErrorDataExtractor()

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
            put("legacyCode", errorCodeMapper.getErrorCode(error))
            putJsonObject("data") {
                put("type", error::class.simpleName)
                put("message", errorMessageMapper.mapContractErrorMessage(error))
                errorDataExtractor.extractErrorData(error, this)
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

    /**
     * Error code mapping logic extracted to reduce complexity.
     */
    private class ErrorCodeMapper {
        fun getErrorCode(error: ScopeContractError): Int = when (error) {
            is ScopeContractError.InputError -> -32602 // Invalid params
            is ScopeContractError.BusinessError -> getBusinessErrorCode(error)
            is ScopeContractError.DataInconsistency -> getDataInconsistencyErrorCode(error)
            is ScopeContractError.SystemError -> -32000 // Server error
        }

        private fun getBusinessErrorCode(error: ScopeContractError.BusinessError): Int = when (error) {
            is ScopeContractError.BusinessError.NotFound,
            is ScopeContractError.BusinessError.AliasNotFound,
            is ScopeContractError.BusinessError.ContextNotFound -> -32011 // Not found
            
            is ScopeContractError.BusinessError.DuplicateAlias,
            is ScopeContractError.BusinessError.DuplicateTitle,
            is ScopeContractError.BusinessError.DuplicateContextKey -> -32012 // Duplicate
            
            is ScopeContractError.BusinessError.HierarchyViolation,
            is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias,
            is ScopeContractError.BusinessError.AliasOfDifferentScope -> -32013 // Hierarchy violation
            
            is ScopeContractError.BusinessError.AlreadyDeleted,
            is ScopeContractError.BusinessError.ArchivedScope,
            is ScopeContractError.BusinessError.NotArchived -> -32014 // State conflict
            
            is ScopeContractError.BusinessError.HasChildren -> -32010 // Business constraint violation
            
            is ScopeContractError.BusinessError.AliasGenerationFailed,
            is ScopeContractError.BusinessError.AliasGenerationValidationFailed -> -32015 // Alias generation error
        }

        private fun getDataInconsistencyErrorCode(error: ScopeContractError.DataInconsistency): Int = when (error) {
            is ScopeContractError.DataInconsistency.MissingCanonicalAlias -> -32016 // Data consistency error
        }
    }

    /**
     * Error message mapping logic extracted to reduce complexity.
     */
    private class ErrorMessageMapper {
        fun mapContractErrorMessage(error: ScopeContractError): String = when (error) {
            is ScopeContractError.BusinessError -> mapBusinessErrorMessage(error)
            is ScopeContractError.InputError -> mapInputErrorMessage(error)
            is ScopeContractError.SystemError -> mapSystemErrorMessage(error)
            is ScopeContractError.DataInconsistency -> mapDataInconsistencyErrorMessage(error)
        }

        private fun mapBusinessErrorMessage(error: ScopeContractError.BusinessError): String = when (error) {
            is ScopeContractError.BusinessError.NotFound -> "Scope not found: ${error.scopeId}"
            is ScopeContractError.BusinessError.AliasNotFound -> "Alias not found: ${error.alias}"
            is ScopeContractError.BusinessError.DuplicateAlias -> "Duplicate alias: ${error.alias}"
            is ScopeContractError.BusinessError.DuplicateTitle -> "Duplicate title"
            is ScopeContractError.BusinessError.AliasOfDifferentScope -> "Alias belongs to different scope: ${error.alias}"
            is ScopeContractError.BusinessError.HierarchyViolation -> "Hierarchy violation"
            is ScopeContractError.BusinessError.AlreadyDeleted -> "Already deleted"
            is ScopeContractError.BusinessError.ArchivedScope -> "Archived scope"
            is ScopeContractError.BusinessError.NotArchived -> "Not archived"
            is ScopeContractError.BusinessError.HasChildren -> "Has children"
            is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias -> "Cannot remove canonical alias"
            is ScopeContractError.BusinessError.AliasGenerationFailed -> "Failed to generate alias for scope: ${error.scopeId}"
            is ScopeContractError.BusinessError.AliasGenerationValidationFailed -> "Generated alias failed validation: ${error.alias}"
            is ScopeContractError.BusinessError.ContextNotFound -> "Context not found: ${error.contextKey}"
            is ScopeContractError.BusinessError.DuplicateContextKey -> "Duplicate context key: ${error.contextKey}"
        }

        private fun mapInputErrorMessage(error: ScopeContractError.InputError): String = when (error) {
            is ScopeContractError.InputError.InvalidId -> "Invalid id: ${error.id}"
            is ScopeContractError.InputError.InvalidAlias -> "Invalid alias: ${error.alias}"
            is ScopeContractError.InputError.InvalidTitle -> "Invalid title"
            is ScopeContractError.InputError.InvalidDescription -> "Invalid description"
            is ScopeContractError.InputError.InvalidParentId -> "Invalid parent id"
            is ScopeContractError.InputError.InvalidContextKey -> "Invalid context key: ${error.key}"
            is ScopeContractError.InputError.InvalidContextName -> "Invalid context name: ${error.name}"
            is ScopeContractError.InputError.InvalidContextFilter -> "Invalid context filter: ${error.filter}"
            is ScopeContractError.InputError.ValidationFailure -> "Validation failed for ${error.field}: ${error.value}"
        }

        private fun mapSystemErrorMessage(error: ScopeContractError.SystemError): String = when (error) {
            is ScopeContractError.SystemError.ServiceUnavailable -> "Service unavailable: ${error.service}"
            is ScopeContractError.SystemError.Timeout -> "Timeout: ${error.operation}"
            is ScopeContractError.SystemError.ConcurrentModification -> "Concurrent modification"
        }

        private fun mapDataInconsistencyErrorMessage(error: ScopeContractError.DataInconsistency): String = when (error) {
            is ScopeContractError.DataInconsistency.MissingCanonicalAlias -> "Data inconsistency: Missing canonical alias for scope ${error.scopeId}"
        }
    }

    /**
     * Error data extraction logic to reduce complexity in main mapping method.
     */
    private class ErrorDataExtractor {
        fun extractErrorData(error: ScopeContractError, builder: kotlinx.serialization.json.JsonObjectBuilder) {
            when (error) {
                is ScopeContractError.BusinessError.AliasNotFound -> {
                    builder.put("alias", error.alias)
                }
                is ScopeContractError.BusinessError.DuplicateAlias -> {
                    builder.put("alias", error.alias)
                }
                is ScopeContractError.BusinessError.DuplicateTitle -> {
                    builder.put("title", error.title)
                    error.existingScopeId?.let { builder.put("existingScopeId", it) }
                }
                is ScopeContractError.BusinessError.ContextNotFound -> {
                    builder.put("contextKey", error.contextKey)
                }
                is ScopeContractError.BusinessError.DuplicateContextKey -> {
                    builder.put("contextKey", error.contextKey)
                    error.existingContextId?.let { builder.put("existingContextId", it) }
                }
                else -> Unit
            }
        }
    }

    override fun successResult(content: String): CallToolResult = CallToolResult(content = listOf(TextContent(content)), isError = false)

    override fun mapContractErrorToResource(uri: String, error: ScopeContractError): ReadResourceResult {
        val code = errorCodeMapper.getErrorCode(error)
        val message = errorMessageMapper.mapContractErrorMessage(error)
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
