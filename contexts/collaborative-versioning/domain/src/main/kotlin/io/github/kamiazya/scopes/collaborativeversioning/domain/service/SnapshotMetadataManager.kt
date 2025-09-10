package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.agentmanagement.domain.valueobject.AgentId
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceType
import io.github.kamiazya.scopes.platform.observability.logging.ConsoleLogger
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlinx.datetime.Clock

/**
 * Service for managing snapshot metadata.
 *
 * This service provides functionality to enrich, validate, and query
 * snapshot metadata, supporting both system-generated and user-defined
 * metadata entries.
 */
interface SnapshotMetadataManager {
    /**
     * Enrich snapshot metadata with system-generated information.
     *
     * @param snapshot The snapshot to enrich
     * @param resourceType The type of resource being snapshotted
     * @param authorId The author creating the snapshot
     * @return The snapshot with enriched metadata
     */
    fun enrichMetadata(snapshot: Snapshot, resourceType: ResourceType, authorId: AgentId): Snapshot

    /**
     * Validate metadata entries.
     *
     * @param metadata The metadata to validate
     * @return Either an error or Unit on success
     */
    fun validateMetadata(metadata: Map<String, String>): Either<SnapshotServiceError, Unit>

    /**
     * Extract specific metadata categories.
     *
     * @param snapshot The snapshot to extract from
     * @return Categorized metadata
     */
    fun extractMetadataCategories(snapshot: Snapshot): SnapshotMetadataCategories

    /**
     * Merge metadata from multiple sources.
     *
     * @param base Base metadata
     * @param additional Additional metadata to merge
     * @param overwriteExisting Whether to overwrite existing keys
     * @return Merged metadata
     */
    fun mergeMetadata(base: Map<String, String>, additional: Map<String, String>, overwriteExisting: Boolean = false): Map<String, String>

    /**
     * Filter sensitive metadata.
     *
     * @param metadata The metadata to filter
     * @return Filtered metadata with sensitive values removed or redacted
     */
    fun filterSensitiveMetadata(metadata: Map<String, String>): Map<String, String>
}

/**
 * Categorized snapshot metadata.
 */
data class SnapshotMetadataCategories(
    val system: Map<String, String>,
    val user: Map<String, String>,
    val performance: Map<String, String>,
    val audit: Map<String, String>,
)

/**
 * Default implementation of SnapshotMetadataManager.
 */
