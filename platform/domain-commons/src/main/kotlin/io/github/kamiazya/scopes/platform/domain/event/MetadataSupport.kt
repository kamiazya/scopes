package io.github.kamiazya.scopes.platform.domain.event

/**
 * Interface for domain events that support metadata updates.
 * Events implementing this interface can have their metadata updated via the withMetadata method.
 */
interface MetadataSupport<T : DomainEvent> {
    fun withMetadata(metadata: EventMetadata): T
}
