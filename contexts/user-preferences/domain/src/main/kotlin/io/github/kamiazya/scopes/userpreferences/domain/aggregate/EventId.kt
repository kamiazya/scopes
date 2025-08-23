package io.github.kamiazya.scopes.userpreferences.domain.aggregate

import io.github.kamiazya.scopes.platform.commons.id.ULID

@JvmInline
value class EventId private constructor(val value: String) {
    companion object {
        fun generate(): EventId = EventId(ULID.generate().value)

        fun from(value: String): EventId = EventId(value)
    }
}
