package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import kotlinx.datetime.Instant

/**
 * Represents a detected conflict.
 */
data class Conflict(
    val id: ConflictId,
    val type: ConflictType,
    val path: JsonPath,
    val change1: JsonChange,
    val change2: JsonChange,
    val description: String,
    val detectedAt: Instant,
    val severity: ConflictSeverity,
    val resolution: ConflictResolution? = null,
)
