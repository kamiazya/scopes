package io.github.kamiazya.scopes.userpreferences.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError

sealed class PreferenceValue {
    abstract fun asString(): String

    data class StringValue(val value: String) : PreferenceValue() {
        override fun asString(): String = value
    }

    data class IntValue(val value: Int) : PreferenceValue() {
        override fun asString(): String = value.toString()
    }

    data class BooleanValue(val value: Boolean) : PreferenceValue() {
        override fun asString(): String = value.toString()
    }

    object NullValue : PreferenceValue() {
        override fun asString(): String = "null"
    }

    companion object {
        fun fromString(value: String): PreferenceValue = StringValue(value)

        fun fromInt(value: Int): PreferenceValue = IntValue(value)

        fun fromBoolean(value: Boolean): PreferenceValue = BooleanValue(value)

        fun fromNullable(value: Any?): PreferenceValue = when (value) {
            null -> NullValue
            is String -> fromString(value)
            is Int -> fromInt(value)
            is Boolean -> fromBoolean(value)
            else -> fromString(value.toString())
        }

        fun parseAsInt(value: PreferenceValue, key: String): Either<UserPreferencesError, Int?> = when (value) {
            is IntValue -> value.value.right()
            is NullValue -> null.right()
            is StringValue -> value.value.toIntOrNull()?.right() ?: UserPreferencesError.InvalidPreferenceValue(
                key = key,
                value = value.value,
                reason = "Expected integer value",
            ).left()
            is BooleanValue -> UserPreferencesError.InvalidPreferenceValue(
                key = key,
                value = value.value.toString(),
                reason = "Expected integer value, got boolean",
            ).left()
        }

        fun parseAsBoolean(value: PreferenceValue, key: String): Either<UserPreferencesError, Boolean> = when (value) {
            is BooleanValue -> value.value.right()
            is StringValue -> when (value.value.lowercase()) {
                "true", "yes", "1" -> true.right()
                "false", "no", "0" -> false.right()
                else -> UserPreferencesError.InvalidPreferenceValue(
                    key = key,
                    value = value.value,
                    reason = "Expected boolean value",
                ).left()
            }
            else -> UserPreferencesError.InvalidPreferenceValue(
                key = key,
                value = value.asString(),
                reason = "Expected boolean value",
            ).left()
        }
    }
}
