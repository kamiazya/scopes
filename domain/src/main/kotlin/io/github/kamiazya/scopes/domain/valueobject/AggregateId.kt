package io.github.kamiazya.scopes.domain.valueobject

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.domain.error.AggregateIdError
import kotlinx.datetime.Clock
import kotlin.reflect.KClass

/**
 * A URI-based aggregate identifier following the Global ID pattern.
 *
 * Format: gid://scopes/{type}/{id}
 * Example: gid://scopes/Scope/01HX3BQXYZ
 *
 * This provides a globally unique, self-describing identifier that includes:
 * - The schema (gid)
 * - The application namespace (scopes)
 * - The aggregate type
 * - The unique identifier (typically ULID)
 */
@JvmInline
value class AggregateId private constructor(val value: String) {

    /**
     * Extract the aggregate type from the URI.
     * For "gid://scopes/Scope/01HX3BQXYZ", returns "Scope"
     */
    val aggregateType: String
        get() = value.split("/")[3]

    /**
     * Extract the ID portion from the URI.
     * For "gid://scopes/Scope/01HX3BQXYZ", returns "01HX3BQXYZ"
     */
    val id: String
        get() = value.split("/")[4]

    /**
     * Returns the full URI string representation.
     */
    override fun toString(): String = value

    companion object {
        private const val SCHEMA = "gid"
        private const val NAMESPACE = "scopes"
        private val URI_PATTERN = Regex("^gid://scopes/[A-Z][A-Za-z]+/[A-Z0-9]+$")

        // Supported aggregate types
        private val VALID_TYPES = setOf(
            "Scope",
            "ScopeAlias",
            "ContextView"
        )

        /**
         * Create an AggregateId from components.
         *
         * @param type The aggregate type (e.g., "Scope", "ScopeAlias")
         * @param id The unique identifier (typically a ULID)
         * @return Either an error or the AggregateId
         */
        fun create(type: String, id: String): Either<AggregateIdError, AggregateId> {
            val now = Clock.System.now()
            return when {
                type.isBlank() -> AggregateIdError.EmptyValue(
                    occurredAt = now,
                    field = "type"
                ).left()
                id.isBlank() -> AggregateIdError.EmptyValue(
                    occurredAt = now,
                    field = "id"
                ).left()
                type !in VALID_TYPES -> AggregateIdError.InvalidType(
                    occurredAt = now,
                    attemptedType = type,
                    validTypes = VALID_TYPES
                ).left()
                !id.matches(Regex("^[A-Z0-9]+$")) -> AggregateIdError.InvalidIdFormat(
                    occurredAt = now,
                    attemptedId = id,
                    expectedFormat = "ULID format (uppercase letters and numbers only)"
                ).left()
                else -> {
                    val uri = "$SCHEMA://$NAMESPACE/$type/$id"
                    AggregateId(uri).right()
                }
            }
        }

        /**
         * Create an AggregateId from a Kotlin class and ID.
         * The class simple name will be used as the aggregate type.
         *
         * @param klass The Kotlin class representing the aggregate type
         * @param id The unique identifier
         * @return Either an error message or the AggregateId
         */
        fun <T : Any> create(klass: KClass<T>, id: String): Either<AggregateIdError, AggregateId> {
            val typeName = klass.simpleName ?: return AggregateIdError.InvalidType(
                occurredAt = Clock.System.now(),
                attemptedType = "<anonymous>",
                validTypes = VALID_TYPES
            ).left()
            return create(typeName, id)
        }

        /**
         * Parse an AggregateId from a URI string.
         *
         * @param uri The URI string to parse
         * @return Either an error message or the AggregateId
         */
        fun parse(uri: String): Either<AggregateIdError, AggregateId> {
            val now = Clock.System.now()
            return when {
                uri.isBlank() -> AggregateIdError.EmptyValue(
                    occurredAt = now,
                    field = "uri"
                ).left()
                !uri.startsWith("$SCHEMA://$NAMESPACE/") -> AggregateIdError.InvalidUriFormat(
                    occurredAt = now,
                    attemptedUri = uri,
                    reason = "URI must start with $SCHEMA://$NAMESPACE/"
                ).left()
                !URI_PATTERN.matches(uri) -> AggregateIdError.InvalidUriFormat(
                    occurredAt = now,
                    attemptedUri = uri,
                    reason = "Invalid URI format. Expected: gid://scopes/{Type}/{ID}"
                ).left()
                else -> {
                    val parts = uri.split("/")
                    if (parts.size != 5) {
                        AggregateIdError.InvalidUriFormat(
                            occurredAt = now,
                            attemptedUri = uri,
                            reason = "Invalid URI structure. Expected 5 parts, got ${parts.size}"
                        ).left()
                    } else {
                        val type = parts[3]
                        if (type !in VALID_TYPES) {
                            AggregateIdError.InvalidType(
                                occurredAt = now,
                                attemptedType = type,
                                validTypes = VALID_TYPES
                            ).left()
                        } else {
                            AggregateId(uri).right()
                        }
                    }
                }
            }
        }

    }
}
