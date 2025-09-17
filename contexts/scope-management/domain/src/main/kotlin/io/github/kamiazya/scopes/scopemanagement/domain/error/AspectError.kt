package io.github.kamiazya.scopes.scopemanagement.domain.error

sealed class AspectError {
    object NoAspectsProvided : AspectError()

    data class InvalidAspectKey(val key: String, val reason: KeyError) : AspectError() {
        enum class KeyError {
            EMPTY,
            CONTAINS_INVALID_CHARACTERS,
            TOO_LONG,
            RESERVED_KEYWORD,
        }
    }

    data class InvalidAspectValue(val key: String, val value: String, val reason: ValueError) : AspectError() {
        enum class ValueError {
            EMPTY,
            TOO_LONG,
            INVALID_FORMAT,
            OUT_OF_RANGE,
        }
    }

    data class InvalidAspectFormat(val reason: FormatError) : AspectError() {
        enum class FormatError {
            NO_DELIMITER,
            MULTIPLE_DELIMITERS,
            EMPTY_KEY,
            EMPTY_VALUE,
            MALFORMED,
        }
    }
}
