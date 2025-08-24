package io.github.kamiazya.scopes.userpreferences.infrastructure.adapter

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.interfaces.shared.dto.HierarchyPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.dto.UserPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.error.UserPreferencesServiceError
import io.github.kamiazya.scopes.interfaces.shared.services.UserPreferencesService
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import kotlinx.datetime.Clock

class UserPreferencesServiceImpl(private val repository: UserPreferencesRepository, private val logger: Logger, private val clock: Clock = Clock.System) :
    UserPreferencesService {

    override suspend fun getCurrentUserPreferences(): Either<UserPreferencesServiceError, UserPreferencesDto> = either {
        logger.debug("Getting current user preferences")

        val aggregate = repository.findForCurrentUser()
            .mapLeft { domainError ->
                logger.error("Failed to get user preferences: ${domainError.message}")
                when (domainError) {
                    is io.github.kamiazya.scopes.userpreferences.domain.error.UserPreferencesError.InvalidPreferenceValue ->
                        UserPreferencesServiceError.PreferencesCorrupted(
                            details = domainError.message,
                            configPath = null,
                        )
                    else ->
                        UserPreferencesServiceError.PreferencesReadError(
                            cause = domainError.message,
                            configPath = null,
                        )
                }
            }
            .bind()

        // If no preferences exist, create default ones
        val preferences = if (aggregate == null) {
            val (newAggregate, _) = io.github.kamiazya.scopes.userpreferences.domain.aggregate.UserPreferencesAggregate.create(clock = clock)
                .mapLeft { domainError ->
                    logger.error("Failed to create default preferences: ${domainError.message}")
                    UserPreferencesServiceError.PreferencesReadError(
                        cause = domainError.message,
                        configPath = null,
                    )
                }
                .bind()

            repository.save(newAggregate)
                .mapLeft { domainError ->
                    logger.error("Failed to save default preferences: ${domainError.message}")
                    UserPreferencesServiceError.PreferencesWriteError(
                        cause = domainError.message,
                        configPath = null,
                    )
                }
                .bind()

            newAggregate.preferences
                ?: raise(
                    UserPreferencesServiceError.PreferencesCorrupted(
                        details = "Newly created aggregate has no preferences",
                        configPath = null,
                    ),
                )
        } else {
            aggregate.preferences
                ?: raise(
                    UserPreferencesServiceError.PreferencesCorrupted(
                        details = "Aggregate exists but has no preferences",
                        configPath = null,
                    ),
                )
        }

        val dto = UserPreferencesDto(
            hierarchyPreferences = HierarchyPreferencesDto(
                maxDepth = preferences.hierarchyPreferences.maxDepth,
                maxChildrenPerScope = preferences.hierarchyPreferences.maxChildrenPerScope,
            ),
        )

        logger.debug("Successfully retrieved user preferences")
        dto
    }
}
