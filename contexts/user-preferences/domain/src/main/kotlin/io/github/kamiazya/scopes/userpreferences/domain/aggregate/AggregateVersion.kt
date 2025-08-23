package io.github.kamiazya.scopes.userpreferences.domain.aggregate

@JvmInline
value class AggregateVersion private constructor(val value: Long) {
    fun increment(): AggregateVersion = AggregateVersion(value + 1)

    companion object {
        fun initial(): AggregateVersion = AggregateVersion(0)

        fun from(value: Long): AggregateVersion = AggregateVersion(value)
    }
}
