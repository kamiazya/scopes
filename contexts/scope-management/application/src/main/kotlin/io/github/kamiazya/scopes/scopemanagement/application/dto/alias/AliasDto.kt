package io.github.kamiazya.scopes.scopemanagement.application.dto.alias
import kotlinx.datetime.Instant

/**
 * Data Transfer Object for Scope aliases.
 *
 * Used to transfer alias data between application and external layers.
 */
data class AliasDto(val alias: String, val scopeId: String, val isCanonical: Boolean, val createdAt: Instant)
