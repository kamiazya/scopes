package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.domain.error.ScopeInputError
import io.github.kamiazya.scopes.domain.error.currentTimestamp

/**
 * Value object representing a scope description with embedded validation.
 * Encapsulates the business rules for scope descriptions following DDD principles.
 * Nullable to represent optional descriptions.
 */
@JvmInline
value class ScopeDescription private constructor(val value: String) {

    companion object {
        const val MAX_LENGTH = 1000

        /**
         * Create a validated ScopeDescription from a nullable string.
         * Returns Either with specific error type or nullable ScopeDescription.
         * Null input or blank strings result in null ScopeDescription.
         */
        fun create(description: String?): Either<ScopeInputError.DescriptionError, ScopeDescription?> = either {
            when (description) {
                null -> null
                else -> {
                    val trimmedDescription = description.trim()
                    if (trimmedDescription.isEmpty()) {
                        null
                    } else {
                        ensure(trimmedDescription.length <= MAX_LENGTH) {
                            ScopeInputError.DescriptionError.TooLong(
                                currentTimestamp(),
                                description,
                                MAX_LENGTH
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

