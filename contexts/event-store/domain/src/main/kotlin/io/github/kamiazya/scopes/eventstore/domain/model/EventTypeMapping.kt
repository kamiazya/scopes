package io.github.kamiazya.scopes.eventstore.domain.model

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import kotlin.reflect.KClass

/**
 * Defines the mapping between domain events and their stable type identifiers.
 *
 * This abstraction decouples event persistence from runtime class names,
 * enabling safe refactoring and cross-bounded context compatibility.
 */
interface EventTypeMapping {
    /**
     * Get the stable type identifier for an event class.
     * This identifier is persisted and must remain stable across versions.
     */
    fun getTypeId(eventClass: KClass<out DomainEvent>): String

    /**
     * Get the event class for a given type identifier.
     * Used during deserialization to reconstruct events.
     */
    fun getEventClass(typeId: String): KClass<out DomainEvent>?

    /**
     * Register an event type with its stable identifier.
     * This is typically done during application initialization.
     */
    fun register(eventClass: KClass<out DomainEvent>, typeId: String)
}
