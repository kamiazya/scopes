package io.github.kamiazya.scopes.userpreferences.domain.aggregate

import io.github.kamiazya.scopes.userpreferences.domain.event.UserPreferencesDomainEvent

abstract class AggregateRoot<T : AggregateRoot<T>> {
    abstract val id: AggregateId
    abstract val version: AggregateVersion

    abstract fun applyEvent(event: UserPreferencesDomainEvent): T
}
