package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import kotlinx.serialization.json.JsonElement

/**
 * Represents a single diff operation.
 */
sealed class DiffOperation {
    data class Add(val path: String, val value: JsonElement) : DiffOperation()
    data class Remove(val path: String, val oldValue: JsonElement) : DiffOperation()
    data class Replace(val path: String, val oldValue: JsonElement, val newValue: JsonElement) : DiffOperation()
    data class Move(val fromPath: String, val toPath: String, val value: JsonElement) : DiffOperation()
}
