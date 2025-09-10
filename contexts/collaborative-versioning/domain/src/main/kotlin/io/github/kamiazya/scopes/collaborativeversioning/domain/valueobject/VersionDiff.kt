package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

/**
 * Value object representing differences between versions.
 */
data class VersionDiff(val fromVersion: VersionNumber, val toVersion: VersionNumber, val changes: List<DiffChange>)
