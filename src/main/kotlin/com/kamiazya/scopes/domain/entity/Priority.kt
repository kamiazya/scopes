package com.kamiazya.scopes.domain.entity

import kotlinx.serialization.Serializable

/**
 * Priority levels for Scope entities.
 */
@Serializable
enum class Priority(
    val value: Int,
) {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4),
}
