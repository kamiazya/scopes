package io.github.kamiazya.scopes.eventstore.infrastructure.mapping

import io.github.kamiazya.scopes.eventstore.domain.model.EventTypeId
import io.github.kamiazya.scopes.eventstore.domain.model.EventTypeMapping
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * Default implementation of EventTypeMapping that uses annotations and fallback strategies.
 *
 * Resolution order:
 * 1. @EventTypeId annotation on the event class
 * 2. Explicitly registered mappings (from EventTypeRegistry)
 * 3. Legacy fallback to qualified class name (for backward compatibility)
 */
class DefaultEventTypeMapping(private val logger: Logger, registeredMappings: Map<KClass<out DomainEvent>, String> = emptyMap()) : EventTypeMapping {

    private val typeToClass = mutableMapOf<String, KClass<out DomainEvent>>()
    private val classToType = mutableMapOf<KClass<out DomainEvent>, String>()

    init {
        // Initialize with pre-registered mappings from EventTypeRegistry
        registeredMappings.forEach { (eventClass, typeId) ->
            register(eventClass, typeId)
        }
    }

    override fun getTypeId(eventClass: KClass<out DomainEvent>): String {
        // Check cache first
        classToType[eventClass]?.let { return it }

        // Check for annotation
        eventClass.findAnnotation<EventTypeId>()?.let { annotation ->
            val typeId = annotation.value
            register(eventClass, typeId)
            return typeId
        }

        // Legacy fallback - use qualified name
        val qualifiedName = eventClass.qualifiedName ?: eventClass.simpleName
            ?: error("Cannot determine type ID for event class: $eventClass")

        logger.warn("Using legacy class name as event type ID for $eventClass. Consider adding @EventTypeId annotation.")
        return qualifiedName
    }

    override fun getEventClass(typeId: String): KClass<out DomainEvent>? = typeToClass[typeId]

    override fun register(eventClass: KClass<out DomainEvent>, typeId: String) {
        val existingClass = typeToClass[typeId]
        if (existingClass != null && existingClass != eventClass) {
            error("Type ID '$typeId' is already registered to $existingClass")
        }

        val existingTypeId = classToType[eventClass]
        if (existingTypeId != null && existingTypeId != typeId) {
            error("Event class $eventClass is already registered with type ID '$existingTypeId'")
        }

        typeToClass[typeId] = eventClass
        classToType[eventClass] = typeId
        logger.debug("Registered event type mapping: $typeId -> ${eventClass.simpleName}")
    }

    /**
     * Register legacy class name mappings for backward compatibility.
     * This allows deserializing events that were persisted before introducing stable type IDs.
     */
    fun registerLegacyMapping(eventClass: KClass<out DomainEvent>) {
        val qualifiedName = eventClass.qualifiedName
        val simpleName = eventClass.simpleName

        if (qualifiedName != null && !typeToClass.containsKey(qualifiedName)) {
            typeToClass[qualifiedName] = eventClass
            logger.debug("Registered legacy qualified name mapping: $qualifiedName -> ${eventClass.simpleName}")
        }

        if (simpleName != null && !typeToClass.containsKey(simpleName)) {
            typeToClass[simpleName] = eventClass
            logger.debug("Registered legacy simple name mapping: $simpleName -> ${eventClass.simpleName}")
        }
    }
}
