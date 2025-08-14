package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.ScopeValidationError
import kotlinx.serialization.Serializable

/**
 * Value object representing a scope description with embedded validation.
 * Encapsulates the business rules for scope descriptions following DDD principles.
 * Nullable to represent optional descriptions.
 */
@Serializable
@JvmInline
value class ScopeDescription private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 1000

        /**
         * Create a validated ScopeDescription from a nullable string.
         * Returns Either with validation error or nullable ScopeDescription.
         * Null input or blank strings result in null ScopeDescription.
         */
        fun create(description: String?): Either<ScopeValidationError, ScopeDescription?> = either {
            when (description) {
                null -> null
                else -> {
                    val trimmedDescription = description.trim()
                    if (trimmedDescription.isEmpty()) {
                        null
                    } else {
                        ensure(trimmedDescription.length <= MAX_LENGTH) {
                            ScopeValidationError.ScopeDescriptionTooLong(
                                MAX_LENGTH,
                                trimmedDescription.length
                            )
                        }
                        ScopeDescription(trimmedDescription)
                    }
                }
            }
        }


    }

    override fun toString(): String = value
}
