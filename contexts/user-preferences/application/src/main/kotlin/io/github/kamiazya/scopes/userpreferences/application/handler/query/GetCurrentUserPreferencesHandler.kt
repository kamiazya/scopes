package io.github.kamiazya.scopes.userpreferences.application.handler.query

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.QueryHandler
import io.github.kamiazya.scopes.userpreferences.application.dto.UserPreferencesInternalDto
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
class GetCurrentUserPreferencesHandler(private val repository: UserPreferencesRepository) :
    QueryHandler<GetCurrentUserPreferences, UserPreferencesError, UserPreferencesInternalDto> {

    override suspend fun invoke(query: GetCurrentUserPreferences): Either<UserPreferencesError, UserPreferencesInternalDto> = either {
        // Query handlers should only read data, not create or modify state
        val aggregate = repository.findForCurrentUser().bind()
            ?: raise(UserPreferencesError.PreferencesNotInitialized())

        val preferences = aggregate.preferences
            ?: raise(UserPreferencesError.PreferencesNotInitialized())

        UserPreferencesInternalDto.from(preferences)
    }
}
