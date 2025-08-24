package io.github.kamiazya.scopes.userpreferences.infrastructure.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class UserPreferencesConfig(val version: Int = CURRENT_VERSION, val hierarchyPreferences: HierarchyPreferencesConfig = HierarchyPreferencesConfig()) {
    companion object {
        const val CURRENT_VERSION = 1
        const val CONFIG_FILE_NAME = "preferences.json"

        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

@Serializable
data class HierarchyPreferencesConfig(val maxDepth: Int? = null, val maxChildrenPerScope: Int? = null)
