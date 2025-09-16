package io.github.kamiazya.scopes.collaborativeversioning.application.query

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId
import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.SnapshotId

/**
 * Query to retrieve a specific snapshot.
 *
 * This query fetches a single snapshot by its ID for a given resource.
 */
data class GetSnapshotQuery(val resourceId: ResourceId, val snapshotId: SnapshotId)
