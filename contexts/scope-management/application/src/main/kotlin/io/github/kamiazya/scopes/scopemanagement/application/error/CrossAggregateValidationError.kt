package io.github.kamiazya.scopes.scopemanagement.application.error

/**
 * Cross-aggregate validation errors that occur when validating invariants
 * that span multiple aggregates or bounded contexts.
 *
 * Note: This is an application-layer error that doesn't extend domain errors
 * to maintain proper layer separation.
 */
sealed class CrossAggregateValidationError : ScopeManagementApplicationError() {

    /**
     * Error when a cross-aggregate reference is violated.
     */
    data class CrossReferenceViolation(val sourceAggregate: String, val targetAggregate: String, val referenceType: String, val violation: String) :
        CrossAggregateValidationError()

    /**
     * Error when a cross-aggregate invariant is violated.
     */
    data class InvariantViolation(val invariantName: String, val aggregateIds: List<String>, val violationDescription: String) : CrossAggregateValidationError()

    /**
     * Error when compensating transaction fails in a saga.
     */
    data class CompensationFailure(val sagaName: String, val failedStep: String, val compensationError: String) : CrossAggregateValidationError()

    /**
     * Error when eventual consistency violation is detected.
     */
    data class EventualConsistencyViolation(val aggregateIds: List<String>, val expectedState: String, val actualState: String) :
        CrossAggregateValidationError()
}
