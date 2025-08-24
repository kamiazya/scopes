package io.github.kamiazya.scopes.userpreferences.domain.repository

import arrow.core.Either
import io.github.kamiazya.scopes.platform.domain.value.AggregateId
import io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate
import io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError

interface UserPreferencesRepository {
    suspend fun save(aggregate: UserPreferencesAggregate): Either<UserPreferencesError, Unit>
    suspend fun findById(id: AggregateId): Either<UserPreferencesError, UserPreferencesAggregate?>
    suspend fun findForCurrentUser(): Either<UserPreferencesError, UserPreferencesAggregate?>
}
