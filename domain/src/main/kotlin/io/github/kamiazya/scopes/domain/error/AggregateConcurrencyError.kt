package io.github.kamiazya.scopes.domain.error

import io.github.kamiazya.scopes.domain.valueobject.AggregateId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Errors specific to aggregate concurrency and consistency.
 *
 * These errors occur when multiple processes attempt to modify the same
 * aggregate simultaneously, or when events are applied in an invalid sequence.
 * They are part of the optimistic concurrency control mechanism.
 */
sealed class AggregateConcurrencyError : ConceptualModelError() {

    /**
     * Thrown when the expected version doesn't match the actual version.
     * This indicates a concurrent modification has occurred.
     *
     * Resolution: Reload the aggregate and retry the operation with the current version.
     */
    data class VersionMismatch(
        val aggregateId: AggregateId,
        val expectedVersion: Int,
        val actualVersion: Int,
        override val occurredAt: Instant = Clock.System.now(),
    ) : AggregateConcurrencyError()

    /**
     * Thrown when trying to apply events to an aggregate in wrong order.
     * This can happen during event replay if events are corrupted or out of sequence.
     *
     * Resolution: Check event store integrity and ensure events are loaded in correct order.
     */
    data class InvalidEventSequence(
        val aggregateId: AggregateId,
        val expectedEventVersion: Int,
        val actualEventVersion: Int,
        override val occurredAt: Instant = Clock.System.now(),
    ) : AggregateConcurrencyError()
}
