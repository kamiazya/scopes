package io.github.kamiazya.scopes.userpreferences.application.dto

import io.github.kamiazya.scopes.platform.application.dto.DTO
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import kotlinx.datetime.Instant

data class UserPreferencesInternalDto(val hierarchyPreferences: HierarchyPreferencesInternalDto, val createdAt: Instant, val updatedAt: Instant) : DTO {
    companion object {
        fun from(preferences: UserPreferences): UserPreferencesInternalDto = UserPreferencesInternalDto(
            hierarchyPreferences = HierarchyPreferencesInternalDto.from(preferences.hierarchyPreferences),
            createdAt = preferences.createdAt,
            updatedAt = preferences.updatedAt,
        )
    }
}

data class HierarchyPreferencesInternalDto(val maxDepth: Int?, val maxChildrenPerScope: Int?) : DTO {
    companion object {
        fun from(preferences: io.github.kamiazya.scopes.userpreferences.domain.value.HierarchyPreferences): HierarchyPreferencesInternalDto =
            HierarchyPreferencesInternalDto(
                maxDepth = preferences.maxDepth,
                maxChildrenPerScope = preferences.maxChildrenPerScope,
            )
    }
}
