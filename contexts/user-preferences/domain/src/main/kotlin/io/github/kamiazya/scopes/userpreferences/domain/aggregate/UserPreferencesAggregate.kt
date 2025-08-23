package io.github.kamiazya.scopes.userpreferences.domain.aggregate

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.event.PreferenceRemoved
import io.github.kamiazya.scopes.userpreferences.domain.event.PreferenceSet
import io.github.kamiazya.scopes.userpreferences.domain.event.PreferencesReset
import io.github.kamiazya.scopes.userpreferences.domain.event.UserPreferencesCreated
import io.github.kamiazya.scopes.userpreferences.domain.event.UserPreferencesDomainEvent
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceKey
import io.github.kamiazya.scopes.userpreferences.domain.value.PreferenceValue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class UserPreferencesAggregate(
    override val id: AggregateId,
    override val version: AggregateVersion,
    val preferences: UserPreferences?,
    val createdAt: Instant,
    val updatedAt: Instant,
) : AggregateRoot<UserPreferencesAggregate>() {

    companion object {
        fun create(
            aggregateId: AggregateId = AggregateId.generate(),
            clock: Clock = Clock.System,
        ): Either<UserPreferencesError, Pair<UserPreferencesAggregate, DomainEvent>> {
            val now = clock.now()
            val preferences = UserPreferences.createDefault(now)

            val event = UserPreferencesCreated(
                eventId = EventId.generate(),
                aggregateId = aggregateId,
                occurredAt = now,
                preferences = preferences,
            )

            val aggregate = UserPreferencesAggregate(
                id = aggregateId,
                version = AggregateVersion.initial(),
                preferences = preferences,
                createdAt = now,
                updatedAt = now,
            )

            return (aggregate to event).right()
        }
    }

    fun setPreference(
        key: PreferenceKey,
        value: PreferenceValue,
        clock: Clock = Clock.System,
    ): Either<UserPreferencesError, Pair<UserPreferencesAggregate, DomainEvent>> = either {
        val currentPreferences = ensureInitialized().bind()
        val oldValue = currentPreferences.getPreference(key)

        val event = PreferenceSet(
            eventId = EventId.generate(),
            aggregateId = id,
            occurredAt = clock.now(),
            key = key,
            oldValue = oldValue,
            newValue = value,
        )

        val updated = copy(
            preferences = currentPreferences.setPreference(key, value, clock.now()),
            version = version.increment(),
            updatedAt = clock.now(),
        )

        updated to event
    }

    fun removePreference(key: PreferenceKey, clock: Clock = Clock.System): Either<UserPreferencesError, Pair<UserPreferencesAggregate, DomainEvent>> = either {
        val currentPreferences = ensureInitialized().bind()
        val oldValue = currentPreferences.getPreference(key)
            ?: raise(UserPreferencesError.PreferenceNotFound(key.value))

        val event = PreferenceRemoved(
            eventId = EventId.generate(),
            aggregateId = id,
            occurredAt = clock.now(),
            key = key,
            oldValue = oldValue,
        )

        val updatedPreferences = currentPreferences.removePreference(key, clock.now()).bind()
        val updated = copy(
            preferences = updatedPreferences,
            version = version.increment(),
            updatedAt = clock.now(),
        )

        updated to event
    }

    fun resetToDefaults(clock: Clock = Clock.System): Either<UserPreferencesError, Pair<UserPreferencesAggregate, DomainEvent>> = either {
        val currentPreferences = ensureInitialized().bind()
        val newPreferences = UserPreferences.createDefault(clock.now())

        val event = PreferencesReset(
            eventId = EventId.generate(),
            aggregateId = id,
            occurredAt = clock.now(),
            oldPreferences = currentPreferences,
            newPreferences = newPreferences,
        )

        val updated = copy(
            preferences = newPreferences,
            version = version.increment(),
            updatedAt = clock.now(),
        )

        updated to event
    }

    private fun ensureInitialized(): Either<UserPreferencesError, UserPreferences> =
        preferences?.right() ?: UserPreferencesError.PreferencesNotInitialized().left()

    override fun applyEvent(event: UserPreferencesDomainEvent): UserPreferencesAggregate = when (event) {
        is UserPreferencesCreated -> copy(
            preferences = event.preferences,
            version = version.increment(),
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
        )
        is PreferenceSet -> copy(
            preferences = preferences?.setPreference(event.key, event.newValue, event.occurredAt),
            version = version.increment(),
            updatedAt = event.occurredAt,
        )
        is PreferenceRemoved -> copy(
            preferences = preferences?.removePreference(event.key, event.occurredAt)?.getOrNull(),
            version = version.increment(),
            updatedAt = event.occurredAt,
        )
        is PreferencesReset -> copy(
            preferences = event.newPreferences,
            version = version.increment(),
            updatedAt = event.occurredAt,
        )
    }
}
