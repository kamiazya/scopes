package io.github.kamiazya.scopes.domain.valueobject

import com.github.guepardoapps.kulid.ULID

/**
 * Value object representing a unique identifier for a context view.
 * Uses ULID for lexicographically sortable distributed system compatibility.
 */
@JvmInline
value class ContextViewId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "ContextViewId cannot be blank" }
        require(ULID.isValid(value)) { "ContextViewId must be a valid ULID format" }
    }

    companion object {
        /**
         * Generate a new unique ContextViewId.
         */
        fun generate(): ContextViewId = ContextViewId(ULID.random())

        /**
         * Create a ContextViewId from an existing string value.
         * Validates that the string is a valid ULID format.
         */
        fun from(value: String): ContextViewId = ContextViewId(value)
    }

    override fun toString(): String = value
}
