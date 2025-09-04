package io.github.kamiazya.scopes.scopemanagement.domain.service.validation

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ContextError

/**
 * Domain service interface for validating filter expressions.
 *
 * This interface defines the contract for filter validation without
 * exposing the technical details of how the validation is performed.
 * The actual implementation will be provided by the infrastructure layer.
 */
interface FilterExpressionValidator {
    /**
     * Validates a filter expression according to the business rules.
     *
     * @param expression The filter expression to validate
     * @return Either a validation error or Unit if the expression is valid
     */
    fun validate(expression: String): Either<ContextError, Unit>
}
