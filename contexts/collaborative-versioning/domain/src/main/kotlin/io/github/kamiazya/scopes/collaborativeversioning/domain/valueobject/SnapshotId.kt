package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.collaborativeversioning.domain.error.SnapshotIdError

/**
 * Strongly-typed identifier for resource snapshots using ULID.
 *
 * A snapshot represents the complete state of a resource at a specific
 * version. Each snapshot is immutable once created.
 */
@JvmInline
value class SnapshotId private constructor(val value: String) {
    companion object {
        /**
         * Generate a new ULID-based SnapshotId.
         */
        fun generate(): SnapshotId = SnapshotId(ULID.random())

        /**
         * Create SnapshotId from existing ULID string.
         *
         * @param value Valid ULID string
         * @return Either<SnapshotIdError, SnapshotId>
         */
        fun from(value: String): Either<SnapshotIdError, SnapshotId> = if (isValidUlid(value)) {
            SnapshotId(value).right()
        } else {
            SnapshotIdError.InvalidFormat(
                providedValue = value,
                expectedFormat = "26-character ULID string",
            ).left()
        }

        private fun isValidUlid(value: String): Boolean = value.length == 26 && value.all { it.isLetterOrDigit() }
    }

    override fun toString(): String = value
}
