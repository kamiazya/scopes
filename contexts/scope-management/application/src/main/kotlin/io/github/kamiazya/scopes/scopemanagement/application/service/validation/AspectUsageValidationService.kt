package io.github.kamiazya.scopes.scopemanagement.application.service.validation

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import kotlinx.datetime.Clock

/**
 * Service for validating aspect usage in scopes.
 * This service checks if an aspect is being used by any scopes
 * before allowing operations like deletion.
 */
class AspectUsageValidationService(private val scopeRepository: ScopeRepository) {
    /**
     * Ensures that an aspect is not in use by any scopes.
     * Returns an error if the aspect is being used.
     */
    suspend fun ensureNotInUse(aspectKey: AspectKey): Either<ScopesError, Unit> = either {
        // Check if any scopes are using this aspect
        val count = scopeRepository.countByAspectKey(aspectKey).bind()

        ensure(count == 0) {
            ScopesError.Conflict(
                resourceType = "AspectDefinition",
                resourceId = aspectKey.value,
                conflictType = ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES,
                details = mapOf("usage_count" to count),
                occurredAt = Clock.System.now(),
            )
        }
    }
}
