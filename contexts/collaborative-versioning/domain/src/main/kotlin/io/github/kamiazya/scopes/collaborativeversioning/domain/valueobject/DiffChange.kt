package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import kotlinx.serialization.json.JsonElement

/**
 * Types of changes in a diff.
 */
sealed class DiffChange {
    data class Added(val path: String, val value: JsonElement) : DiffChange()
    data class Modified(val path: String, val oldValue: JsonElement, val newValue: JsonElement) : DiffChange()
    data class Deleted(val path: String, val value: JsonElement) : DiffChange()
}
