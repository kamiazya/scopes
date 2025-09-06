package io.github.kamiazya.scopes.eventstore.application.query

import kotlinx.datetime.Instant

/**
 * Query to retrieve events since a timestamp.
 * Follows CQRS naming convention where all queries end with 'Query' suffix.
 */
data class GetEventsSinceQuery(val since: Instant, val limit: Int? = null)
