package io.github.kamiazya.scopes.userpreferences.domain.entity

import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences
import kotlinx.datetime.Instant

data class UserPreferences(val hierarchyPreferences: HierarchyPreferences, val createdAt: Instant, val updatedAt: Instant) {
    companion object {
        fun createDefault(now: Instant): UserPreferences = UserPreferences(
            hierarchyPreferences = HierarchyPreferences.DEFAULT,
            createdAt = now,
            updatedAt = now,
        )
    }
}
