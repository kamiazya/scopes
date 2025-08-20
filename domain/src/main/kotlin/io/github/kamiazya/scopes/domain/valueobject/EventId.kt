package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.github.guepardoapps.kulid.ULID
import io.github.kamiazya.scopes.domain.error.EventIdError
import kotlinx.datetime.Clock
import kotlin.reflect.KClass

/**
 * A URI-based event identifier following the event ID pattern.
 *
 * Format: evt://scopes/{EventType}/{ULID}
 * Example: evt://scopes/ScopeCreated/01HX3BQXYZ
 *
 * This provides a globally unique, self-describing identifier that includes:
 * - The schema (evt)
 * - The application namespace (scopes)
 * - The event type
 * - The unique identifier (ULID for time-ordered sorting)
 */
@JvmInline
value class EventId private constructor(val value: String) {

    /**
     * Extract the event type from the URI.
     * For "evt://scopes/ScopeCreated/01HX3BQXYZ", returns "ScopeCreated"
     */
    val eventType: String
        get() = value.split("/")[3]

    /**
     * Extract the ULID portion from the URI.
     * For "evt://scopes/ScopeCreated/01HX3BQXYZ", returns "01HX3BQXYZ"
     */
    val ulid: String
        get() = value.split("/")[4]

    /**
     * Returns the full URI string representation.
     */
    override fun toString(): String = value

    companion object {
        private const val SCHEMA = "evt"
        private const val NAMESPACE = "scopes"
        private val URI_PATTERN = Regex("^evt://scopes/[A-Z][A-Za-z]+/[0-9A-HJKMNP-TV-Z]{26}$")

        /**
         * Create an EventId for a specific event type.
         * Generates a new ULID for uniqueness and time-ordering.
         *
         * @param eventType The type of event (e.g., "ScopeCreated", "ScopeTitleUpdated")
         * @return Either an error or the EventId
         */
        fun create(eventType: String): Either<EventIdError, EventId> {
            val now = Clock.System.now()

            return when {
                eventType.isBlank() -> EventIdError.EmptyValue(
                    occurredAt = now,
                    field = "eventType"
                ).left()
                !eventType.matches(Regex("^[A-Z][A-Za-z]+$")) -> EventIdError.InvalidEventType(
                    occurredAt = now,
                    attemptedType = eventType,
                    reason = "Event type must be in PascalCase (e.g., ScopeCreated)"
                ).left()
                else -> {
                    try {
                        val ulid = ULID.random()
                        val uri = "$SCHEMA://$NAMESPACE/$eventType/$ulid"
                        EventId(uri).right()
                    } catch (e: Exception) {
                        EventIdError.UlidError(
                            occurredAt = now,
                            reason = "Failed to generate ULID: ${e.message}"
                        ).left()
                    }
                }
            }
        }

        /**
         * Create an EventId from a domain event class.
         * The class simple name will be used as the event type.
         *
         * @param klass The Kotlin class representing the event type
         * @return Either an error or the EventId
         */
        fun <T : Any> create(klass: KClass<T>): Either<EventIdError, EventId> {
            val eventType = klass.simpleName ?: return EventIdError.InvalidEventType(
                occurredAt = Clock.System.now(),
                attemptedType = "<anonymous>",
                reason = "Cannot create EventId from anonymous class"
            ).left()
            return create(eventType)
        }

        /**
         * Create an EventId from a domain event class (Java-friendly version).
         */
        fun <T : Any> create(clazz: Class<T>): Either<EventIdError, EventId> {
            return create(clazz.kotlin)
        }

        /**
         * Parse an EventId from a URI string.
         *
         * @param uri The URI string to parse
         * @return Either an error or the EventId
         */
        fun parse(uri: String): Either<EventIdError, EventId> {
            val now = Clock.System.now()

            return when {
                uri.isBlank() -> EventIdError.EmptyValue(
                    occurredAt = now,
                    field = "uri"
                ).left()
                !uri.startsWith("$SCHEMA://$NAMESPACE/") -> EventIdError.InvalidUriFormat(
                    occurredAt = now,
                    attemptedUri = uri,
                    reason = "URI must start with $SCHEMA://$NAMESPACE/"
                ).left()
                !URI_PATTERN.matches(uri) -> EventIdError.InvalidUriFormat(
                    occurredAt = now,
                    attemptedUri = uri,
                    reason = "Invalid URI format. Expected: evt://scopes/{EventType}/{ULID}"
                ).left()
                else -> {
                    val parts = uri.split("/")
                    if (parts.size != 5) {
                        EventIdError.InvalidUriFormat(
                            occurredAt = now,
                            attemptedUri = uri,
                            reason = "Invalid URI structure. Expected 5 parts, got ${parts.size}"
                        ).left()
                    } else {
                        EventId(uri).right()
                    }
                }
            }
        }
    }
}
