package io.github.kamiazya.scopes.userpreferences.application.handler

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.usecase.UseCase
import io.github.kamiazya.scopes.userpreferences.application.dto.UserPreferencesInternalDto
import io.github.kamiazya.scopes.userpreferences.application.query.GetCurrentUserPreferences
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
class GetCurrentUserPreferencesHandler(private val repository: UserPreferencesRepository) :
    UseCase<GetCurrentUserPreferences, UserPreferencesError, UserPreferencesInternalDto> {

    override suspend fun invoke(query: GetCurrentUserPreferences): Either<UserPreferencesError, UserPreferencesInternalDto> = either {
        val aggregate = repository.findForCurrentUser().bind()
            ?: createDefaultPreferences().bind()

        val preferences = aggregate.preferences
            ?: raise(UserPreferencesError.PreferencesNotInitialized())

        UserPreferencesInternalDto.from(preferences)
    }

    private suspend fun createDefaultPreferences(): Either<UserPreferencesError, UserPreferencesAggregate> = UserPreferencesAggregate.create()
        .flatMap { (aggregate, _) ->
            repository.save(aggregate).map { aggregate }
        }
}
