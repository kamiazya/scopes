package io.github.kamiazya.scopes.contracts.userpreferences.queries

/**
 * Query for retrieving user preferences.
 *
 * Currently supports hierarchy preferences via PreferenceKey.
 * Future preferences can be added by extending PreferenceKey.
 */
data class GetPreferenceQuery(val userId: String, val key: PreferenceKey) {
    /**
     * Enumeration of available preference keys.
     * Currently only hierarchy preferences are supported.
     */
    enum class PreferenceKey {
        HIERARCHY,
    }
}
