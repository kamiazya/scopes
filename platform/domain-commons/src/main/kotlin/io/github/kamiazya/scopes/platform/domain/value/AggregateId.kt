package io.github.kamiazya.scopes.platform.domain.value

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.platform.commons.id.ULID
import io.github.kamiazya.scopes.platform.domain.error.DomainError
import io.github.kamiazya.scopes.platform.domain.error.currentTimestamp

/**
 * Aggregate identifier for domain entities.
 *
 * This supports two styles:
 * 1. Simple ULID-based IDs (default)
 * 2. URI-based Global IDs (for advanced scenarios)
 */
sealed interface AggregateId {
    val value: String

    /**
     * Simple ULID-based aggregate ID.
     * Format: 26-character ULID string
     * Example: "01HX3BQXYZ123456789ABCDEF"
     */
    @JvmInline
    value class Simple private constructor(override val value: String) : AggregateId {
        companion object {
            fun generate(): Simple = Simple(ULID.generate().value)

            fun from(value: String): Either<DomainError.InvalidId, Simple> = when {
                value.isEmpty() -> DomainError.InvalidId(
                    value = value,
                    errorType = DomainError.InvalidId.InvalidIdType.EMPTY,
                    occurredAt = currentTimestamp(),
                ).left()
                !ULID.isValid(value) -> DomainError.InvalidId(
                    value = value,
                    errorType = DomainError.InvalidId.InvalidIdType.INVALID_FORMAT,
                    occurredAt = currentTimestamp(),
                ).left()
                else -> Simple(value.uppercase()).right() // Normalize to uppercase like ULID
            }

            fun fromUnsafe(value: String): Simple = Simple(value)
        }
    }

    /**
     * URI-based Global ID following the Global ID pattern.
     * Format: gid://namespace/type/id
     * Example: "gid://scopes/Scope/01HX3BQXYZ"
     */
    @JvmInline
    value class Uri private constructor(override val value: String) : AggregateId {
        val schema: String
            get() = value.split("://")[0]

        val namespace: String
            get() = value.split("://")[1].split("/")[0]

        val aggregateType: String
            get() = value.split("/")[3]

        val id: String
            get() = value.split("/")[4]

        companion object {
            private const val DEFAULT_SCHEMA = "gid"
            private const val DEFAULT_NAMESPACE = "scopes"

            // Pattern for URI format, but we'll separately validate the ULID part
            private val URI_PATTERN = Regex("^[a-z]+://[a-z]+/[A-Z][A-Za-z]+/[0-9A-HJKMNP-TV-Z]{26}$")

            fun generate(aggregateType: String, schema: String = DEFAULT_SCHEMA, namespace: String = DEFAULT_NAMESPACE): Either<DomainError.InvalidId, Uri> =
                create(
                    aggregateType = aggregateType,
                    id = ULID.generate().value,
                    schema = schema,
                    namespace = namespace,
                )

            fun create(
                aggregateType: String,
                id: String,
                schema: String = DEFAULT_SCHEMA,
                namespace: String = DEFAULT_NAMESPACE,
            ): Either<DomainError.InvalidId, Uri> {
                // Validate the ULID part using common validation
                if (!ULID.isValid(id)) {
                    return DomainError.InvalidId(
                        value = id,
                        errorType = DomainError.InvalidId.InvalidIdType.INVALID_FORMAT,
                        occurredAt = currentTimestamp(),
                    ).left()
                }
                val normalizedId = id.uppercase() // Normalize ULID to uppercase
                val uri = "$schema://$namespace/$aggregateType/$normalizedId"
                return from(uri)
            }

            fun from(value: String): Either<DomainError.InvalidId, Uri> = when {
                value.isEmpty() -> DomainError.InvalidId(
                    value = value,
                    errorType = DomainError.InvalidId.InvalidIdType.EMPTY,
                    occurredAt = currentTimestamp(),
                ).left()
                !URI_PATTERN.matches(value) -> DomainError.InvalidId(
                    value = value,
                    errorType = DomainError.InvalidId.InvalidIdType.INVALID_FORMAT,
                    occurredAt = currentTimestamp(),
                ).left()
                else -> Uri(value).right()
            }

            fun fromUnsafe(value: String): Uri = Uri(value)
        }
    }

    companion object {
        /**
         * Generate a simple ULID-based ID (default).
         */
        fun generate(): Simple = Simple.generate()

        /**
         * Parse an ID string, automatically detecting the format.
         */
        fun from(value: String): Either<DomainError.InvalidId, AggregateId> = when {
            value.contains("://") -> Uri.from(value)
            else -> Simple.from(value)
        }
    }
}
