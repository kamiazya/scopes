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
import io.github.kamiazya.scopes.interfaces.cli.grpc.GatewayClient
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

/**
 * gRPC transport implementation.
 * Routes operations through the daemon via GatewayClient.
 * This transport bypasses SQLite completely to avoid JNI issues in native builds.
 */
class GrpcTransport(private val gatewayClient: GatewayClient, private val logger: Logger) : Transport {

    private var connected = false

    private suspend fun ensureConnected() {
        if (!connected) {
            gatewayClient.connect().fold(
                { error ->
                    throw IllegalStateException("Failed to connect to daemon: ${error.message}")
                },
                {
                    connected = true
                    logger.info("Connected to daemon via gRPC")
                },
            )
        }
    }

    // Scope Management Operations

    override suspend fun createScope(
        title: String,
        description: String?,
        parentId: String?,
        customAlias: String?,
    ): Either<ScopeContractError, CreateScopeResult> {
        logger.debug(
            "GrpcTransport: createScope",
            mapOf(
                "title" to title,
                "parentId" to parentId,
                "customAlias" to customAlias,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.createScope(title, description, parentId, customAlias)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error creating scope via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun getScope(id: String): Either<ScopeContractError, ScopeResult?> {
        logger.debug("GrpcTransport: getScope", mapOf("id" to id) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.getScope(id)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error getting scope via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun updateScope(id: String, title: String?, description: String?): Either<ScopeContractError, UpdateScopeResult> {
        logger.debug(
            "GrpcTransport: updateScope",
            mapOf(
                "id" to id,
                "title" to title,
                "description" to description,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.updateScope(id, title, description, null)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error updating scope via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun deleteScope(id: String, cascade: Boolean): Either<ScopeContractError, DeleteScopeResult> {
        logger.debug(
            "GrpcTransport: deleteScope",
            mapOf(
                "id" to id,
                "cascade" to cascade,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.deleteScope(id, cascade)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error deleting scope via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun getRootScopes(): Either<ScopeContractError, ScopeListResult> {
        logger.debug("GrpcTransport: getRootScopes")

        return try {
            ensureConnected()
            gatewayClient.getRootScopes()
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error getting root scopes via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun getChildren(parentId: String, includeDescendants: Boolean): Either<ScopeContractError, ScopeListResult> {
        logger.debug(
            "GrpcTransport: getChildren",
            mapOf(
                "parentId" to parentId,
                "includeDescendants" to includeDescendants,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.getChildren(parentId, includeDescendants)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error getting children via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun listScopes(): Either<ScopeContractError, ScopeListResult> {
        logger.debug("GrpcTransport: listScopes")

        return try {
            ensureConnected()
            gatewayClient.listScopes()
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error listing scopes via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    // Alias Operations

    override suspend fun addAlias(scopeOrAlias: String, alias: String): Either<ScopeContractError, AddAliasResult> {
        logger.debug(
            "GrpcTransport: addAlias",
            mapOf(
                "scopeOrAlias" to scopeOrAlias,
                "alias" to alias,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            // Resolve scope ID from alias if needed via gRPC
            val scopeId = resolveParameterViaGrpc(scopeOrAlias).fold(
                { error -> return Either.Left(error) },
                { id -> id },
            )
            gatewayClient.addAlias(scopeId, alias)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error adding alias via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun removeAlias(scopeId: String, alias: String): Either<ScopeContractError, RemoveAliasResult> {
        logger.debug("GrpcTransport: removeAlias", mapOf("scopeId" to scopeId, "alias" to alias) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.removeAlias(scopeId, alias)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error removing alias via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun setCanonicalAlias(scopeId: String, newCanonicalAlias: String): Either<ScopeContractError, RenameAliasResult> {
        logger.debug(
            "GrpcTransport: setCanonicalAlias",
            mapOf(
                "scopeId" to scopeId,
                "newCanonicalAlias" to newCanonicalAlias,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.setCanonicalAlias(scopeId, newCanonicalAlias)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error setting canonical alias via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun listAliases(scopeOrAlias: String): Either<ScopeContractError, AliasListResult> {
        logger.debug("GrpcTransport: listAliases", mapOf("scopeOrAlias" to scopeOrAlias) as Map<String, Any>)

        return try {
            ensureConnected()
            // Resolve scope ID from alias if needed via gRPC
            val scopeId = resolveParameterViaGrpc(scopeOrAlias).fold(
                { error -> return Either.Left(error) },
                { id -> id },
            )
            gatewayClient.listAliases(scopeId)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error listing aliases via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun resolveAlias(aliasOrPrefix: String): Either<ScopeContractError, ResolveAliasResult> {
        logger.debug("GrpcTransport: resolveAlias", mapOf("aliasOrPrefix" to aliasOrPrefix) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.getScopeByAlias(aliasOrPrefix)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
                .map { scopeResult ->
                    scopeResult?.let { scope ->
                        ResolveAliasResult(
                            scopeId = scope.id,
                            matchedAlias = scope.canonicalAlias,
                            wasPrefixMatch = false,
                            otherMatches = emptyList()
                        )
                    } ?: throw IllegalStateException("Alias '$aliasOrPrefix' not found")
                }
        } catch (e: Exception) {
            logger.error("Error resolving alias via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    // Context View Operations

    override suspend fun createContextView(
        key: String,
        name: String,
        description: String?,
        filter: String,
    ): Either<ScopeContractError, CreateContextViewResult> {
        logger.debug(
            "GrpcTransport: createContextView",
            mapOf(
                "key" to key,
                "name" to name,
                "filter" to filter,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.createContextView(key, name, description, filter)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error creating context view via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun getContextView(key: String): Either<ScopeContractError, ContextViewResult?> {
        logger.debug("GrpcTransport: getContextView", mapOf("key" to key) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.getContextView(key)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error getting context view via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun updateContextView(
        key: String,
        name: String?,
        description: String?,
        filter: String?,
    ): Either<ScopeContractError, UpdateContextViewResult> {
        logger.debug(
            "GrpcTransport: updateContextView",
            mapOf(
                "key" to key,
                "name" to name,
                "filter" to filter,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.updateContextView(key, name, description, filter)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error updating context view via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun deleteContextView(key: String): Either<ScopeContractError, DeleteContextViewResult> {
        logger.debug("GrpcTransport: deleteContextView", mapOf("key" to key) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.deleteContextView(key)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error deleting context view via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun listContextViews(): Either<ScopeContractError, ContextViewListResult> {
        logger.debug("GrpcTransport: listContextViews")

        return try {
            ensureConnected()
            gatewayClient.listContextViews()
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error listing context views via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun getCurrentContextView(): Either<ScopeContractError, ContextViewResult?> {
        logger.debug("GrpcTransport: getCurrentContextView")

        return try {
            ensureConnected()
            gatewayClient.getCurrentContextView()
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error getting current context view via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun switchContextView(key: String): Either<ScopeContractError, SwitchContextViewResult> {
        logger.debug("GrpcTransport: switchContextView", mapOf("key" to key) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.switchContextView(key)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error switching context view via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun clearCurrentContextView(): Either<ScopeContractError, ClearContextViewResult> {
        logger.debug("GrpcTransport: clearCurrentContextView")

        return try {
            ensureConnected()
            gatewayClient.clearCurrentContextView()
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error clearing current context view via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    // Aspect Definition Operations

    override suspend fun createAspectDefinition(key: String, description: String, type: String): Either<ScopeContractError, Unit> {
        logger.debug(
            "GrpcTransport: createAspectDefinition",
            mapOf(
                "key" to key,
                "description" to description,
                "type" to type,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.createAspectDefinition(key, description, type)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error creating aspect definition via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun getAspectDefinition(key: String): Either<ScopeContractError, AspectDefinition?> {
        logger.debug("GrpcTransport: getAspectDefinition", mapOf("key" to key) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.getAspectDefinition(key)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error getting aspect definition via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun updateAspectDefinition(key: String, description: String?): Either<ScopeContractError, Unit> {
        logger.debug(
            "GrpcTransport: updateAspectDefinition",
            mapOf(
                "key" to key,
                "description" to description,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.updateAspectDefinition(key, description)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error updating aspect definition via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun deleteAspectDefinition(key: String): Either<ScopeContractError, Unit> {
        logger.debug("GrpcTransport: deleteAspectDefinition", mapOf("key" to key) as Map<String, Any>)

        return try {
            ensureConnected()
            gatewayClient.deleteAspectDefinition(key)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error deleting aspect definition via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun listAspectDefinitions(): Either<ScopeContractError, List<AspectDefinition>> {
        logger.debug("GrpcTransport: listAspectDefinitions")

        return try {
            ensureConnected()
            gatewayClient.listAspectDefinitions()
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error listing aspect definitions via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    override suspend fun validateAspectValue(key: String, values: List<String>): Either<ScopeContractError, List<String>> {
        logger.debug(
            "GrpcTransport: validateAspectValue",
            mapOf(
                "key" to key,
                "values" to values,
            ) as Map<String, Any>,
        )

        return try {
            ensureConnected()
            gatewayClient.validateAspectValue(key, values)
                .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
        } catch (e: Exception) {
            logger.error("Error validating aspect value via gRPC", mapOf(), e)
            Either.Left(mapExceptionToContractError(e))
        }
    }

    // User Preferences

    override suspend fun getPreferences(): Either<UserPreferencesContractError, PreferenceResult> {
        logger.debug("GrpcTransport: getPreferences")

        // TODO: Implement when preference queries are added to gateway
        return Either.Left(
            UserPreferencesContractError.DataError.PreferencesCorrupted(
                details = "gRPC preference operations not yet implemented",
            ),
        )
    }

    // Streaming Operations

    /**
     * Subscribes to real-time event stream.
     */
    suspend fun subscribeToEvents(): Either<GatewayClient.ClientError, kotlinx.coroutines.flow.Flow<GatewayClient.StreamEvent>> {
        logger.debug("GrpcTransport: subscribeToEvents")
        ensureConnected()
        return gatewayClient.subscribeToEvents()
    }

    // Lifecycle Operations

    override suspend fun isAvailable(): Boolean = try {
        ensureConnected()
        true // If we're connected, we're available
    } catch (e: Exception) {
        logger.warn("gRPC transport not available", mapOf("error" to e.message) as Map<String, Any>)
        false
    }

    override suspend fun disconnect() {
        logger.debug("GrpcTransport: disconnect")
        gatewayClient.disconnect()
        connected = false
    }

    // Helper Methods

    /**
     * Resolves a parameter (ULID or alias) to a scope ID via gRPC.
     * This replaces local SQLite-based parameter resolution for native builds.
     */
    private suspend fun resolveParameterViaGrpc(parameter: String): Either<ScopeContractError, String> {
        // First, check if it's a valid ULID
        return when {
            isValidUlid(parameter) -> {
                // It's a ULID, return as-is
                Either.Right(parameter)
            }
            else -> {
                // Try to resolve as alias via gRPC
                gatewayClient.getScopeByAlias(parameter)
                    .mapLeft { clientError -> mapClientErrorToContractError(clientError) }
                    .map { scopeResult -> 
                        scopeResult?.id ?: throw IllegalStateException("Alias '$parameter' resolved to null scope")
                    }
            }
        }
    }

    /**
     * Checks if a string is a valid ULID format.
     * ULIDs are 26 characters long and contain only valid ULID characters.
     */
    private fun isValidUlid(value: String): Boolean {
        if (value.length != 26) return false

        // ULID uses Crockford's base32: 0123456789ABCDEFGHJKMNPQRSTVWXYZ
        val validChars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        return value.all { it.uppercaseChar() in validChars }
    }

    private fun mapExceptionToContractError(e: Exception): ScopeContractError = when (e) {
        is StatusException -> GrpcStatusDetailsMapper.mapStatusExceptionToContractError(e)
        is IllegalStateException -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "gRPC transport",
        )
        else -> ScopeContractError.SystemError.ServiceUnavailable(
            service = "gRPC transport",
        )
    }

    private fun mapStatusToContractError(status: Status, message: String?): ScopeContractError = when (status.code) {
        Status.Code.INVALID_ARGUMENT -> {
            // Try to extract field information from message
            // This is a simplified version - in production, use structured error details
            ScopeContractError.InputError.ValidationFailure(
                field = "unknown",
                value = "unknown",
                constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                    expectedValues = null,
                    actualValue = "unknown",
                ),
            )
        }
        Status.Code.NOT_FOUND -> {
            // Try to extract resource ID from message
            ScopeContractError.BusinessError.NotFound(
                scopeId = message?.substringAfter("Scope not found: ")?.trim() ?: "unknown",
            )
        }
        Status.Code.ALREADY_EXISTS -> {
            // Try to extract duplicate information from message
            val titleMatch = Regex("Title '(.+)' already exists").find(message ?: "")
            val title = titleMatch?.groupValues?.getOrNull(1) ?: "unknown"
            ScopeContractError.BusinessError.DuplicateTitle(
                title = title,
                parentId = null,
                existingScopeId = null,
            )
        }
        Status.Code.UNAVAILABLE -> {
            ScopeContractError.SystemError.ServiceUnavailable(
                service = message?.substringAfter("Service '")?.substringBefore("' is") ?: "daemon",
            )
        }
        Status.Code.DEADLINE_EXCEEDED -> {
            ScopeContractError.SystemError.Timeout(
                operation = "gRPC call",
                timeout = Duration.parse("30s"), // Default gRPC timeout
            )
        }
        else -> {
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "gRPC transport",
            )
        }
    }

    private fun mapClientErrorToContractError(clientError: GatewayClient.ClientError): ScopeContractError = when (clientError) {
        is GatewayClient.ClientError.ConnectionError ->
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "gRPC daemon connection",
            )
        is GatewayClient.ClientError.ServiceError -> {
            val statusException = clientError.statusException
            if (statusException != null) {
                // Use the detailed mapper if we have the original StatusException
                GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)
            } else {
                // Fall back to basic mapping
                mapStatusToContractError(
                    Status.fromCodeValue(clientError.code.value()),
                    clientError.message,
                )
            }
        }
        is GatewayClient.ClientError.Unexpected ->
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "gRPC transport",
            )
    }
}
