package io.github.kamiazya.scopes.scopemanagement.eventstore.config

import kotlinx.serialization.Serializable

/**
 * Configuration for the Event Store.
 */
@Serializable
data class EventStoreConfig(
    /**
     * Path to the SQLite database file.
     */
    val databasePath: String = "scopes-events.db",

    /**
     * Maximum number of events to store before cleanup.
     */
    val maxEvents: Long = 1_000_000L,

    /**
     * Whether to enable WAL mode for better performance.
     */
    val enableWalMode: Boolean = true,

    /**
     * Connection pool size for concurrent access.
     */
    val connectionPoolSize: Int = 5,

    /**
     * Maximum time to retain events in days.
     * Events older than this will be eligible for cleanup.
     */
    val retentionDays: Int = 365,

    /**
     * Synchronization settings.
     */
    val synchronization: SynchronizationConfig = SynchronizationConfig(),
)

/**
 * Configuration for synchronization between devices.
 */
@Serializable
data class SynchronizationConfig(
    /**
     * Maximum number of events to sync in a single batch.
     */
    val batchSize: Int = 100,

    /**
     * Interval between sync attempts in minutes.
     */
    val syncIntervalMinutes: Int = 5,

    /**
     * Maximum number of retry attempts for failed syncs.
     */
    val maxRetryAttempts: Int = 3,

    /**
     * Whether to enable automatic conflict resolution.
     */
    val enableAutoConflictResolution: Boolean = true,
)
