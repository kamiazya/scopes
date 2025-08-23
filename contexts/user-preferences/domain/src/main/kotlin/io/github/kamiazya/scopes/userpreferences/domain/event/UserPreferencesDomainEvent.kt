package io.github.kamiazya.scopes.userpreferences.domain.event

import io.github.kamiazya.scopes.userpreferences.domain.aggregate.AggregateId
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.DomainEvent
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.EventId
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceKey
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceValue
import kotlinx.datetime.Instant

sealed interface UserPreferencesDomainEvent : DomainEvent

data class UserPreferencesCreated(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val occurredAt: Instant,
    val preferences: UserPreferences,
) : UserPreferencesDomainEvent

data class PreferenceSet(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val occurredAt: Instant,
    val key: PreferenceKey,
    val oldValue: PreferenceValue?,
    val newValue: PreferenceValue,
) : UserPreferencesDomainEvent

data class PreferenceRemoved(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val occurredAt: Instant,
    val key: PreferenceKey,
    val oldValue: PreferenceValue,
) : UserPreferencesDomainEvent

data class PreferencesReset(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val occurredAt: Instant,
    val oldPreferences: UserPreferences,
    val newPreferences: UserPreferences,
) : UserPreferencesDomainEvent
