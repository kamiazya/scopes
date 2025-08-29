package io.github.kamiazya.scopes.eventstore.application.query

import kotlinx.datetime.Instant

/**
 * Query to retrieve events since a timestamp.
 */
data class GetEventsSince(val since: Instant, val limit: Int? = null)
