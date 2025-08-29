package io.github.kamiazya.scopes.eventstore.domain.entity

import io.github.kamiazya.scopes.eventstore.domain.valueobject.EventMetadata
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent

/**
 * Represents a persisted event record with its metadata.
 * This is an entity that wraps a domain event with additional storage metadata.
 *
 * @property metadata The metadata associated with the stored event
 * @property event The actual domain event
 */
data class PersistedEventRecord(val metadata: EventMetadata, val event: DomainEvent)
