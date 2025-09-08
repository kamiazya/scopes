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
            // ULID must be exactly 26 characters using Crockford's Base32
            // Valid characters: 0-9 and A-Z excluding I, L, O, U (to avoid confusion)
            val validChars = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

            value.length == 26 &&
                value.uppercase().all { it in validChars } &&
                // Additional validation: timestamp (first 10 chars) should not overflow
                value.take(10).uppercase().let { timestamp ->
                    // Maximum valid timestamp is "7ZZZZZZZZZ" (max 48-bit value in base32)
                    timestamp <= "7ZZZZZZZZZ"
                }
        } catch (e: Exception) {
            false
        }

        // Keep concrete implementation for backward compatibility temporarily
        @Deprecated("Use ULIDGenerator interface instead for better testability", ReplaceWith("ULIDGenerator.generate()"))
        fun generate(): ULID = ULID(KULID.random())
    }

    override fun toString(): String = value
}
