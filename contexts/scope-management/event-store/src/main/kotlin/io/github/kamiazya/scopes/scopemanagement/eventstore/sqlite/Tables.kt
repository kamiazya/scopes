package io.github.kamiazya.scopes.scopemanagement.eventstore.sqlite

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Events table for storing domain events.
 */
object Events : Table("events") {
    val eventId = varchar("event_id", 36)
    val aggregateId = varchar("aggregate_id", 255)
    val eventType = varchar("event_type", 255)
    val deviceId = varchar("device_id", 64)
    val sequenceNumber = long("sequence_number")
    val timestamp = timestamp("timestamp")
    val eventData = text("event_data")
    val vectorClock = text("vector_clock")

    override val primaryKey = PrimaryKey(eventId)

    init {
        // Create indexes for efficient querying
        index(true, deviceId, sequenceNumber)
        index(false, aggregateId)
        index(false, timestamp)
        index(false, deviceId, timestamp)
    }
}

/**
 * Vector clocks table for tracking the current vector clock per device.
 */
object VectorClocks : Table("vector_clocks") {
    val deviceId = varchar("device_id", 64)
    val vectorClock = text("vector_clock")
    val lastUpdated = timestamp("last_updated")

    override val primaryKey = PrimaryKey(deviceId)
}
