package io.github.kamiazya.scopes.contracts.userpreferences.queries

/**
 * Query for retrieving user preferences.
 *
 * Currently supports hierarchy preferences via PreferenceKey.
 * Future preferences can be added by extending PreferenceKey.
 */
public data class GetPreferenceQuery(public val key: PreferenceKey) {
    /**
     * Enumeration of available preference keys.
     * Currently only hierarchy preferences are supported.
     */
    public enum class PreferenceKey {
        HIERARCHY,
    }
}
