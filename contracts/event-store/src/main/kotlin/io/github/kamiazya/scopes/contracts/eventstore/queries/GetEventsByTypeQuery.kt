package io.github.kamiazya.scopes.contracts.eventstore.queries

/**
 * Query to retrieve events by event type.
 * @property eventType The type of events to retrieve
 * @property limit Maximum number of events to return
 * @property offset Number of events to skip
 */
public data class GetEventsByTypeQuery(public val eventType: String, public val limit: Int = 100, public val offset: Int = 0)
