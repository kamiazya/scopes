package io.github.kamiazya.scopes.application.service.error

/**
 * Application-level validation errors for cross-cutting validation concerns.
 * 
 * This hierarchy provides comprehensive error types for validation that spans
 * multiple aggregates, application services, and cross-cutting concerns.
 * 
 * Based on Serena MCP research on validation patterns in Clean Architecture:
 * - Field-level specificity for precise error reporting
 * - Cross-aggregate validation support
 * - Business rule validation with precondition/postcondition semantics
 * - Asynchronous validation error handling
 * 
 * Following Arrow Either patterns for type-safe error handling.
 */
sealed class ApplicationValidationError {

    /**
     * Input validation errors for field-level validation problems.
     * These represent issues with the structure and format of input data.
     */
    sealed class InputValidationError : ApplicationValidationError() {
        
        /**
         * A field has an invalid format according to validation rules.
         */
        data class InvalidFieldFormat(
            val field: String,
            val value: String,
            val expectedFormat: String,
            val validationRule: String
        ) : InputValidationError()
        
        /**
         * A required field is missing from the input.
         */
        data class MissingRequiredField(
            val field: String
        ) : InputValidationError()
        
        /**
         * A field violates specific constraints (length, range, etc.).
         */
        data class FieldConstraintViolation(
            val field: String,
            val constraint: String,
            val actualValue: String,
            val expectedValue: String
        ) : InputValidationError()
    }

    /**
     * Cross-aggregate validation errors for consistency across multiple aggregates.
     * These represent violations that span aggregate boundaries.
     */
    sealed class CrossAggregateValidationError : ApplicationValidationError() {
        
        /**
         * Consistency violation affecting multiple aggregates.
         */
        data class AggregateConsistencyViolation(
            val operation: String,
            val affectedAggregates: Set<String>,
            val consistencyRule: String,
            val violationDetails: String
        ) : CrossAggregateValidationError()
        
        /**
         * Cross-reference violation between aggregates.
         */
        data class CrossReferenceViolation(
            val sourceAggregate: String,
            val targetAggregate: String,
            val referenceType: String,
            val violation: String
        ) : CrossAggregateValidationError()
        
        /**
         * Domain invariant violation across multiple aggregates.
         */
        data class InvariantViolation(
            val invariantName: String,
            val aggregateIds: List<String>,
            val violationDescription: String
        ) : CrossAggregateValidationError()
    }

    /**
     * Business rule validation errors for complex business logic.
     * These represent violations of business rules at the application layer.
     */
    sealed class BusinessRuleValidationError : ApplicationValidationError() {
        
        /**
         * A precondition for an operation was not met.
         */
        data class PreconditionViolation(
            val operation: String,
            val precondition: String,
            val actualCondition: String,
            val affectedEntityId: String
        ) : BusinessRuleValidationError()
        
        /**
         * A postcondition for an operation was violated.
         */
        data class PostconditionViolation(
            val operation: String,
            val postcondition: String,
            val actualResult: String,
            val affectedEntityIds: List<String>
        ) : BusinessRuleValidationError()
    }

    /**
     * Asynchronous validation errors for concurrent and external validations.
     * These handle validation issues that occur in async contexts.
     */
    sealed class AsyncValidationError : ApplicationValidationError() {
        
        /**
         * Validation operation timed out.
         */
        data class ValidationTimeout(
            val validationType: String,
            val timeoutMs: Long,
            val elapsedMs: Long
        ) : AsyncValidationError()
        
        /**
         * Concurrent modification detected during validation.
         */
        data class ConcurrentModificationDetected(
            val entityId: String,
            val expectedVersion: Int,
            val actualVersion: Int,
            val operation: String
        ) : AsyncValidationError()
        
        /**
         * External service validation failed.
         */
        data class ExternalServiceValidationFailure(
            val serviceName: String,
            val validationType: String,
            val cause: Throwable,
            val retryAttempts: Int
        ) : AsyncValidationError()
    }
}