package com.kamiazya.scopes.domain.entity

import com.github.guepardoapps.kulid.ULID
import kotlinx.serialization.Serializable

/**
 * Type-safe identifier for Scope entities using ULID for lexicographically sortable distributed system compatibility.
 */
@Serializable
@JvmInline
value class ScopeId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ScopeId cannot be blank" }
        require(ULID.isValid(value)) { "ScopeId must be a valid ULID format" }
    }

    companion object {
        fun generate(): ScopeId = ScopeId(ULID.random())

        fun from(value: String): ScopeId = ScopeId(value)
    }

    override fun toString(): String = value
}
