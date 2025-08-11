package io.github.kamiazya.scopes.application.service.error

/**
 * Application layer validation errors for input validation and business rule violations.
 * 
 * This hierarchy provides comprehensive error types for application-level validation
 * including input validation, cross-aggregate validation, business rules, and async validation.
 * 
 * Based on Serena MCP research on validation patterns:
 * - Field-level input validation with detailed feedback
 * - Cross-aggregate consistency enforcement
 * - Business rule validation at application boundaries
 * - Async validation with timeout handling
 * 
 * Following Arrow Either patterns for type-safe error handling.
 */
sealed class ApplicationValidationError

/**
 * Input validation errors for field-level validation problems.
 * These represent issues with the structure and format of input data.
 */
sealed class InputValidationError : ApplicationValidationError() {
    
    /**
     * A field has an invalid format according to validation rules.
     */
    data class InvalidFieldFormat(
        val fieldName: String,
        val expectedFormat: String,
        val actualValue: String,
        val validationRule: String
    ) : InputValidationError()
    
    /**
     * A required field is missing from the input.
     */
    data class MissingRequiredField(
        val fieldName: String,
        val entityType: String,
        val context: String? = null
    ) : InputValidationError()
    
    /**
     * A field value is outside the allowed range.
     */
    data class ValueOutOfRange(
        val fieldName: String,
        val minValue: Any?,
        val maxValue: Any?,
        val actualValue: Any
    ) : InputValidationError()
}

/**
 * Cross-aggregate validation errors for multi-aggregate consistency violations.
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
     * Invalid reference between aggregates.
     */
    data class CrossReferenceViolation(
        val sourceAggregate: String,
        val targetAggregate: String,
        val referenceType: String,
        val violation: String
    ) : CrossAggregateValidationError()
    
    /**
     * Invariant violation across multiple aggregates.
     */
    data class InvariantViolation(
        val invariantName: String,
        val aggregateIds: List<String>,
        val violationDescription: String
    ) : CrossAggregateValidationError()
}

/**
 * Business rule validation errors for application-level business logic violations.
 * These represent violations of business rules at the application layer.
 */
sealed class BusinessRuleValidationError : ApplicationValidationError() {
    
    /**
     * A precondition for an operation was not met.
     */
    data class PreconditionViolation(
        val operation: String,
        val precondition: String,
        val currentState: String,
        val requiredState: String
    ) : BusinessRuleValidationError()
    
    /**
     * A postcondition check failed after an operation.
     */
    data class PostconditionViolation(
        val operation: String,
        val postcondition: String,
        val expectedOutcome: String,
        val actualOutcome: String
    ) : BusinessRuleValidationError()
}

/**
 * Async validation errors for validation in asynchronous contexts.
 * These handle validation issues that occur in async contexts.
 */
sealed class AsyncValidationError : ApplicationValidationError() {
    
    /**
     * Validation operation timed out.
     */
    data class ValidationTimeout(
        val operation: String,
        val timeoutMillis: Long,
        val validationPhase: String,
        val partialResults: Map<String, Any>? = null
    ) : AsyncValidationError()
    
    /**
     * Concurrent validation conflict detected.
     */
    data class ConcurrentValidationConflict(
        val resource: String,
        val conflictingOperations: List<String>,
        val timestamp: Long,
        val resolution: String? = null
    ) : AsyncValidationError()
}