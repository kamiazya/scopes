package io.github.kamiazya.scopes.userpreferences.application.handler.query

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.userpreferences.application.dto.UserPreferencesInternalDto
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
class GetCurrentUserPreferencesHandler(private val repository: UserPreferencesRepository) :
    QueryHandler<GetCurrentUserPreferences, UserPreferencesError, UserPreferencesInternalDto> {

    override suspend fun invoke(query: GetCurrentUserPreferences): Either<UserPreferencesError, UserPreferencesInternalDto> = either {
        val aggregate = repository.findForCurrentUser().bind()
            ?: createDefaultPreferences().bind()

        val preferences = aggregate.preferences
            ?: raise(UserPreferencesError.PreferencesNotInitialized())

        UserPreferencesInternalDto.from(preferences)
    }

    private suspend fun createDefaultPreferences(): Either<UserPreferencesError, UserPreferencesAggregate> = either {
        // Create the aggregate with default preferences
        val (aggregate, _) = UserPreferencesAggregate.create().bind()

        // Try to save the new aggregate
        repository.save(aggregate).fold(
            { error ->
                // If PreferencesAlreadyInitialized, reload from repository
                if (error is UserPreferencesError.PreferencesAlreadyInitialized) {
                    // Another process/thread created preferences, reload them
                    val reloadedAggregate = repository.findForCurrentUser().bind()
                    if (reloadedAggregate != null) {
                        reloadedAggregate
                    } else {
                        raise(UserPreferencesError.PreferencesNotInitialized())
                    }
                } else {
                    raise(error)
                }
            },
            { aggregate },
        )
    }
}
