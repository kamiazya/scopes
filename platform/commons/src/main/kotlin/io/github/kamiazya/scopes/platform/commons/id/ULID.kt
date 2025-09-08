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
        // Valid ULID characters according to Crockford's Base32 (excludes I, L, O, U)
        private const val VALID_CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

        fun fromString(value: String): ULID = ULID(value)

        fun isValid(value: String): Boolean = try {
            // ULID must be exactly 26 characters using Crockford's Base32
            run {
                val up = value.uppercase()
                up.length == 26 &&
                    up.all { it in VALID_CHARS } &&
                    // Maximum valid timestamp is "7ZZZZZZZZZ" (48-bit max in Base32)
                    up.substring(0, 10) <= "7ZZZZZZZZZ"
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
