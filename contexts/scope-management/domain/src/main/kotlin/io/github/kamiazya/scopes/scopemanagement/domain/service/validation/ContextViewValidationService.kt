package io.github.kamiazya.scopes.scopemanagement.domain.service.validation

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Domain service for validating context view filter expressions.
 *
 * This service encapsulates the business rules for filter validation
 * in the domain layer, ensuring that validation logic is not spread
 * across application layer use cases.
 */
class ContextViewValidationService {
    /**
     * Validates a filter expression and returns a domain-appropriate error if invalid.
     *
     * @param filter The filter expression to validate
     * @return Either an error or Unit if valid
     */
    fun validateFilterExpression(filter: String): Either<ScopesError, Unit> {
        // For now, we just check if the filter is not empty
        // The actual parsing validation will be delegated to the application layer
        // which has access to the AspectQueryParser
        return if (filter.isBlank()) {
            Either.Left(ContextError.EmptyFilter)
        } else {
            Either.Right(Unit)
        }
    }
}
