package io.github.kamiazya.scopes.platform.commons.id

import com.github.guepardoapps.kulid.ULID as KULID

/**
 * Abstraction for ULID generators to support testability and dependency inversion.
 * Domain layers should depend on this interface rather than concrete ULID implementations.
 */
fun interface ULIDGenerator {
    fun generate(): ULID
}

@JvmInline
value class ULID private constructor(val value: String) {
    companion object {
        // Valid ULID characters according to Crockford's Base32 (excludes I, L, O, U)
        private const val VALID_CHARS = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

        /**
         * Creates a ULID from string, normalizing to uppercase.
         * This is the only public way to create a ULID instance.
         */
        fun fromString(value: String): ULID {
            val normalized = value.uppercase()
            require(isValid(normalized)) { "Invalid ULID format: $value" }
            return ULID(normalized)
        }

        /**
         * Validates whether a string is a well-formed ULID.
         *
         * Performs case-insensitive validation against Crockford Base32:
         * - exactly 26 characters,
         * - all characters are in the allowed alphabet,
         * - the 10-character timestamp prefix is <= "7ZZZZZZZZZ" (enforces the 48-bit timestamp max).
         *
         * @param value the candidate ULID (case-insensitive; will be normalized to uppercase)
         * @return true if the input is a valid ULID, false otherwise
         */
        fun isValid(value: String): Boolean = try {
            // ULID must be exactly 26 characters using Crockford's Base32
            run {
                val up = value.uppercase()
                up.length == 26 &&
                    up.all { it in VALID_CHARS } &&
                    // Maximum valid timestamp is "7ZZZZZZZZZ" (48-bit max in Base32)
                    up.take(10) <= "7ZZZZZZZZZ"
            }
        } catch (e: Exception) {
            false
        }

        // Temporary bridge method to SystemULIDGenerator
        /**
         * Generates a new ULID using the system ULID provider and returns it as a validated ULID instance.
         *
         * This is a temporary bridge to the global/system ULID generator. Prefer injecting an implementation of
         * ULIDGenerator and calling its `generate()` method for better testability; this bridge will be removed once
         * dependency injection is adopted by all consumers.
         *
         * @return A ULID created from a randomly generated ULID string.
         */
        @Deprecated(
            "Use ULIDGenerator interface with dependency injection for better testability",
            ReplaceWith("ulidGenerator.generate()", "io.github.kamiazya.scopes.platform.commons.id.ULIDGenerator"),
        )
        fun generate(): ULID = fromString(KULID.random())
    }

    override fun toString(): String = value
}
