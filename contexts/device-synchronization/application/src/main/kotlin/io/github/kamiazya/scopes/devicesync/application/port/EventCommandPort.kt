package io.github.kamiazya.scopes.devicesync.application.port

import arrow.core.Either
import io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent

/**
 * Port for appending events to the event store.
 *
 * This abstraction allows the application layer to write events
 * without depending on specific event store contracts or implementations.
 */
interface EventCommandPort {
    /**
     * Append a single event to the event store.
     *
     * @param event The domain event to append
     * @return Either an error or unit on successful append
     */
    suspend fun append(event: DomainEvent): Either<DeviceSyncApplicationError, Unit>

    /**
     * Append multiple events to the event store in a batch.
     *
     * @param events The list of domain events to append
     * @return Either an error or unit on successful append
     */
    suspend fun appendBatch(events: List<DomainEvent>): Either<DeviceSyncApplicationError, Unit>
}
