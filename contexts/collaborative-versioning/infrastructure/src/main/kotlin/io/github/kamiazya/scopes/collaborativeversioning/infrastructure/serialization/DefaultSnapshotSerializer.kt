package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.serialization

import arrow.core.Either
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SnapshotSerializer
import io.github.kamiazya.scopes.collaborativeversioning.infrastructure.dto.SnapshotSerializationDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Default implementation of SnapshotSerializer using kotlinx.serialization.
 * This is an infrastructure component that handles the technical aspects of serialization.
 */
class DefaultSnapshotSerializer : SnapshotSerializer {

    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = false
    }

    override fun serialize(snapshot: Snapshot): Either<SnapshotServiceError, String> = Either.catch {
        val dto = SnapshotSerializationDto.fromDomain(snapshot)
        json.encodeToString(dto)
    }.fold(
        { throwable ->
            Either.Left(
                SnapshotServiceError.SerializationError(
                    reason = "Failed to serialize snapshot: ${throwable.message}",
                    cause = throwable,
                ),
            )
        },
        { Either.Right(it) },
    )

    override fun deserialize(json: String): Either<SnapshotServiceError, Snapshot> = Either.catch {
        val dto = this.json.decodeFromString<SnapshotSerializationDto>(json)
        dto.toDomain()
    }.fold(
        { throwable ->
            Either.Left(
                SnapshotServiceError.DeserializationError(
                    reason = "Failed to deserialize snapshot: ${throwable.message}",
                    cause = throwable,
                ),
            )
        },
        { result -> result },
    )

    override fun serializeToElement(snapshot: Snapshot): Either<SnapshotServiceError, JsonElement> = Either.catch {
        val dto = SnapshotSerializationDto.fromDomain(snapshot)
        json.encodeToJsonElement(dto)
    }.fold(
        { throwable ->
            Either.Left(
                SnapshotServiceError.SerializationError(
                    reason = "Failed to serialize snapshot to element: ${throwable.message}",
                    cause = throwable,
                ),
            )
        },
        { Either.Right(it) },
    )

    override fun deserializeFromElement(element: JsonElement): Either<SnapshotServiceError, Snapshot> = Either.catch {
        val dto = json.decodeFromJsonElement<SnapshotSerializationDto>(element)
        dto.toDomain()
    }.fold(
        { throwable ->
            Either.Left(
                SnapshotServiceError.DeserializationError(
                    reason = "Failed to deserialize snapshot from element: ${throwable.message}",
                    cause = throwable,
                ),
            )
        },
        { result -> result },
    )
}
