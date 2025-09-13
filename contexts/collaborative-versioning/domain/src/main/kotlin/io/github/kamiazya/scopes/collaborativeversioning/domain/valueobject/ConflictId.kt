package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import com.github.guepardoapps.kulid.ULID

/**
 * Conflict ID value object.
 */
@JvmInline
value class ConflictId private constructor(private val value: String) {
    override fun toString(): String = value

    companion object {
        fun generate(): ConflictId = ConflictId(ULID.random())
        fun from(value: String): ConflictId = ConflictId(value)
    }
}
