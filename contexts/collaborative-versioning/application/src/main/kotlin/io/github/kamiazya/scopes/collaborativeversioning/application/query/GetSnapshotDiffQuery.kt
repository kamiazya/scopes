package io.github.kamiazya.scopes.collaborativeversioning.application.query

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId

/**
 * Query to calculate the difference between two snapshots.
 *
 * This query computes the changes between two snapshots,
 * providing detailed information about modifications.
 */
data class GetSnapshotDiffQuery(val resourceId: ResourceId, val fromSnapshotId: SnapshotId, val toSnapshotId: SnapshotId)
