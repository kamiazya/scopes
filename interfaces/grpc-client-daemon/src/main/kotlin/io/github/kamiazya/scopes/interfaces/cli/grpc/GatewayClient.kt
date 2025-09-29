package io.github.kamiazya.scopes.interfaces.cli.grpc

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AddAliasResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.AliasInfo
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
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.SwitchContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateContextViewResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition
import io.github.kamiazya.scopes.platform.infrastructure.grpc.ChannelBuilder
import io.github.kamiazya.scopes.platform.infrastructure.grpc.ChannelWithEventLoop
import io.github.kamiazya.scopes.platform.infrastructure.grpc.EndpointResolver
import io.github.kamiazya.scopes.platform.infrastructure.grpc.RetryPolicy
import io.github.kamiazya.scopes.platform.infrastructure.grpc.interceptors.RetryUtils
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.rpc.v1beta.Envelope
import io.github.kamiazya.scopes.rpc.v1beta.TaskGatewayServiceGrpcKt
import io.grpc.StatusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class GatewayClient(
    private val endpointResolver: EndpointResolver,
    private val logger: Logger,
    private val json: Json = Json,
    private val retryPolicy: RetryPolicy = RetryPolicy.fromEnvironment(logger),
) {
    private var channelWithEventLoop: ChannelWithEventLoop? = null
    private var stub: TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineStub? = null

    private fun generateId(): String = Random.nextLong().toString(16)

    suspend fun connect(useRetry: Boolean = true): Either<ClientError, Unit> = if (useRetry) {
        retryPolicy.execute<ClientError, Unit>("gateway-connection") { attemptNumber ->
            performConnect(attemptNumber)
        }
    } else {
        performConnect(1)
    }

    private suspend fun performConnect(attemptNumber: Int): Either<ClientError, Unit> = try {
        val endpoint = endpointResolver.resolve().getOrElse {
            return ClientError.ConnectionError(it.message ?: "endpoint error").left()
        }

        logger.debug(
            "Connecting to gateway",
            mapOf(
                "host" to endpoint.host,
                "port" to endpoint.port.toString(),
                "attempt" to attemptNumber,
            ),
        )

        // Create channel with EventLoopGroup using common builder
        val channelWithEventLoop = ChannelBuilder.createChannelWithEventLoop(
            host = endpoint.host,
            port = endpoint.port,
            logger = logger,
            // Uses default timeout from environment or 30 seconds
        )

        this.channelWithEventLoop = channelWithEventLoop
        stub = TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineStub(channelWithEventLoop.channel)
        logger.info("Connected to gateway", mapOf("address" to "${endpoint.host}:${endpoint.port}"))
        Unit.right()
    } catch (e: Exception) {
        logger.error("Gateway connect failed", mapOf("error" to e.javaClass.simpleName, "attempt" to attemptNumber), e)
        ClientError.ConnectionError(e.message ?: "connect failed", e).left()
    }

    suspend fun disconnect() {
        // Properly shutdown both channel and EventLoopGroup
        channelWithEventLoop?.shutdown()
        channelWithEventLoop = null
        stub = null
    }

    suspend fun createScope(title: String, description: String?, parentId: String?, customAlias: String?): Either<ClientError, CreateScopeResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(CreateScopeCommandData(title, description, parentId, customAlias))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.CreateScope")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            RetryUtils.withRetry {
                val res = s.executeCommand(req)
                // Decode response payload
                val jsonText = res.payload.toStringUtf8()
                val r = json.decodeFromString(CreateScopeResponseData.serializer(), jsonText)
                val created = Instant.parse(r.createdAt)
                CreateScopeResult(
                    id = r.id,
                    title = r.title,
                    description = r.description,
                    parentId = r.parentId,
                    canonicalAlias = r.canonicalAlias,
                    createdAt = created,
                    updatedAt = created,
                ).right()
            }
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun getScope(id: String): Either<ClientError, ScopeResult?> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(GetScopeQueryData(id))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.GetScope")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            RetryUtils.withRetry {
                val res = s.query(req)
                // Decode response payload
                val jsonText = res.payload.toStringUtf8()
                val r = json.decodeFromString(GetScopeResponseData.serializer(), jsonText)
                val created = Instant.parse(r.createdAt)
                val updated = Instant.parse(r.updatedAt)
                ScopeResult(
                    id = r.id,
                    title = r.title,
                    description = r.description,
                    parentId = r.parentId,
                    canonicalAlias = r.canonicalAlias,
                    createdAt = created,
                    updatedAt = updated,
                    isArchived = r.isArchived,
                    aspects = r.aspects,
                ).right()
            }
        } catch (e: StatusException) {
            // NOT_FOUND means scope doesn't exist, which should return null
            if (e.status.code == io.grpc.Status.Code.NOT_FOUND) {
                null.right()
            } else {
                ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
            }
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun getRootScopes(offset: Int = 0, limit: Int = 50): Either<ClientError, ScopeListResult> = RetryUtils.withRetry {
        val s = stub ?: return@withRetry ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(GetRootScopesQueryData(offset, limit))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.GetRootScopes")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        try {
            val res = s.query(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(GetRootScopesResponseData.serializer(), jsonText)
            val scopes = r.scopes.map { scope ->
                val created = Instant.parse(scope.createdAt)
                val updated = Instant.parse(scope.updatedAt)
                ScopeResult(
                    id = scope.id,
                    title = scope.title,
                    description = scope.description,
                    parentId = scope.parentId,
                    canonicalAlias = scope.canonicalAlias,
                    createdAt = created,
                    updatedAt = updated,
                    isArchived = scope.isArchived,
                    aspects = scope.aspects,
                )
            }
            ScopeListResult(
                scopes = scopes,
                totalCount = r.totalCount,
                offset = offset,
                limit = limit,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun getChildren(parentId: String, includeDescendants: Boolean = false, offset: Int = 0, limit: Int = 50): Either<ClientError, ScopeListResult> =
        RetryUtils.withRetry {
            val s = stub ?: return@withRetry ClientError.ConnectionError("Not connected").left()

            val payload = json.encodeToString(GetChildrenQueryData(parentId, includeDescendants, offset, limit))
            val req = Envelope.newBuilder()
                .setId(generateId())
                .setKind("scopes.queries.GetChildren")
                .setVersion("v1")
                .setCorrelationId(generateId())
                .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
                .build()

            try {
                val res = s.query(req)
                // Decode response payload
                val jsonText = res.payload.toStringUtf8()
                val r = json.decodeFromString(GetChildrenResponseData.serializer(), jsonText)
                val scopes = r.scopes.map { scope ->
                    val created = Instant.parse(scope.createdAt)
                    val updated = Instant.parse(scope.updatedAt)
                    ScopeResult(
                        id = scope.id,
                        title = scope.title,
                        description = scope.description,
                        parentId = scope.parentId,
                        canonicalAlias = scope.canonicalAlias,
                        createdAt = created,
                        updatedAt = updated,
                        isArchived = scope.isArchived,
                        aspects = scope.aspects,
                    )
                }
                ScopeListResult(
                    scopes = scopes,
                    totalCount = r.totalCount,
                    offset = offset,
                    limit = limit,
                ).right()
            } catch (e: StatusException) {
                ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
            } catch (e: Exception) {
                ClientError.Unexpected(e.message ?: "unexpected", e).left()
            }
        }

    suspend fun listScopes(offset: Int = 0, limit: Int = 50): Either<ClientError, ScopeListResult> = RetryUtils.withRetry {
        val s = stub ?: return@withRetry ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(ListScopesQueryData(offset, limit))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.ListScopes")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        try {
            val res = s.query(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(ListScopesResponseData.serializer(), jsonText)
            val scopes = r.scopes.map { scope ->
                val created = Instant.parse(scope.createdAt)
                val updated = Instant.parse(scope.updatedAt)
                ScopeResult(
                    id = scope.id,
                    title = scope.title,
                    description = scope.description,
                    parentId = scope.parentId,
                    canonicalAlias = scope.canonicalAlias,
                    createdAt = created,
                    updatedAt = updated,
                    isArchived = scope.isArchived,
                    aspects = scope.aspects,
                )
            }
            ScopeListResult(
                scopes = scopes,
                totalCount = r.totalCount,
                offset = offset,
                limit = limit,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun updateScope(id: String, title: String?, description: String?, parentId: String?): Either<ClientError, UpdateScopeResult> =
        RetryUtils.withRetry {
            val s = stub ?: return@withRetry ClientError.ConnectionError("Not connected").left()

            val payload = json.encodeToString(UpdateScopeCommandData(id, title, description, parentId))
            val req = Envelope.newBuilder()
                .setId(generateId())
                .setKind("scopes.commands.UpdateScope")
                .setVersion("v1")
                .setCorrelationId(generateId())
                .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
                .build()

            try {
                val res = s.executeCommand(req)
                // Decode response payload
                val jsonText = res.payload.toStringUtf8()
                val r = json.decodeFromString(UpdateScopeResponseData.serializer(), jsonText)
                val created = Instant.parse(r.createdAt)
                val updated = Instant.parse(r.updatedAt)
                UpdateScopeResult(
                    id = r.id,
                    title = r.title,
                    description = r.description,
                    parentId = r.parentId,
                    canonicalAlias = r.canonicalAlias,
                    createdAt = created,
                    updatedAt = updated,
                ).right()
            } catch (e: StatusException) {
                ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
            } catch (e: Exception) {
                ClientError.Unexpected(e.message ?: "unexpected", e).left()
            }
        }

    suspend fun deleteScope(id: String, cascade: Boolean = false): Either<ClientError, DeleteScopeResult> = RetryUtils.withRetry {
        val s = stub ?: return@withRetry ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(DeleteScopeCommandData(id, cascade))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.DeleteScope")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        try {
            val res = s.executeCommand(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(DeleteScopeResponseData.serializer(), jsonText)
            DeleteScopeResult(
                deletedScopeId = r.deletedScopeId,
                deletedChildrenCount = r.deletedChildrenCount,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun addAlias(scopeId: String, aliasName: String): Either<ClientError, AddAliasResult> = RetryUtils.withRetry {
        val s = stub ?: return@withRetry ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(AddAliasCommandData(scopeId, aliasName))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.AddAlias")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        try {
            val res = s.executeCommand(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(AddAliasResponseData.serializer(), jsonText)
            AddAliasResult(
                scopeId = r.scopeId,
                alias = r.aliasName,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun removeAlias(scopeId: String, aliasName: String): Either<ClientError, RemoveAliasResult> = RetryUtils.withRetry {
        val s = stub ?: return@withRetry ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(RemoveAliasCommandData(scopeId, aliasName))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.RemoveAlias")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        try {
            val res = s.executeCommand(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(RemoveAliasResponseData.serializer(), jsonText)
            RemoveAliasResult(
                scopeId = r.scopeId,
                removedAlias = r.aliasName,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun setCanonicalAlias(scopeId: String, aliasName: String): Either<ClientError, RenameAliasResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(SetCanonicalAliasCommandData(scopeId, aliasName))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.SetCanonicalAlias")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.executeCommand(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(SetCanonicalAliasResponseData.serializer(), jsonText)
            RenameAliasResult(
                scopeId = r.scopeId,
                oldCanonicalAlias = "", // Note: Not provided by response, would need to be tracked
                newCanonicalAlias = r.aliasName,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun listAliases(scopeId: String): Either<ClientError, AliasListResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(ListAliasesQueryData(scopeId))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.ListAliases")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.query(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(ListAliasesResponseData.serializer(), jsonText)
            AliasListResult(
                scopeId = r.scopeId,
                aliases = r.aliases.map { alias ->
                    AliasInfo(
                        aliasName = alias.name,
                        aliasType = if (alias.isCanonical) "canonical" else "custom",
                        isCanonical = alias.isCanonical,
                        createdAt = Instant.parse(alias.createdAt),
                    )
                },
                totalCount = r.aliases.size,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun getScopeByAlias(aliasName: String): Either<ClientError, ScopeResult?> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(GetScopeByAliasQueryData(aliasName))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.GetScopeByAlias")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.query(req)
            // Decode response payload
            val jsonText = res.payload.toStringUtf8()
            if (jsonText == "null") {
                null.right()
            } else {
                val r = json.decodeFromString(GetScopeResponseData.serializer(), jsonText)
                val created = Instant.parse(r.createdAt)
                val updated = Instant.parse(r.updatedAt)
                ScopeResult(
                    id = r.id,
                    title = r.title,
                    description = r.description,
                    parentId = r.parentId,
                    canonicalAlias = r.canonicalAlias,
                    createdAt = created,
                    updatedAt = updated,
                    isArchived = r.isArchived,
                    aspects = r.aspects,
                ).right()
            }
        } catch (e: StatusException) {
            // NOT_FOUND means alias doesn't exist, which should return null
            if (e.status.code == io.grpc.Status.Code.NOT_FOUND) {
                null.right()
            } else {
                ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
            }
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    // Context View Operations

    suspend fun createContextView(key: String, name: String, description: String?, filter: String): Either<ClientError, CreateContextViewResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(CreateContextViewCommandData(key, name, filter, description))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.CreateContextView")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.executeCommand(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(CreateContextViewResponseData.serializer(), jsonText)
            CreateContextViewResult(
                key = r.key,
                name = r.name,
                description = r.description,
                filter = r.filter,
                createdAt = Instant.parse(r.createdAt),
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun getContextView(key: String): Either<ClientError, ContextViewResult?> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(GetContextViewQueryData(key))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.GetContextView")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.query(req)
            val jsonText = res.payload.toStringUtf8()
            if (jsonText == "null") {
                null.right()
            } else {
                val r = json.decodeFromString(ContextViewData.serializer(), jsonText)
                ContextViewResult(
                    key = r.key,
                    name = r.name,
                    description = r.description,
                    filter = r.filter,
                    createdAt = Instant.parse(r.createdAt),
                    updatedAt = Instant.parse(r.updatedAt),
                    isActive = r.isActive,
                ).right()
            }
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun updateContextView(key: String, name: String?, description: String?, filter: String?): Either<ClientError, UpdateContextViewResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(UpdateContextViewCommandData(key, name, filter, description))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.UpdateContextView")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.executeCommand(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(UpdateContextViewResponseData.serializer(), jsonText)
            UpdateContextViewResult(
                key = r.key,
                name = r.name,
                description = r.description,
                filter = r.filter,
                updatedAt = Instant.parse(r.updatedAt),
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun deleteContextView(key: String): Either<ClientError, DeleteContextViewResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(DeleteContextViewCommandData(key))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.DeleteContextView")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.executeCommand(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(DeleteContextViewResponseData.serializer(), jsonText)
            DeleteContextViewResult(
                deletedKey = r.key,
                wasActive = false, // TODO: Get from response when available
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun listContextViews(): Either<ClientError, ContextViewListResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.ListContextViews")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8("{}"))
            .build()

        return try {
            val res = s.query(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(ContextViewListData.serializer(), jsonText)
            val contextViews = r.contextViews.map { cv ->
                ContextViewResult(
                    key = cv.key,
                    name = cv.name,
                    description = cv.description,
                    filter = cv.filter,
                    createdAt = Instant.parse(cv.createdAt),
                    updatedAt = Instant.parse(cv.updatedAt),
                    isActive = cv.isActive,
                )
            }
            ContextViewListResult(
                contextViews = contextViews,
                totalCount = contextViews.size,
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun getCurrentContextView(): Either<ClientError, ContextViewResult?> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.GetActiveContext")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8("{}"))
            .build()

        return try {
            val res = s.query(req)
            val jsonText = res.payload.toStringUtf8()
            if (jsonText == "null") {
                null.right()
            } else {
                val r = json.decodeFromString(ContextViewData.serializer(), jsonText)
                ContextViewResult(
                    key = r.key,
                    name = r.name,
                    description = r.description,
                    filter = r.filter,
                    createdAt = Instant.parse(r.createdAt),
                    updatedAt = Instant.parse(r.updatedAt),
                    isActive = r.isActive,
                ).right()
            }
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun switchContextView(key: String): Either<ClientError, SwitchContextViewResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(SetActiveContextCommandData(key))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.SetActiveContext")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.executeCommand(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(SetActiveContextResponseData.serializer(), jsonText)
            SwitchContextViewResult(
                previousKey = null, // TODO: Get from response when available
                newKey = r.key,
                filter = "", // TODO: Get from response when available
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun clearCurrentContextView(): Either<ClientError, ClearContextViewResult> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.ClearActiveContext")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8("{}"))
            .build()

        return try {
            val res = s.executeCommand(req)
            ClearContextViewResult(
                clearedKey = null, // TODO: Get from response when available
            ).right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    // Aspect Definition Operations

    suspend fun createAspectDefinition(key: String, description: String, type: String): Either<ClientError, Unit> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(CreateAspectDefinitionCommandData(key, description, type))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.CreateAspectDefinition")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            s.executeCommand(req)
            Unit.right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun getAspectDefinition(key: String): Either<ClientError, AspectDefinition?> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(GetAspectDefinitionQueryData(key))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.GetAspectDefinition")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.query(req)
            val jsonText = res.payload.toStringUtf8()
            if (jsonText == "null") {
                null.right()
            } else {
                val r = json.decodeFromString(AspectDefinitionData.serializer(), jsonText)
                AspectDefinition(
                    key = r.key,
                    description = r.description,
                    type = r.type,
                    createdAt = Instant.parse(r.createdAt),
                    updatedAt = Instant.parse(r.updatedAt),
                ).right()
            }
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun updateAspectDefinition(key: String, description: String?): Either<ClientError, Unit> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(UpdateAspectDefinitionCommandData(key, description))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.UpdateAspectDefinition")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            s.executeCommand(req)
            Unit.right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun deleteAspectDefinition(key: String): Either<ClientError, Unit> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(DeleteAspectDefinitionCommandData(key))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.commands.DeleteAspectDefinition")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            s.executeCommand(req)
            Unit.right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun listAspectDefinitions(): Either<ClientError, List<AspectDefinition>> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.ListAspectDefinitions")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8("{}"))
            .build()

        return try {
            val res = s.query(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(AspectDefinitionListData.serializer(), jsonText)
            r.aspectDefinitions.map { ad ->
                AspectDefinition(
                    key = ad.key,
                    description = ad.description,
                    type = ad.type,
                    createdAt = Instant.parse(ad.createdAt),
                    updatedAt = Instant.parse(ad.updatedAt),
                )
            }.right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    suspend fun validateAspectValue(key: String, values: List<String>): Either<ClientError, List<String>> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        val payload = json.encodeToString(ValidateAspectValueQueryData(key, values))
        val req = Envelope.newBuilder()
            .setId(generateId())
            .setKind("scopes.queries.ValidateAspectValue")
            .setVersion("v1")
            .setCorrelationId(generateId())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = s.query(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(ValidateAspectValueResponseData.serializer(), jsonText)
            r.values.right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            ClientError.Unexpected(e.message ?: "unexpected", e).left()
        }
    }

    // Streaming Operations

    suspend fun subscribeToEvents(): Either<ClientError, Flow<StreamEvent>> {
        val s = stub ?: return ClientError.ConnectionError("Not connected").left()

        return try {
            val subscribeRequest = Envelope.newBuilder()
                .setId(generateId())
                .setKind("scopes.stream.Subscribe")
                .setVersion("v1")
                .setCorrelationId(generateId())
                .setPayload(
                    com.google.protobuf.ByteString.copyFromUtf8(
                        json.encodeToString(
                            SubscribeData(message = "Subscribe to event stream"),
                        ),
                    ),
                )
                .build()

            val requestFlow = flowOf(subscribeRequest)
            val responseFlow = s.streamEvents(requestFlow)

            val eventFlow = flow {
                responseFlow.collect { envelope ->
                    logger.debug(
                        "Received stream event",
                        mapOf(
                            "envelope.id" to envelope.id,
                            "envelope.kind" to envelope.kind,
                            "envelope.correlationId" to envelope.correlationId,
                        ) as Map<String, Any>,
                    )

                    val eventData = envelope.payload.toStringUtf8()
                    val event = when (envelope.kind) {
                        "scopes.events.Connected" -> {
                            val data = json.decodeFromString<ConnectedEventData>(eventData)
                            StreamEvent.Connected(
                                message = data.message,
                                timestamp = data.timestamp,
                            )
                        }

                        "scopes.events.ProgressUpdate" -> {
                            val data = json.decodeFromString<ProgressUpdateEventData>(eventData)
                            StreamEvent.ProgressUpdate(
                                operationId = data.operationId,
                                percentage = data.percentage,
                                message = data.message,
                                estimatedSecondsRemaining = data.estimatedSecondsRemaining,
                                metadata = data.metadata,
                            )
                        }

                        "scopes.events.OperationCompleted" -> {
                            val data = json.decodeFromString<OperationCompletedEventData>(eventData)
                            StreamEvent.OperationCompleted(
                                operationId = data.operationId,
                                message = data.message,
                                timestamp = data.timestamp,
                            )
                        }

                        "scopes.events.Error" -> {
                            val data = json.decodeFromString<ErrorEventData>(eventData)
                            StreamEvent.Error(
                                error = data.error,
                                timestamp = data.timestamp,
                            )
                        }

                        else -> {
                            StreamEvent.Unknown(
                                kind = envelope.kind,
                                data = eventData,
                            )
                        }
                    }

                    emit(event)
                }
            }

            eventFlow.right()
        } catch (e: StatusException) {
            ClientError.ServiceError(e.status.code, e.status.description ?: "", e).left()
        } catch (e: Exception) {
            logger.error("Error subscribing to events", emptyMap(), e)
            ClientError.Unexpected(e.message ?: "streaming error", e).left()
        }
    }

    sealed class ClientError(message: String, cause: Throwable? = null) : Exception(message, cause) {
        class ConnectionError(message: String, cause: Throwable? = null) : ClientError(message, cause)
        class ServiceError(val code: io.grpc.Status.Code, message: String, val statusException: StatusException? = null) :
            ClientError(message, statusException)
        class Unexpected(message: String, cause: Throwable? = null) : ClientError(message, cause)
    }

    @Serializable
    data class CreateScopeCommandData(val title: String, val description: String? = null, val parentId: String? = null, val customAlias: String? = null)

    @Serializable
    data class CreateScopeResponseData(
        val id: String,
        val title: String,
        val description: String? = null,
        val parentId: String? = null,
        val canonicalAlias: String,
        val createdAt: String,
    )

    @Serializable
    data class GetScopeQueryData(val id: String)

    @Serializable
    data class GetScopeByAliasQueryData(val aliasName: String)

    @Serializable
    data class GetScopeResponseData(
        val id: String,
        val title: String,
        val description: String?,
        val parentId: String?,
        val canonicalAlias: String,
        val createdAt: String,
        val updatedAt: String,
        val isArchived: Boolean = false,
        val aspects: Map<String, List<String>> = emptyMap(),
    )

    @Serializable
    data class GetRootScopesQueryData(val offset: Int = 0, val limit: Int = 50)

    @Serializable
    data class GetChildrenQueryData(val parentId: String, val includeDescendants: Boolean = false, val offset: Int = 0, val limit: Int = 50)

    @Serializable
    data class ListScopesQueryData(val offset: Int = 0, val limit: Int = 50)

    @Serializable
    data class ScopeResponseData(
        val id: String,
        val title: String,
        val description: String?,
        val parentId: String?,
        val canonicalAlias: String,
        val createdAt: String,
        val updatedAt: String,
        val isArchived: Boolean = false,
        val aspects: Map<String, List<String>> = emptyMap(),
    )

    @Serializable
    data class GetRootScopesResponseData(val scopes: List<ScopeResponseData>, val totalCount: Int, val hasMore: Boolean)

    @Serializable
    data class GetChildrenResponseData(val scopes: List<ScopeResponseData>, val totalCount: Int, val hasMore: Boolean)

    @Serializable
    data class ListScopesResponseData(val scopes: List<ScopeResponseData>, val totalCount: Int, val hasMore: Boolean)

    @Serializable
    data class UpdateScopeCommandData(val id: String, val title: String? = null, val description: String? = null, val parentId: String? = null)

    @Serializable
    data class UpdateScopeResponseData(
        val id: String,
        val title: String,
        val description: String?,
        val parentId: String?,
        val canonicalAlias: String,
        val createdAt: String,
        val updatedAt: String,
    )

    @Serializable
    data class DeleteScopeCommandData(val id: String, val cascade: Boolean = false)

    @Serializable
    data class DeleteScopeResponseData(val deletedScopeId: String, val deletedChildrenCount: Int)

    @Serializable
    data class AddAliasCommandData(val scopeId: String, val aliasName: String)

    @Serializable
    data class AddAliasResponseData(val scopeId: String, val aliasName: String)

    @Serializable
    data class RemoveAliasCommandData(val scopeId: String, val aliasName: String)

    @Serializable
    data class RemoveAliasResponseData(val scopeId: String, val aliasName: String)

    @Serializable
    data class SetCanonicalAliasCommandData(val scopeId: String, val aliasName: String)

    @Serializable
    data class SetCanonicalAliasResponseData(val scopeId: String, val aliasName: String)

    // Context View Data Classes

    @Serializable
    data class CreateContextViewCommandData(val key: String, val name: String, val filter: String, val description: String? = null)

    @Serializable
    data class CreateContextViewResponseData(val key: String, val name: String, val filter: String, val description: String?, val createdAt: String)

    @Serializable
    data class GetContextViewQueryData(val key: String)

    @Serializable
    data class ContextViewData(
        val key: String,
        val name: String,
        val filter: String,
        val description: String?,
        val createdAt: String,
        val updatedAt: String,
        val isActive: Boolean = false,
    )

    @Serializable
    data class UpdateContextViewCommandData(val key: String, val name: String? = null, val filter: String? = null, val description: String? = null)

    @Serializable
    data class UpdateContextViewResponseData(val key: String, val name: String, val filter: String, val description: String?, val updatedAt: String)

    @Serializable
    data class DeleteContextViewCommandData(val key: String)

    @Serializable
    data class DeleteContextViewResponseData(val key: String)

    @Serializable
    data class SetActiveContextCommandData(val key: String)

    @Serializable
    data class SetActiveContextResponseData(val key: String)

    @Serializable
    data class ContextViewListData(val contextViews: List<ContextViewData>)

    // Aspect Definition Data Classes

    @Serializable
    data class CreateAspectDefinitionCommandData(val key: String, val description: String, val type: String)

    @Serializable
    data class GetAspectDefinitionQueryData(val key: String)

    @Serializable
    data class AspectDefinitionData(val key: String, val description: String, val type: String, val createdAt: String, val updatedAt: String)

    @Serializable
    data class UpdateAspectDefinitionCommandData(val key: String, val description: String? = null)

    @Serializable
    data class DeleteAspectDefinitionCommandData(val key: String)

    @Serializable
    data class AspectDefinitionListData(val aspectDefinitions: List<AspectDefinitionData>)

    @Serializable
    data class ValidateAspectValueQueryData(val key: String, val values: List<String>)

    @Serializable
    data class ValidateAspectValueResponseData(val values: List<String>)

    @Serializable
    data class ListAliasesQueryData(val scopeId: String)

    @Serializable
    data class ListAliasesResponseData(val scopeId: String, val aliases: List<AliasData>)

    @Serializable
    data class AliasData(val name: String, val isCanonical: Boolean, val createdAt: String)

    @Serializable
    data class AliasInfo(val name: String, val isCanonical: Boolean)

    // Streaming Data Classes

    @Serializable
    data class SubscribeData(val message: String)

    @Serializable
    data class ConnectedEventData(val message: String, val timestamp: String)

    @Serializable
    data class ProgressUpdateEventData(
        val operationId: String,
        val percentage: Int,
        val message: String,
        val estimatedSecondsRemaining: Int,
        val metadata: Map<String, String> = emptyMap(),
    )

    @Serializable
    data class OperationCompletedEventData(val operationId: String, val message: String, val timestamp: String)

    @Serializable
    data class ErrorEventData(val error: String, val timestamp: String)

    // Stream Event Types

    sealed class StreamEvent {
        data class Connected(val message: String, val timestamp: String) : StreamEvent()

        data class ProgressUpdate(
            val operationId: String,
            val percentage: Int,
            val message: String,
            val estimatedSecondsRemaining: Int,
            val metadata: Map<String, String>,
        ) : StreamEvent()

        data class OperationCompleted(val operationId: String, val message: String, val timestamp: String) : StreamEvent()

        data class Error(val error: String, val timestamp: String) : StreamEvent()

        data class Unknown(val kind: String, val data: String) : StreamEvent()
    }
}
