package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ChangesetError
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.CollaborativeVersioningError
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement

/**
 * Represents a set of changes between versions.
 * This is a structured representation of a JsonDiff that can be persisted and applied.
 */
data class ChangeSet private constructor(
    val id: ChangesetId,
    val diff: JsonDiff,
    val sourceHash: String,
    val targetHash: String,
    val createdAt: Instant,
    val metadata: Map<String, String>,
) {
    /**
     * Check if this changeset is empty (no changes).
     */
    fun isEmpty(): Boolean = diff.changes.isEmpty()

    /**
     * Get the number of changes in this changeset.
     */
    fun changeCount(): Int = diff.changeCount()

    /**
     * Check if this changeset can be applied to a document with the given hash.
     */
    fun canApplyTo(documentHash: String): Boolean = documentHash == sourceHash

    /**
     * Apply this changeset to a JSON document.
     */
    fun apply(document: JsonElement): Either<ChangesetError, JsonElement> = either {
        val actualHash = computeHash(document)

        ensure(canApplyTo(actualHash)) {
            ChangesetError.InvalidChangeset(
                changesetId = id,
                reason = "Document hash mismatch. Expected: $sourceHash, Actual: $actualHash",
            )
        }

        diff.apply(document).mapLeft { error ->
            ChangesetError.InvalidChangeset(
                changesetId = id,
                reason = "Failed to apply diff: $error",
            )
        }.bind()
    }

    /**
     * Create an inverse changeset that reverses this changeset.
     */
    fun inverse(): Either<CollaborativeVersioningError, ChangeSet> = either {
        val inverseDiff = JsonDiff(
            changes = diff.changes.map { change ->
                when (change) {
                    is JsonChange.Add -> JsonChange.Remove(
                        path = change.path,
                        oldValue = change.value,
                    )
                    is JsonChange.Remove -> JsonChange.Add(
                        path = change.path,
                        value = change.oldValue,
                    )
                    is JsonChange.Replace -> JsonChange.Replace(
                        path = change.path,
                        oldValue = change.newValue,
                        newValue = change.oldValue,
                    )
                    is JsonChange.Move -> JsonChange.Move(
                        from = change.to,
                        to = change.from,
                        value = change.value,
                    )
                }
            }.reversed(),
            structuralChanges = diff.structuralChanges.map { change ->
                when (change) {
                    is StructuralChange.FieldAdded -> StructuralChange.FieldRemoved(change.path)
                    is StructuralChange.FieldRemoved -> StructuralChange.FieldAdded(change.path)
                    is StructuralChange.TypeChanged -> StructuralChange.TypeChanged(
                        path = change.path,
                        fromType = change.toType,
                        toType = change.fromType,
                    )
                    is StructuralChange.ArraySizeChanged -> StructuralChange.ArraySizeChanged(
                        path = change.path,
                        fromSize = change.toSize,
                        toSize = change.fromSize,
                    )
                }
            },
            metadata = metadata + mapOf(
                "inverseOf" to id.toString(),
                "inversedAt" to SystemTimeProvider().now().toString(),
            ),
        )

        ChangeSet(
            id = ChangesetId.generate(),
            diff = inverseDiff,
            sourceHash = targetHash,
            targetHash = sourceHash,
            createdAt = SystemTimeProvider().now(),
            metadata = metadata + mapOf(
                "inverseOf" to id.toString(),
            ),
        )
    }

    companion object {
        /**
         * Create a new changeset from a diff and source/target documents.
         */
        fun create(
            diff: JsonDiff,
            source: JsonElement,
            target: JsonElement,
            metadata: Map<String, String> = emptyMap(),
        ): Either<CollaborativeVersioningError, ChangeSet> = either {
            val sourceHash = computeHash(source)
            val targetHash = computeHash(target)

            ChangeSet(
                id = ChangesetId.generate(),
                diff = diff,
                sourceHash = sourceHash,
                targetHash = targetHash,
                createdAt = SystemTimeProvider().now(),
                metadata = metadata,
            )
        }

        /**
         * Create an empty changeset (no changes).
         */
        fun empty(sourceHash: String, metadata: Map<String, String> = emptyMap()): ChangeSet = ChangeSet(
            id = ChangesetId.generate(),
            diff = JsonDiff.empty(),
            sourceHash = sourceHash,
            targetHash = sourceHash,
            createdAt = SystemTimeProvider().now(),
            metadata = metadata,
        )

        /**
         * Compute a hash for a JSON document.
         */
        private fun computeHash(document: JsonElement): String {
            // Simple hash implementation - in production would use proper crypto hash
            return document.toString().hashCode().toString(16)
        }
    }
}

/**
 * Value object for DiffPath used in change tracking.
 */
data class DiffPath(val value: String) {
    init {
        require(value.isNotBlank()) { "DiffPath cannot be blank" }
    }

    override fun toString(): String = value

    companion object {
        fun root() = DiffPath("$")

        fun fromJsonPath(path: JsonPath): DiffPath = DiffPath(path.toString())
    }
}
