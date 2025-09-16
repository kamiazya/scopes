package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import kotlinx.serialization.json.JsonElement

/**
 * Context for merge operations.
 */
data class MergeContext(val base: JsonElement, val changeSet1: ChangeSet, val changeSet2: ChangeSet)
