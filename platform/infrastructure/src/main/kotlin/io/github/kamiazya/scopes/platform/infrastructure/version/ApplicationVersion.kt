package io.github.kamiazya.scopes.platform.infrastructure.version

/**
 * Application version information and database schema versioning.
 *
 * This singleton provides:
 * - Application semantic version
 * - Database schema version mapping
 * - Version comparison utilities
 */
object ApplicationVersion {
    /**
     * Current application version (semantic versioning).
     * This should be updated with each release.
     */
    const val CURRENT_VERSION = "0.1.0"

    /**
     * Database schema versions for each bounded context.
     * These versions are incremented when database schema changes.
     */
    object SchemaVersions {
        /**
         * Scope Management context database schema version.
         */
        const val SCOPE_MANAGEMENT = 1L

        /**
         * Event Store context database schema version.
         */
        const val EVENT_STORE = 1L

        /**
         * Device Synchronization context database schema version.
         */
        const val DEVICE_SYNCHRONIZATION = 1L

        /**
         * User Preferences context database schema version.
         * (When implemented with database storage)
         */
        const val USER_PREFERENCES = 1L
    }

    /**
     * Maps application version to schema versions.
     * This helps track which schema versions are compatible with which app versions.
     */
    data class VersionMapping(
        val appVersion: String,
        val scopeManagementSchema: Long,
        val eventStoreSchema: Long,
        val deviceSyncSchema: Long,
        val userPreferencesSchema: Long
    )

    /**
     * Historical version mappings for reference.
     * Add new entries when releasing versions with schema changes.
     */
    val versionHistory = listOf(
        VersionMapping(
            appVersion = "0.1.0",
            scopeManagementSchema = 1L,
            eventStoreSchema = 1L,
            deviceSyncSchema = 1L,
            userPreferencesSchema = 1L
        )
        // Add future versions here as they are released
        // Example:
        // VersionMapping(
        //     appVersion = "0.2.0",
        //     scopeManagementSchema = 2L,  // Schema updated
        //     eventStoreSchema = 1L,        // No change
        //     deviceSyncSchema = 1L,        // No change
        //     userPreferencesSchema = 1L   // No change
        // )
    )

    /**
     * Gets the current version mapping.
     */
    fun getCurrentMapping(): VersionMapping {
        return versionHistory.last()
    }

    /**
     * Parses semantic version string into comparable parts.
     */
    fun parseVersion(version: String): Triple<Int, Int, Int> {
        val parts = version.split(".")
        return Triple(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }

    /**
     * Compares two semantic version strings.
     * Returns:
     * - negative if version1 < version2
     * - zero if version1 == version2
     * - positive if version1 > version2
     */
    fun compareVersions(version1: String, version2: String): Int {
        val v1 = parseVersion(version1)
        val v2 = parseVersion(version2)

        return when {
            v1.first != v2.first -> v1.first.compareTo(v2.first)
            v1.second != v2.second -> v1.second.compareTo(v2.second)
            else -> v1.third.compareTo(v2.third)
        }
    }

    /**
     * Checks if the application version is compatible with a database version.
     */
    fun isCompatible(databaseSchemaVersion: Long, contextSchemaVersion: Long): Boolean {
        return databaseSchemaVersion <= contextSchemaVersion
    }
}