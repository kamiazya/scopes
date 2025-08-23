package io.github.kamiazya.scopes.userpreferences.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError

@JvmInline
value class PreferenceKey private constructor(val value: String) {
    companion object {
        private val VALID_KEY_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_.-]*$")
        private const val MAX_LENGTH = 100

        fun create(value: String): Either<UserPreferencesError, PreferenceKey> {
            val trimmed = value.trim()
            return when {
                trimmed.isEmpty() -> UserPreferencesError.InvalidPreferenceValue(
                    key = "key",
                    value = value,
                    reason = "Key cannot be empty",
                ).left()
                trimmed.length > MAX_LENGTH -> UserPreferencesError.InvalidPreferenceValue(
                    key = "key",
                    value = value,
                    reason = "Key length cannot exceed $MAX_LENGTH characters",
                ).left()
                !VALID_KEY_PATTERN.matches(trimmed) -> UserPreferencesError.InvalidPreferenceValue(
                    key = "key",
                    value = value,
                    reason = "Key must start with a letter and contain only letters, numbers, underscores, dots, or hyphens",
                ).left()
                else -> PreferenceKey(trimmed).right()
            }
        }

        // Well-known keys
        val HIERARCHY_MAX_DEPTH = PreferenceKey("hierarchy.maxDepth")
        val HIERARCHY_MAX_CHILDREN = PreferenceKey("hierarchy.maxChildrenPerScope")
        val THEME_NAME = PreferenceKey("theme.name")
        val EDITOR_TAB_SIZE = PreferenceKey("editor.tabSize")
        val EDITOR_USE_SPACES = PreferenceKey("editor.useSpaces")
    }
}
