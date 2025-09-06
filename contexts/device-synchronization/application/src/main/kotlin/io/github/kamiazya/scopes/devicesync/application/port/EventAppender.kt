package io.github.kamiazya.scopes.devicesync.application.port

import arrow.core.Either
import io.github.kamiazya.scopes.devicesync.application.error.DeviceSyncApplicationError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import kotlinx.datetime.Instant

/**
 * Port for appending events to the event store.
 *
 * This abstraction allows the application layer to work with events
 * without depending on specific event store contracts or implementations.
 */
interface EventAppender {
    /**
     * Retrieve events from the event store since the given timestamp.
     *
     * @param since The timestamp from which to retrieve events
     * @param limit Maximum number of events to retrieve
     * @return Either an error or list of domain events
     */
    suspend fun getEventsSince(since: Instant, limit: Int = 1000): Either<DeviceSyncApplicationError, List<DomainEvent>>
}
