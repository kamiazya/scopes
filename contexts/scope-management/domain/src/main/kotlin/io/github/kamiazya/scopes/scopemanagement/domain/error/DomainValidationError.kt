package io.github.kamiazya.scopes.scopemanagement.domain.error

sealed class DomainValidationError {
    data class InvalidULID(val value: String) : DomainValidationError()

    sealed class PaginationViolation : DomainValidationError() {
        data class OffsetTooSmall(val offset: Int, val minOffset: Int) : PaginationViolation()
        data class LimitTooSmall(val limit: Int, val minLimit: Int) : PaginationViolation()
        data class LimitTooLarge(val limit: Int, val maxLimit: Int) : PaginationViolation()
    }

    data class EmptyField(val fieldName: String) : DomainValidationError()

    data class InvalidLength(val fieldName: String, val actualLength: Int, val minLength: Int, val maxLength: Int) : DomainValidationError()

    data class InvalidIdempotencyKey(val key: String, val reason: IdempotencyKeyError) : DomainValidationError() {
        enum class IdempotencyKeyError {
            INVALID_CHARACTERS,
            TOO_LONG,
            TOO_SHORT,
            INVALID_FORMAT,
        }
    }
}
