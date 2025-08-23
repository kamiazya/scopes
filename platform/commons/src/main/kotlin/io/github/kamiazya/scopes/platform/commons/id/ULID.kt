package io.github.kamiazya.scopes.platform.commons.id

import com.github.guepardoapps.kulid.ULID as KULID

@JvmInline
value class ULID(val value: String) {
    init {
        require(isValid(value)) { "Invalid ULID format: $value" }
    }

    companion object {
        fun generate(): ULID = ULID(KULID.random())

        fun fromString(value: String): ULID = ULID(value)

        fun isValid(value: String): Boolean = try {
            // KULID doesn't have a validation method, so we check the format
            value.length == 26 && value.all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' }
        } catch (e: Exception) {
            false
        }
    }

    override fun toString(): String = value
}
