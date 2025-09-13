package io.github.kamiazya.scopes.collaborativeversioning.domain.service

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.entity.Snapshot
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.TrackedResourceServiceError
import io.github.kamiazya.scopes.collaborativeversioning.domain.model.TrackedResource
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.DiffChange
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceContent
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceType
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionDiff
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.VersionNumber
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/**
 * Domain service for TrackedResource operations.
 *
 * This service provides business logic that doesn't naturally belong to any
 * single entity, particularly operations that involve complex validation,
 * merging, or comparison of tracked resources.
 */
object TrackedResourceService {

    /**
     * Merge two versions of a resource into a new version.
     *
     * This performs a simple JSON merge, preferring values from the source version
     * when conflicts occur. More sophisticated merge strategies can be implemented
     * based on ResourceType.
     */
    fun mergeVersions(baseSnapshot: Snapshot, sourceSnapshot: Snapshot, targetSnapshot: Snapshot): Either<TrackedResourceServiceError, ResourceContent> =
        either {
            ensure(baseSnapshot.resourceId == sourceSnapshot.resourceId) {
                TrackedResourceServiceError.ResourceMismatch(
                    expected = baseSnapshot.resourceId,
                    actual = sourceSnapshot.resourceId,
                )
            }

            ensure(baseSnapshot.resourceId == targetSnapshot.resourceId) {
                TrackedResourceServiceError.ResourceMismatch(
                    expected = baseSnapshot.resourceId,
                    actual = targetSnapshot.resourceId,
                )
            }

            val baseJson = baseSnapshot.content.value.jsonObject
            val sourceJson = sourceSnapshot.content.value.jsonObject
            val targetJson = targetSnapshot.content.value.jsonObject

            val mergedJson = performJsonMerge(baseJson, sourceJson, targetJson)
            ResourceContent.fromJsonElement(mergedJson)
        }

    /**
     * Calculate the difference between two snapshots.
     *
     * Returns a structured representation of what changed between versions.
     */
    fun calculateDiff(fromSnapshot: Snapshot, toSnapshot: Snapshot): Either<TrackedResourceServiceError, VersionDiff> = either {
        ensure(fromSnapshot.resourceId == toSnapshot.resourceId) {
            TrackedResourceServiceError.ResourceMismatch(
                expected = fromSnapshot.resourceId,
                actual = toSnapshot.resourceId,
            )
        }

        val fromJson = fromSnapshot.content.value.jsonObject
        val toJson = toSnapshot.content.value.jsonObject

        val changes = calculateJsonDiff(fromJson, toJson)

        VersionDiff(
            fromVersion = fromSnapshot.versionNumber,
            toVersion = toSnapshot.versionNumber,
            changes = changes,
        )
    }

    /**
     * Validate that a resource content is valid for its type.
     */
    fun validateContentForType(content: ResourceContent, resourceType: ResourceType): Either<TrackedResourceServiceError, Unit> = either {
        val json = content.value.jsonObject

        when (resourceType) {
            ResourceType.SCOPE -> validateScopeContent(json)
            ResourceType.ASPECT -> validateAspectContent(json)
            ResourceType.ALIAS -> validateAliasContent(json)
        }.bind()
    }

    /**
     * Check if two tracked resources can be merged.
     */
    fun canMerge(source: TrackedResource, target: TrackedResource): Boolean = source.id == target.id &&
        source.resourceType == target.resourceType

    /**
     * Find the common ancestor version between two version histories.
     */
    fun findCommonAncestor(sourceHistory: List<VersionNumber>, targetHistory: List<VersionNumber>): VersionNumber? {
        val sourceSet = sourceHistory.toSet()
        return targetHistory.find { it in sourceSet }
    }

    private fun performJsonMerge(base: JsonObject, source: JsonObject, target: JsonObject): JsonObject {
        val merged = mutableMapOf<String, JsonElement>()

        // Start with all keys from all three objects
        val allKeys = base.keys + source.keys + target.keys

        for (key in allKeys) {
            val baseValue = base[key]
            val sourceValue = source[key]
            val targetValue = target[key]

            // Skip if deleted in both
            if (sourceValue == null && targetValue == null) continue

            val value: JsonElement? = when {
                // If only in source, take source
                baseValue == null && targetValue == null -> sourceValue
                // If only in target, take target
                baseValue == null && sourceValue == null -> targetValue
                // If deleted in source but modified in target, take target
                sourceValue == null && targetValue != baseValue -> targetValue
                // If deleted in target but modified in source, take source
                targetValue == null && sourceValue != baseValue -> sourceValue
                // If both have same value, take it
                sourceValue == targetValue -> sourceValue
                // If source modified but target didn't, take source
                sourceValue != baseValue && targetValue == baseValue -> sourceValue
                // If target modified but source didn't, take target
                targetValue != baseValue && sourceValue == baseValue -> targetValue
                // Both modified differently - prefer source (could be made configurable)
                else -> sourceValue
            }

            // Only add to merged if value is not null
            value?.let { merged[key] = it }
        }

        return JsonObject(merged)
    }

    private fun calculateJsonDiff(from: JsonObject, to: JsonObject): List<DiffChange> {
        val changes = mutableListOf<DiffChange>()

        // Check for additions and modifications
        for ((key, toValue) in to) {
            val fromValue = from[key]
            when {
                fromValue == null -> changes.add(DiffChange.Added(key, toValue))
                fromValue != toValue -> changes.add(DiffChange.Modified(key, fromValue, toValue))
            }
        }

        // Check for deletions
        for ((key, fromValue) in from) {
            if (key !in to) {
                changes.add(DiffChange.Deleted(key, fromValue))
            }
        }

        return changes
    }

    private fun validateScopeContent(json: JsonObject): Either<TrackedResourceServiceError, Unit> = either {
        ensure("title" in json) {
            TrackedResourceServiceError.InvalidContent("Scope must have a title")
        }
    }

    private fun validateAspectContent(json: JsonObject): Either<TrackedResourceServiceError, Unit> = either {
        ensure("name" in json) {
            TrackedResourceServiceError.InvalidContent("Aspect must have a name")
        }
        ensure("type" in json) {
            TrackedResourceServiceError.InvalidContent("Aspect must have a type")
        }
    }

    private fun validateAliasContent(json: JsonObject): Either<TrackedResourceServiceError, Unit> = either {
        ensure("alias" in json) {
            TrackedResourceServiceError.InvalidContent("Alias must have an alias field")
        }
        ensure("targetId" in json) {
            TrackedResourceServiceError.InvalidContent("Alias must have a targetId")
        }
    }
}
