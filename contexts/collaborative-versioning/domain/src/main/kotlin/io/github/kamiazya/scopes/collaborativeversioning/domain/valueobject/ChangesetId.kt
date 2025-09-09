package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import io.github.kamiazya.scopes.platform.commons.id.ULID

/**
 * Unique identifier for a changeset.
 * A changeset represents a collection of changes made by an agent.
 */
@JvmInline
value class ChangesetId(val value: String) {
    companion object {
        fun generate(): ChangesetId = ChangesetId(ULID.generate().toString())
    }
}
