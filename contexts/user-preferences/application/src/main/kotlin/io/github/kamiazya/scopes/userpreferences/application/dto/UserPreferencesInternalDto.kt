package io.github.kamiazya.scopes.userpreferences.application.dto

import io.github.kamiazya.scopes.platform.application.dto.DTO
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import kotlinx.datetime.Instant

data class UserPreferencesInternalDto(
    val hierarchySettings: HierarchySettingsInternalDto,
    val customPreferences: Map<String, String>,
    val createdAt: Instant,
    val updatedAt: Instant,
) : DTO {
    companion object {
        fun from(preferences: UserPreferences): UserPreferencesInternalDto = UserPreferencesInternalDto(
            hierarchySettings = HierarchySettingsInternalDto.from(preferences.hierarchySettings),
            customPreferences = preferences.customPreferences
                .mapKeys { it.key.value }
                .mapValues { it.value.asString() },
            createdAt = preferences.createdAt,
            updatedAt = preferences.updatedAt,
        )
    }
}

data class HierarchySettingsInternalDto(val maxDepth: Int?, val maxChildrenPerScope: Int?) : DTO {
    companion object {
        fun from(settings: io.github.kamiazya.scopes.userpreferences.domain.value.HierarchySettings): HierarchySettingsInternalDto =
            HierarchySettingsInternalDto(
                maxDepth = settings.maxDepth,
                maxChildrenPerScope = settings.maxChildrenPerScope,
            )
    }
}
