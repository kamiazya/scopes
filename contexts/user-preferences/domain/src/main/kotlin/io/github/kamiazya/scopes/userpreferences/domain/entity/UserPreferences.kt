package io.github.kamiazya.scopes.userpreferences.domain.entity

import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences
import kotlinx.datetime.Instant

data class UserPreferences(val hierarchyPreferences: HierarchyPreferences, val createdAt: Instant, val updatedAt: Instant) {

    /**
     * Updates the hierarchy preferences with a new value.
     * This method ensures the updatedAt timestamp is updated accordingly.
     */
    fun updateHierarchyPreferences(newPreferences: HierarchyPreferences, now: Instant): UserPreferences = copy(
        hierarchyPreferences = newPreferences,
        updatedAt = now,
    )

    /**
     * Returns a new instance with updated timestamp.
     * Useful for marking the preferences as recently accessed or validated.
     */
    fun withUpdatedTimestamp(now: Instant): UserPreferences = copy(updatedAt = now)

    /**
     * Checks if the preferences are using default values.
     */
    fun isDefault(): Boolean = hierarchyPreferences == HierarchyPreferences.DEFAULT

    /**
     * Checks if the user has customized any preferences.
     */
    fun hasCustomizations(): Boolean = !isDefault()

    /**
     * Merges these preferences with another set, with the other set taking precedence.
     * Useful for applying updates or overlays.
     */
    fun mergeWith(other: UserPreferences, now: Instant): UserPreferences = copy(hierarchyPreferences = other.hierarchyPreferences, updatedAt = now)

    /**
     * Resets preferences to default values while preserving creation timestamp.
     */
    fun resetToDefaults(now: Instant): UserPreferences = copy(hierarchyPreferences = HierarchyPreferences.DEFAULT, updatedAt = now)

    /**
     * Validates that the preferences are in a consistent state.
     * Returns true if valid, false otherwise.
     */
    fun isValid(): Boolean {
        // Add validation logic here as needed
        // For now, all states are considered valid
        return createdAt <= updatedAt
    }

    companion object {
        fun createDefault(now: Instant): UserPreferences = UserPreferences(
            hierarchyPreferences = HierarchyPreferences.DEFAULT,
            createdAt = now,
            updatedAt = now,
        )

        /**
         * Creates preferences with custom hierarchy settings.
         */
        fun createWithHierarchy(hierarchyPreferences: HierarchyPreferences, now: Instant): UserPreferences = UserPreferences(
            hierarchyPreferences = hierarchyPreferences,
            createdAt = now,
            updatedAt = now,
        )
    }
}
