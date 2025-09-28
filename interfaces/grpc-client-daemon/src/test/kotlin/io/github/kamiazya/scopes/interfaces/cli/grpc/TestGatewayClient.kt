package io.github.kamiazya.scopes.interfaces.cli.grpc

import arrow.core.Either
import io.github.kamiazya.scopes.contracts.scopemanagement.results.CreateScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeListResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.ScopeResult
import io.github.kamiazya.scopes.contracts.scopemanagement.results.UpdateScopeResult
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.rpc.v1beta.Envelope
import io.github.kamiazya.scopes.rpc.v1beta.TaskGatewayServiceGrpcKt
import io.grpc.ManagedChannel
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Test wrapper for GatewayClient that uses composition instead of inheritance.
 * This avoids the final class inheritance issue by wrapping the client and
 * directly using the gRPC stub for testing.
 */
class TestGatewayClient(private val logger: Logger, private val json: Json, private val testChannel: ManagedChannel) {
    private val stub = TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineStub(testChannel)

    suspend fun createScope(
        title: String,
        description: String?,
        parentId: String?,
        customAlias: String?,
    ): Either<GatewayClient.ClientError, CreateScopeResult> {
        val payload = json.encodeToString(
            GatewayClient.CreateScopeCommandData.serializer(),
            GatewayClient.CreateScopeCommandData(title, description, parentId, customAlias),
        )
        val req = Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind("scopes.commands.CreateScope")
            .setVersion("v1")
            .setCorrelationId(UUID.randomUUID().toString())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = stub.executeCommand(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(GatewayClient.CreateScopeResponseData.serializer(), jsonText)
            val created = kotlinx.datetime.Instant.parse(r.createdAt)
            CreateScopeResult(
                id = r.id,
                title = r.title,
                description = r.description,
                parentId = r.parentId,
                canonicalAlias = r.canonicalAlias,
                createdAt = created,
                updatedAt = created,
            ).let { Either.Right(it) }
        } catch (e: io.grpc.StatusException) {
            Either.Left(GatewayClient.ClientError.ServiceError(e.status.code, e.status.description ?: "", e))
        } catch (e: Exception) {
            Either.Left(GatewayClient.ClientError.Unexpected(e.message ?: "unexpected"))
        }
    }

