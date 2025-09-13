package io.github.kamiazya.scopes.collaborativeversioning.infrastructure.dto

import arrow.core.Either
import arrow.core.getOrElse
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.*
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Data transfer object for Snapshot serialization.
 * This is an infrastructure concern and should not be in the domain layer.
 */
@Serializable
internal data class SnapshotSerializationDto(
    val id: String,
    val resourceId: String,
    val versionId: String,
    val versionNumber: Int,
    val content: JsonElement,
    val authorId: String,
    val message: String,
    val createdAt: Long,
    val metadata: Map<String, String>,
) {
    companion object {
        fun fromDomain(snapshot: Snapshot): SnapshotSerializationDto = SnapshotSerializationDto(
            id = snapshot.id.toString(),
            resourceId = snapshot.resourceId.toString(),
            versionId = snapshot.versionId.toString(),
            versionNumber = snapshot.versionNumber.value,
            content = snapshot.content.value,
            authorId = snapshot.authorId.toString(),
            message = snapshot.message,
            createdAt = snapshot.createdAt.epochSeconds,
            metadata = snapshot.metadata,
        )
    }

    fun toDomain(): Either<SnapshotServiceError, Snapshot> = Either.catch {
        Snapshot(
            id = SnapshotId.from(id).getOrElse {
                error("Invalid snapshot ID: $id")
            },
            resourceId = ResourceId.from(resourceId).getOrElse {
                error("Invalid resource ID: $resourceId")
            },
            versionId = VersionId.from(versionId).getOrElse {
                error("Invalid version ID: $versionId")
            },
            versionNumber = VersionNumber.from(versionNumber).getOrElse {
                error("Invalid version number: $versionNumber")
            },
            content = ResourceContent.fromJsonElement(content),
            authorId = AgentId.from(authorId).getOrElse {
                error("Invalid agent ID: $authorId")
            },
            message = message,
            createdAt = Instant.fromEpochSeconds(createdAt),
            metadata = metadata,
        )
    }.mapLeft { throwable ->
        SnapshotServiceError.DeserializationError(
            reason = "Failed to convert DTO to domain: ${throwable.message}",
            cause = throwable,
        )
    }
}
