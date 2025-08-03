package com.kamiazya.scopes.domain.entity

import kotlinx.serialization.Serializable

/**
 * Status enumeration for Scope entities.
 */
@Serializable
enum class ScopeStatus {
    ACTIVE,
    COMPLETED,
    PAUSED,
    CANCELLED,
    ARCHIVED,
}
