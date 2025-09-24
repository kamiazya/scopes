package io.github.kamiazya.scopes.scopemanagement.application.dto.scope

import kotlinx.datetime.Instant

/**
 * Pure DTO for scope deletion result.
 * Contains only primitive types and standard library types.
 * No domain entities or value objects are exposed to maintain layer separation.
 */
data class DeleteScopeResult(val id: String, val deletedAt: Instant)
