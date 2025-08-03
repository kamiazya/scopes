package com.kamiazya.scopes.domain.entity

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Type-safe identifier for Scope entities using UUID for distributed system compatibility.
 */
@Serializable
@JvmInline
value class ScopeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ScopeId cannot be blank" }
    }

    companion object {
        fun generate(): ScopeId = ScopeId(UUID.randomUUID().toString())

        fun from(value: String): ScopeId = ScopeId(value)
    }

    override fun toString(): String = value
}
