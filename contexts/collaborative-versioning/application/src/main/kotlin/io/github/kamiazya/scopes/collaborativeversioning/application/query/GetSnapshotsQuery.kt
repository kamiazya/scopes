package io.github.kamiazya.scopes.collaborativeversioning.application.query

import io.github.kamiazya.scopes.collaborativeversioning.domain.valueobject.ResourceId

/**
 * Query to retrieve all snapshots for a resource.
 *
 * This query fetches all snapshots for a given resource,
 * ordered by version number.
 */
data class GetSnapshotsQuery(val resourceId: ResourceId)
