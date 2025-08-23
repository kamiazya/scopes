package io.github.kamiazya.scopes.interfaces.shared.services

import arrow.core.Either
import io.github.kamiazya.scopes.interfaces.shared.dto.UserPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.error.UserPreferencesServiceError

/**
 * External service interface for user preferences.
 *
 * Simplified to focus only on hierarchy-related preferences.
 *
 * DESIGN PRINCIPLE: Zero-Configuration Start
 * Implementations MUST return default preferences when no configuration exists,
 * rather than returning an error. Users should be able to start using the tool
 * immediately without any setup. The system should never fail due to missing
 * preferences - always provide sensible defaults.
 */
interface UserPreferencesService {
    /**
     * Retrieves the current user's hierarchy preferences.
     *
     * IMPORTANT: If preferences don't exist, return default values wrapped in Right(),
     * not an error. Only return Left() for actual failures (corrupted data, I/O errors).
     *
     * @return Either an error or the user preferences with hierarchy settings
     */
    suspend fun getCurrentUserPreferences(): Either<UserPreferencesServiceError, UserPreferencesDto>
}
