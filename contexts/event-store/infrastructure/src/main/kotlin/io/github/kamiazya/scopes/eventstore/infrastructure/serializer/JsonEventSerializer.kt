package io.github.kamiazya.scopes.eventstore.infrastructure.serializer

import arrow.core.Either
import io.github.kamiazya.scopes.eventstore.application.port.EventSerializer
import io.github.kamiazya.scopes.eventstore.domain.error.EventStoreError
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

/**
 * JSON-based implementation of EventSerializer.
 *
 * This implementation uses kotlinx.serialization to handle event serialization/deserialization.
 * Domain events must be registered in the serializers module to be properly handled.
 */
class JsonEventSerializer(
    private val json: Json = Json {
        serializersModule = SerializersModule {
            polymorphic(DomainEvent::class) {
                // Register domain event subclasses here
                // Example:
                // subclass(ScopeCreatedEvent::class)
                // subclass(ScopeDeletedEvent::class)
                // etc.
            }
        }
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        isLenient = true
    },
) : EventSerializer {

    override fun serialize(event: DomainEvent): Either<EventStoreError.InvalidEventError, String> = try {
        val serialized = json.encodeToString<DomainEvent>(event)
        Either.Right(serialized)
    } catch (e: Exception) {
        Either.Left(
            EventStoreError.InvalidEventError(
                eventType = event::class.simpleName,
                validationErrors = listOf(
                    EventStoreError.ValidationIssue(
                        field = "event",
                        rule = EventStoreError.ValidationRule.INVALID_TYPE,
                        actualValue = e.message,
                    ),
                ),
            ),
        )
    }

    override fun deserialize(eventType: String, eventData: String): Either<EventStoreError.InvalidEventError, DomainEvent> = try {
        val event = json.decodeFromString<DomainEvent>(eventData)
        Either.Right(event)
    } catch (e: Exception) {
        Either.Left(
            EventStoreError.InvalidEventError(
                eventType = eventType,
                validationErrors = listOf(
                    EventStoreError.ValidationIssue(
                        field = "eventData",
                        rule = EventStoreError.ValidationRule.INVALID_FORMAT,
                        actualValue = e.message,
                    ),
                ),
            ),
        )
    }
}
