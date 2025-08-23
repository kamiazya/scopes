package io.github.kamiazya.scopes.apps.cli.services

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.interfaces.shared.dto.HierarchySettingsDto
import io.github.kamiazya.scopes.interfaces.shared.dto.UserPreferencesDto
import io.github.kamiazya.scopes.interfaces.shared.error.UserPreferencesServiceError
import io.github.kamiazya.scopes.interfaces.shared.services.UserPreferencesService

/**
 * Default implementation of UserPreferencesService following Zero-Configuration principle.
 *
 * This implementation provides sensible defaults that allow users to start using Scopes
 * immediately without any configuration. The default values are unlimited (null) for both
 * hierarchy depth and children per scope, providing maximum flexibility out of the box.
 *
 * This is not just a "mock" for testing - it's the actual default implementation that
 * enables the zero-configuration experience. Users can override these defaults through
 * actual preference files, but they are never required to do so.
 */
class DefaultUserPreferencesService(
    private val maxDepth: Int? = null, // null means unlimited
    private val maxChildren: Int? = null, // null means unlimited
) : UserPreferencesService {

    override suspend fun getCurrentUserPreferences(): Either<UserPreferencesServiceError, UserPreferencesDto> = UserPreferencesDto(
        hierarchySettings = HierarchySettingsDto(
            maxDepth = maxDepth, // null means unlimited - sensible default for flexibility
            maxChildrenPerScope = maxChildren, // null means unlimited - no artificial restrictions
        ),
    ).right()
}
