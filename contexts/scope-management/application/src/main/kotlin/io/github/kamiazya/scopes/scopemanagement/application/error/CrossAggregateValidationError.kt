package io.github.kamiazya.scopes.scopemanagement.application.error

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Cross-aggregate validation errors that occur when validating invariants
 * that span multiple aggregates or bounded contexts.
 *
 * Note: This is an application-layer error that doesn't extend domain errors
 * to maintain proper layer separation.
 */
sealed class CrossAggregateValidationError {
    abstract val message: String
    abstract val occurredAt: Instant

    /**
     * Error when a cross-aggregate reference is violated.
     */
    data class CrossReferenceViolation(
        val sourceAggregate: String,
        val targetAggregate: String,
        val referenceType: String,
        val violation: String,
        override val occurredAt: Instant = Clock.System.now(),
    ) : CrossAggregateValidationError() {
        override val message: String = "Cross-reference violation: $violation (from $sourceAggregate to $targetAggregate via $referenceType)"
    }

    /**
     * Error when a cross-aggregate invariant is violated.
     */
    data class InvariantViolation(
        val invariantName: String,
        val aggregateIds: List<String>,
        val violationDescription: String,
        override val occurredAt: Instant = Clock.System.now(),
    ) : CrossAggregateValidationError() {
        override val message: String = "Invariant '$invariantName' violated across aggregates ${aggregateIds.joinToString()}: $violationDescription"
    }

    /**
     * Error when compensating transaction fails in a saga.
     */
    data class CompensationFailure(
        val sagaName: String,
        val failedStep: String,
        val compensationError: String,
        override val occurredAt: Instant = Clock.System.now(),
    ) : CrossAggregateValidationError() {
        override val message: String = "Compensation failed in saga '$sagaName' at step '$failedStep': $compensationError"
    }

    /**
     * Error when eventual consistency violation is detected.
     */
    data class EventualConsistencyViolation(
        val aggregateIds: List<String>,
        val expectedState: String,
        val actualState: String,
        override val occurredAt: Instant = Clock.System.now(),
    ) : CrossAggregateValidationError() {
        override val message: String =
            "Eventual consistency violation for aggregates ${aggregateIds.joinToString()}: expected $expectedState but found $actualState"
    }
}
