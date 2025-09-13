package io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject

import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.collaborativeversioning.domain.service.SystemTimeProvider
import kotlinx.datetime.Instant

/**
 * Represents a proposed change within a change proposal.
 *
 * A proposed change references either an existing changeset that should be applied
 * or contains inline changes that need to be reviewed and approved.
 */
sealed class ProposedChange {
    abstract val id: String
    abstract val resourceId: ResourceId
    abstract val description: String
    abstract val createdAt: Instant

    /**
     * A proposed change that references an existing changeset.
     * This is used when proposing to apply an already-created changeset.
     */
    data class FromChangeset(
        override val id: String,
        override val resourceId: ResourceId,
        override val description: String,
        override val createdAt: Instant,
        val changesetId: ChangesetId,
    ) : ProposedChange()

    /**
     * A proposed change with inline modifications.
     * This is used when proposing new changes that haven't been captured in a changeset yet.
     */
    data class Inline(
        override val id: String,
        override val resourceId: ResourceId,
        override val description: String,
        override val createdAt: Instant,
        val changes: List<Change>,
    ) : ProposedChange() {
        /**
         * Convert this inline change to a changeset when the proposal is approved.
         */
        fun toChangesetChanges(): List<Change> = changes
    }

    companion object {
        /**
         * Generate a unique ID for a proposed change.
         */
        fun generateId(): String = "proposed_change_${ULID.random()}"

        /**
         * Create a proposed change from an existing changeset.
         */
        fun fromChangeset(
            resourceId: ResourceId,
            changesetId: ChangesetId,
            description: String,
            timestamp: Instant = SystemTimeProvider().now(),
        ): ProposedChange = FromChangeset(
            id = generateId(),
            resourceId = resourceId,
            description = description,
            createdAt = timestamp,
            changesetId = changesetId,
        )

        /**
         * Create an inline proposed change.
         */
        fun inline(resourceId: ResourceId, changes: List<Change>, description: String, timestamp: Instant = SystemTimeProvider().now()): ProposedChange =
            Inline(
                id = generateId(),
                resourceId = resourceId,
                description = description,
                createdAt = timestamp,
                changes = changes,
            )
    }
}