    suspend fun getScope(id: String): Either<GatewayClient.ClientError, ScopeResult?> {
        val payload = json.encodeToString(
            GatewayClient.GetScopeQueryData.serializer(),
            GatewayClient.GetScopeQueryData(id),
        )
        val req = Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind("scopes.queries.GetScope")
            .setVersion("v1")
            .setCorrelationId(UUID.randomUUID().toString())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = stub.query(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(GatewayClient.GetScopeResponseData.serializer(), jsonText)
            val created = kotlinx.datetime.Instant.parse(r.createdAt)
            val updated = kotlinx.datetime.Instant.parse(r.updatedAt)
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
            ).let { Either.Right(it) }
        } catch (e: io.grpc.StatusException) {
            if (e.status.code == io.grpc.Status.Code.NOT_FOUND) {
                Either.Right(null)
            } else {
                Either.Left(GatewayClient.ClientError.ServiceError(e.status.code, e.status.description ?: "", e))
            }
        } catch (e: Exception) {
            Either.Left(GatewayClient.ClientError.Unexpected(e.message ?: "unexpected"))
        }
    }

    suspend fun getRootScopes(offset: Int = 0, limit: Int = 50): Either<GatewayClient.ClientError, ScopeListResult> {
        val payload = json.encodeToString(
            GatewayClient.GetRootScopesQueryData.serializer(),
            GatewayClient.GetRootScopesQueryData(offset, limit),
        )
        val req = Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind("scopes.queries.GetRootScopes")
            .setVersion("v1")
            .setCorrelationId(UUID.randomUUID().toString())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = stub.query(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(GatewayClient.GetRootScopesResponseData.serializer(), jsonText)
            val scopes = r.scopes.map { scope ->
                val created = kotlinx.datetime.Instant.parse(scope.createdAt)
                val updated = kotlinx.datetime.Instant.parse(scope.updatedAt)
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
            ).let { Either.Right(it) }
        } catch (e: io.grpc.StatusException) {
            Either.Left(GatewayClient.ClientError.ServiceError(e.status.code, e.status.description ?: "", e))
        } catch (e: Exception) {
            Either.Left(GatewayClient.ClientError.Unexpected(e.message ?: "unexpected"))
        }
    }

    suspend fun updateScope(id: String, title: String?, description: String?, parentId: String?): Either<GatewayClient.ClientError, UpdateScopeResult> {
        val payload = json.encodeToString(
            GatewayClient.UpdateScopeCommandData.serializer(),
            GatewayClient.UpdateScopeCommandData(id, title, description, parentId),
        )
        val req = Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind("scopes.commands.UpdateScope")
            .setVersion("v1")
            .setCorrelationId(UUID.randomUUID().toString())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            val res = stub.executeCommand(req)
            val jsonText = res.payload.toStringUtf8()
            val r = json.decodeFromString(GatewayClient.UpdateScopeResponseData.serializer(), jsonText)
            val created = kotlinx.datetime.Instant.parse(r.createdAt)
            val updated = kotlinx.datetime.Instant.parse(r.updatedAt)
            UpdateScopeResult(
                id = r.id,
                title = r.title,
                description = r.description,
                parentId = r.parentId,
                canonicalAlias = r.canonicalAlias,
                createdAt = created,
                updatedAt = updated,
            ).let { Either.Right(it) }
        } catch (e: io.grpc.StatusException) {
            Either.Left(GatewayClient.ClientError.ServiceError(e.status.code, e.status.description ?: "", e))
        } catch (e: Exception) {
            Either.Left(GatewayClient.ClientError.Unexpected(e.message ?: "unexpected"))
        }
    }

    suspend fun addAlias(scopeId: String, aliasName: String): Either<GatewayClient.ClientError, Unit> {
        val payload = json.encodeToString(
            GatewayClient.AddAliasCommandData.serializer(),
            GatewayClient.AddAliasCommandData(scopeId, aliasName),
        )
        val req = Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind("scopes.commands.AddAlias")
            .setVersion("v1")
            .setCorrelationId(UUID.randomUUID().toString())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            stub.executeCommand(req)
            Either.Right(Unit)
        } catch (e: io.grpc.StatusException) {
            Either.Left(GatewayClient.ClientError.ServiceError(e.status.code, e.status.description ?: "", e))
        } catch (e: Exception) {
            Either.Left(GatewayClient.ClientError.Unexpected(e.message ?: "unexpected"))
        }
    }

    suspend fun removeAlias(scopeId: String, aliasName: String): Either<GatewayClient.ClientError, Unit> {
        val payload = json.encodeToString(
            GatewayClient.RemoveAliasCommandData.serializer(),
            GatewayClient.RemoveAliasCommandData(scopeId, aliasName),
        )
        val req = Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind("scopes.commands.RemoveAlias")
            .setVersion("v1")
            .setCorrelationId(UUID.randomUUID().toString())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            stub.executeCommand(req)
            Either.Right(Unit)
        } catch (e: io.grpc.StatusException) {
            Either.Left(GatewayClient.ClientError.ServiceError(e.status.code, e.status.description ?: "", e))
        } catch (e: Exception) {
            Either.Left(GatewayClient.ClientError.Unexpected(e.message ?: "unexpected"))
        }
    }

    suspend fun setCanonicalAlias(scopeId: String, aliasName: String): Either<GatewayClient.ClientError, Unit> {
        val payload = json.encodeToString(
            GatewayClient.SetCanonicalAliasCommandData.serializer(),
            GatewayClient.SetCanonicalAliasCommandData(scopeId, aliasName),
        )
        val req = Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind("scopes.commands.SetCanonicalAlias")
            .setVersion("v1")
            .setCorrelationId(UUID.randomUUID().toString())
            .setPayload(com.google.protobuf.ByteString.copyFromUtf8(payload))
            .build()

        return try {
            stub.executeCommand(req)
            Either.Right(Unit)
        } catch (e: io.grpc.StatusException) {
            Either.Left(GatewayClient.ClientError.ServiceError(e.status.code, e.status.description ?: "", e))
        } catch (e: Exception) {
            Either.Left(GatewayClient.ClientError.Unexpected(e.message ?: "unexpected"))
        }
    }
}
