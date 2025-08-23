package io.github.kamiazya.scopes.userpreferences.domain.aggregate

import io.github.kamiazya.scopes.platform.commons.id.ULID

@JvmInline
value class AggregateId private constructor(val value: String) {
    companion object {
        fun generate(): AggregateId = AggregateId(ULID.generate().value)

        fun from(value: String): AggregateId = AggregateId(value)
    }
}
