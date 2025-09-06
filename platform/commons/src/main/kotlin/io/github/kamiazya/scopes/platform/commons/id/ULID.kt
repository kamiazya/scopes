package io.github.kamiazya.scopes.platform.commons.id

import com.github.guepardoapps.kulid.ULID as KULID

/**
 * Abstraction for ULID generators to support testability and dependency inversion.
 * Domain layers should depend on this interface rather than concrete ULID implementations.
 */
interface ULIDGenerator {
    fun generate(): ULID
}

@JvmInline
value class ULID(val value: String) {
    init {
        require(isValid(value)) { "Invalid ULID format: $value" }
    }

    companion object {
        fun fromString(value: String): ULID = ULID(value)

        fun isValid(value: String): Boolean = try {
            // KULID doesn't have a validation method, so we check the format
            value.length == 26 && value.all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' }
        } catch (e: Exception) {
            false
        }

        // Keep concrete implementation for backward compatibility temporarily
        @Deprecated("Use ULIDGenerator interface instead for better testability", ReplaceWith("ULIDGenerator.generate()"))
        fun generate(): ULID = ULID(KULID.random())
    }

    override fun toString(): String = value
}
