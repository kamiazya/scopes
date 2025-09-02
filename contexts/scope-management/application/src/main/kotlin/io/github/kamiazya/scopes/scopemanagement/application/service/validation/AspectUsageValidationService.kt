package io.github.kamiazya.scopes.scopemanagement.application.service.validation

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey

/**
 * Service for validating aspect usage in scopes.
 * This service checks if an aspect is being used by any scopes
 * before allowing operations like deletion.
 */
class AspectUsageValidationService(
    private val scopeRepository: ScopeRepository,
) {
    /**
     * Ensures that an aspect is not in use by any scopes.
     * Returns an error if the aspect is being used.
     */
    suspend fun ensureNotInUse(aspectKey: AspectKey): Either<ScopesError, Unit> = either {
        // Check if any scopes are using this aspect
        val count = scopeRepository.countByAspectKey(aspectKey).bind()
        
        if (count > 0) {
            raise(
                ScopesError.Conflict(
                    "Cannot delete aspect '${aspectKey.value}' because it is in use by $count scope(s)"
                )
            )
        }
    }
}