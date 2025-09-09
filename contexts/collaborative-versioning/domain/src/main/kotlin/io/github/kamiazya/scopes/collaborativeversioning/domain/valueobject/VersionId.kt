package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import io.github.kamiazya.scopes.platform.commons.id.ULID

/**
 * Unique identifier for a version.
 * A version represents a specific state of an entity at a point in time.
 */
@JvmInline
value class VersionId(val value: String) {
    companion object {
        fun generate(): VersionId = VersionId(ULID.generate().toString())
    }
}
