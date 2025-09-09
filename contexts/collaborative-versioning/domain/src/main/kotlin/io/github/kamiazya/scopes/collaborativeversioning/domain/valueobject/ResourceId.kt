package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.ResourceIdError

/**
 * Strongly-typed identifier for versioned resources using ULID.
 *
 * A resource represents any versionable entity in the system
 * (e.g., scopes, documents, configurations).
 */
@JvmInline
value class ResourceId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new ULID-based ResourceId.
         */
        fun generate(): ResourceId = ResourceId(ULID.random())

        /**
         * Create ResourceId from existing ULID string.
         *
         * @param value Valid ULID string
         * @return Either<ResourceIdError, ResourceId>
         */
        fun from(value: String): Either<ResourceIdError, ResourceId> = if (isValidUlid(value)) {
            ResourceId(value).right()
        } else {
            ResourceIdError.InvalidFormat(
                providedValue = value,
                expectedFormat = "26-character ULID string",
            ).left()
        }

        private fun isValidUlid(value: String): Boolean = value.length == 26 && value.all { it.isLetterOrDigit() }
    }

    override fun toString(): String = value
}
