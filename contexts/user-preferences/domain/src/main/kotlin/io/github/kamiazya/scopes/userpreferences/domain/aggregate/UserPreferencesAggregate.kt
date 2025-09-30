package io.github.kamiazya.scopes.userpreferences.domain.aggregate

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import io.github.kamiazya.scopes.platform.domain.aggregate.AggregateRoot
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.platform.domain.value.AggregateVersion
import io.github.kamiazya.scopes.platform.domain.value.EventId
import io.github.kamiazya.scopes.userpreferences.domain.entity.UserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.event.PreferencesReset
import io.github.kamiazya.scopes.userpreferences.domain.event.UserPreferencesCreated
import io.github.kamiazya.scopes.userpreferences.domain.event.UserPreferencesDomainEvent
import kotlinx.datetime.Instant

data class UserPreferencesAggregate(
    private val _id: AggregateId,
    override val version: AggregateVersion,
    val preferences: UserPreferences?,
    val createdAt: Instant,
    val updatedAt: Instant,
) : AggregateRoot<UserPreferencesAggregate, AggregateId, UserPreferencesDomainEvent>() {

    override fun getId(): AggregateId = _id

    companion object {
        fun create(
            aggregateId: AggregateId = AggregateId.Simple.generate(),
            now: Instant,
        ): Either<UserPreferencesError, Pair<UserPreferencesAggregate, UserPreferencesDomainEvent>> {
            val preferences = UserPreferences.createDefault(now)

            val initialVersion = AggregateVersion.initial()
            val event = UserPreferencesCreated(
                eventId = EventId.generate(),
                aggregateId = aggregateId,
                aggregateVersion = initialVersion.increment(),
                occurredAt = now,
                preferences = preferences,
            )

            val aggregate = UserPreferencesAggregate(
                _id = aggregateId,
                version = AggregateVersion.initial(),
                preferences = preferences,
                createdAt = now,
                updatedAt = now,
            )

            return (aggregate to event).right()
        }
    }

    fun resetToDefaults(now: Instant): Either<UserPreferencesError, Pair<UserPreferencesAggregate, UserPreferencesDomainEvent>> = either {
        val currentPreferences = ensureInitialized().bind()
        val newPreferences = UserPreferences.createDefault(now)

        val event = PreferencesReset(
            eventId = EventId.generate(),
            aggregateId = getId(),
            aggregateVersion = version.increment(),
            occurredAt = now,
            oldPreferences = currentPreferences,
            newPreferences = newPreferences,
        )

        val updated = copy(
            preferences = newPreferences,
            version = version.increment(),
            updatedAt = now,
        )

        updated to event
    }

    private fun ensureInitialized(): Either<UserPreferencesError, UserPreferences> =
        preferences?.right() ?: UserPreferencesError.PreferencesNotInitialized.left()

    override fun applyEvent(event: UserPreferencesDomainEvent): UserPreferencesAggregate = when (event) {
        is UserPreferencesCreated -> copy(
            preferences = event.preferences,
            version = event.aggregateVersion,
            createdAt = event.occurredAt,
            updatedAt = event.occurredAt,
        )
        is PreferencesReset -> copy(
            preferences = event.newPreferences,
            version = event.aggregateVersion,
            updatedAt = event.occurredAt,
        )
    }
}
