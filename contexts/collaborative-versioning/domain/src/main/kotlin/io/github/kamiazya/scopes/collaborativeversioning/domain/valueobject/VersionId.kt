package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.VersionIdError

/**
 * Strongly-typed identifier for resource versions using ULID.
 *
 * Each version represents a specific state of a resource at a point in time.
 */
@JvmInline
value class VersionId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new ULID-based VersionId.
         */
        fun generate(): VersionId = VersionId(ULID.random())

        /**
         * Create VersionId from existing ULID string.
         *
         * @param value Valid ULID string
         * @return Either<VersionIdError, VersionId>
         */
        fun from(value: String): Either<VersionIdError, VersionId> = if (isValidUlid(value)) {
            VersionId(value).right()
        } else {
            VersionIdError.InvalidFormat(
                providedValue = value,
                expectedFormat = "26-character ULID string",
            ).left()
        }

        private fun isValidUlid(value: String): Boolean = value.length == 26 && value.all { it.isLetterOrDigit() }
    }

    override fun toString(): String = value
}
