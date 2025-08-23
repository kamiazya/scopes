package io.github.kamiazya.scopes.userpreferences.domain.entity

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.value.HierarchySettings
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceKey
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceValue
import kotlinx.datetime.Instant

data class UserPreferences(
    val hierarchySettings: HierarchySettings,
    val customPreferences: Map<PreferenceKey, PreferenceValue>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {

    fun setPreference(key: PreferenceKey, value: PreferenceValue, updatedAt: Instant): UserPreferences = copy(
        customPreferences = customPreferences + (key to value),
        updatedAt = updatedAt,
    )

    fun removePreference(key: PreferenceKey, updatedAt: Instant): Either<UserPreferencesError, UserPreferences> = either {
        if (!customPreferences.containsKey(key)) {
            raise(UserPreferencesError.PreferenceNotFound(key.value))
        }
        copy(
            customPreferences = customPreferences - key,
            updatedAt = updatedAt,
        )
    }

    fun getPreference(key: PreferenceKey): PreferenceValue? = customPreferences[key]

    companion object {
        fun createDefault(now: Instant): UserPreferences = UserPreferences(
            hierarchySettings = HierarchySettings.DEFAULT,
            customPreferences = emptyMap(),
            createdAt = now,
            updatedAt = now,
        )
    }
}
