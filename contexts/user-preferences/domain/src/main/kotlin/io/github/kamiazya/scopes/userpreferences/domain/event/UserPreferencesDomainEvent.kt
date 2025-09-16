package io.github.kamiazya.scopes.userpreferences.domain.event

import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
sealed interface UserPreferencesDomainEvent : DomainEvent

data class UserPreferencesCreated(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    val preferences: UserPreferences,
) : UserPreferencesDomainEvent

data class PreferencesReset(
    override val eventId: EventId,
    override val aggregateId: AggregateId,
    override val aggregateVersion: AggregateVersion,
    val oldPreferences: UserPreferences,
    val newPreferences: UserPreferences,
) : UserPreferencesDomainEvent
