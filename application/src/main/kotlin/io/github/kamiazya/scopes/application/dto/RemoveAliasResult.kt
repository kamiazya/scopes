package io.github.kamiazya.scopes.application.dto

/**
 * Result DTO for alias removal.
 * 
 * Indicates whether the alias was successfully removed.
 */
data class RemoveAliasResult(
    val removed: Boolean
) : DTO