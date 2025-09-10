package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import kotlinx.serialization.json.JsonElement

/**
 * Service for serializing and deserializing snapshots.
 *
 * This service handles the conversion of Snapshot entities to and from
 * their serialized JSON representation, ensuring data integrity and
 * providing comprehensive error handling.
 *
 * Note: The actual implementation should be in the infrastructure layer
 * to avoid domain layer dependencies on serialization libraries.
 */
interface SnapshotSerializer {
    /**
     * Serialize a snapshot to JSON.
     *
     * @param snapshot The snapshot to serialize
     * @return Either an error or the serialized JSON string
     */
    fun serialize(snapshot: Snapshot): Either<SnapshotServiceError, String>

    /**
     * Deserialize a snapshot from JSON.
     *
     * @param json The JSON string to deserialize
     * @return Either an error or the deserialized snapshot
     */
    fun deserialize(json: String): Either<SnapshotServiceError, Snapshot>

    /**
     * Serialize a snapshot to a JsonElement.
     *
     * @param snapshot The snapshot to serialize
     * @return Either an error or the serialized JsonElement
     */
    fun serializeToElement(snapshot: Snapshot): Either<SnapshotServiceError, JsonElement>

    /**
     * Deserialize a snapshot from a JsonElement.
     *
     * @param element The JsonElement to deserialize
     * @return Either an error or the deserialized snapshot
     */
    fun deserializeFromElement(element: JsonElement): Either<SnapshotServiceError, Snapshot>
}