class DefaultSnapshotMetadataManager(
    private val metadataValidator: SnapshotMetadataValidator = DefaultSnapshotMetadataValidator(),
    private val logger: Logger = ConsoleLogger("SnapshotMetadataManager"),
) : SnapshotMetadataManager {

    companion object {
        // System metadata keys
        private const val KEY_CREATED_AT = "system.created_at"
        private const val KEY_CREATED_BY_SERVICE = "system.created_by_service"
        private const val KEY_VERSION = "system.version"
        private const val KEY_RESOURCE_TYPE = "system.resource_type"
        private const val KEY_CONTENT_SIZE = "system.content_size_bytes"
        private const val KEY_CONTENT_HASH = "system.content_hash"
        private const val KEY_COMPRESSION = "system.compression"

        // Audit metadata keys
        private const val KEY_AUTHOR_ID = "audit.author_id"
        private const val KEY_AUTHOR_TYPE = "audit.author_type"
        private const val KEY_TIMESTAMP = "audit.timestamp"
        private const val KEY_CLIENT_VERSION = "audit.client_version"
        private const val KEY_PLATFORM = "audit.platform"

        // Performance metadata keys
        private const val KEY_CREATION_DURATION_MS = "perf.creation_duration_ms"
        private const val KEY_SERIALIZATION_DURATION_MS = "perf.serialization_duration_ms"
        private const val KEY_COMPRESSION_RATIO = "perf.compression_ratio"

        // Sensitive key patterns
        private val SENSITIVE_KEY_PATTERNS = listOf(
            Regex(".*password.*", RegexOption.IGNORE_CASE),
            Regex(".*secret.*", RegexOption.IGNORE_CASE),
            Regex(".*token.*", RegexOption.IGNORE_CASE),
            Regex(".*key.*", RegexOption.IGNORE_CASE),
            Regex(".*credential.*", RegexOption.IGNORE_CASE),
        )

        // Reserved key prefixes
        private val RESERVED_PREFIXES = setOf(
            "system.",
            "audit.",
            "perf.",
            "internal.",
        )
    }

    override fun enrichMetadata(snapshot: Snapshot, resourceType: ResourceType, authorId: AgentId): Snapshot {
        val enrichedMetadata = mutableMapOf<String, String>()
        enrichedMetadata.putAll(snapshot.metadata)

        // Add system metadata
        enrichedMetadata[KEY_CREATED_AT] = Clock.System.now().toString()
        enrichedMetadata[KEY_CREATED_BY_SERVICE] = "VersionSnapshotService"
        enrichedMetadata[KEY_VERSION] = snapshot.versionNumber.toString()
        enrichedMetadata[KEY_RESOURCE_TYPE] = resourceType.toString()
        enrichedMetadata[KEY_CONTENT_SIZE] = snapshot.content.sizeInBytes().toString()
        enrichedMetadata[KEY_CONTENT_HASH] = calculateContentHash(snapshot)
        enrichedMetadata[KEY_COMPRESSION] = "none" // Could be enhanced to support compression

        // Add audit metadata
        enrichedMetadata[KEY_AUTHOR_ID] = authorId.toString()
        enrichedMetadata[KEY_AUTHOR_TYPE] = determineAuthorType(authorId)
        enrichedMetadata[KEY_TIMESTAMP] = snapshot.createdAt.toString()
        enrichedMetadata[KEY_PLATFORM] = System.getProperty("os.name") ?: "unspecified"
        enrichedMetadata[KEY_CLIENT_VERSION] = "1.0.0" // Should be injected from configuration

        // Add performance metadata if available
        if (snapshot.hasMetadata("creation_start_time")) {
            val startTime = snapshot.getMetadata("creation_start_time")?.toLongOrNull()
            if (startTime != null) {
                val duration = Clock.System.now().toEpochMilliseconds() - startTime
                enrichedMetadata[KEY_CREATION_DURATION_MS] = duration.toString()
            }
        }

        logger.debug(
            "Enriched snapshot metadata",
            mapOf(
                "snapshotId" to snapshot.id.toString(),
                "originalMetadataSize" to snapshot.metadata.size,
                "enrichedMetadataSize" to enrichedMetadata.size,
            ),
        )

        return snapshot.copy(metadata = enrichedMetadata)
    }

    override fun validateMetadata(metadata: Map<String, String>): Either<SnapshotServiceError, Unit> = either {
        // Check for reserved prefixes in user-provided metadata
        metadata.keys.forEach { key ->
            ensure(!isReservedKey(key)) {
                SnapshotServiceError.MetadataValidationError(
                    key = key,
                    value = metadata[key] ?: "",
                    reason = "Key uses reserved prefix. Reserved prefixes: ${RESERVED_PREFIXES.joinToString()}",
                )
            }
        }

        // Validate each entry
        metadata.forEach { (key, value) ->
            metadataValidator.validate(key, value).bind()
        }
    }

    override fun extractMetadataCategories(snapshot: Snapshot): SnapshotMetadataCategories {
        val system = mutableMapOf<String, String>()
        val user = mutableMapOf<String, String>()
        val performance = mutableMapOf<String, String>()
        val audit = mutableMapOf<String, String>()

        snapshot.metadata.forEach { (key, value) ->
            when {
                key.startsWith("system.") -> system[key] = value
                key.startsWith("perf.") -> performance[key] = value
                key.startsWith("audit.") -> audit[key] = value
                !isReservedKey(key) -> user[key] = value
            }
        }

        return SnapshotMetadataCategories(
            system = system,
            user = user,
            performance = performance,
            audit = audit,
        )
    }

    override fun mergeMetadata(base: Map<String, String>, additional: Map<String, String>, overwriteExisting: Boolean): Map<String, String> {
        val merged = mutableMapOf<String, String>()
        merged.putAll(base)

        additional.forEach { (key, value) ->
            if (overwriteExisting || key !in merged) {
                merged[key] = value
            }
        }

        logger.debug(
            "Merged metadata",
            mapOf(
                "baseSize" to base.size,
                "additionalSize" to additional.size,
                "mergedSize" to merged.size,
                "overwriteExisting" to overwriteExisting,
            ),
        )

        return merged
    }

    override fun filterSensitiveMetadata(metadata: Map<String, String>): Map<String, String> {
        val filtered = mutableMapOf<String, String>()
        var redactedCount = 0

        metadata.forEach { (key, value) ->
            if (isSensitiveKey(key)) {
                filtered[key] = "[REDACTED]"
                redactedCount++
            } else {
                filtered[key] = value
            }
        }

        if (redactedCount > 0) {
            logger.info(
                "Filtered sensitive metadata",
                mapOf(
                    "totalKeys" to metadata.size,
                    "redactedKeys" to redactedCount,
                ),
            )
        }

        return filtered
    }

    private fun calculateContentHash(snapshot: Snapshot): String {
        // Simple hash calculation - could be enhanced with proper hashing
        val contentString = snapshot.content.toJsonString()
        return contentString.hashCode().toString(16)
    }

    private fun determineAuthorType(authorId: AgentId): String {
        // Simple determination - could be enhanced with actual agent type lookup
        return when {
            authorId.toString().startsWith("user_") -> "user"
            authorId.toString().startsWith("agent_") -> "ai_agent"
            authorId.toString().startsWith("system_") -> "system"
            else -> "other"
        }
    }

    private fun isReservedKey(key: String): Boolean = RESERVED_PREFIXES.any { prefix -> key.startsWith(prefix) }

    private fun isSensitiveKey(key: String): Boolean = SENSITIVE_KEY_PATTERNS.any { pattern -> pattern.matches(key) }
}

/**
 * Builder for snapshot metadata.
 */
class SnapshotMetadataBuilder {
    private val metadata = mutableMapOf<String, String>()

    fun addSystemMetadata(key: String, value: String): SnapshotMetadataBuilder {
        metadata["system.$key"] = value
        return this
    }

    fun addUserMetadata(key: String, value: String): SnapshotMetadataBuilder {
        metadata[key] = value
        return this
    }

    fun addAuditMetadata(key: String, value: String): SnapshotMetadataBuilder {
        metadata["audit.$key"] = value
        return this
    }

    fun addPerformanceMetadata(key: String, value: String): SnapshotMetadataBuilder {
        metadata["perf.$key"] = value
        return this
    }

    fun addAll(entries: Map<String, String>): SnapshotMetadataBuilder {
        metadata.putAll(entries)
        return this
    }

    fun build(): Map<String, String> = metadata.toMap()
}
