package io.github.kamiazya.scopes.scopemanagement.application.port

import arrow.core.Either
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy

/**
 * Port interface for providing hierarchy policy configuration.
 *
 * This interface acts as an Anti-Corruption Layer between the Scope Management
 * context and external contexts (e.g., User Preferences).
 */
interface HierarchyPolicyProvider {
    /**
     * Retrieves the current hierarchy policy configuration.
     *
     * @return Either an error or the current HierarchyPolicy
     */
    suspend fun getPolicy(): Either<ScopeManagementApplicationError, HierarchyPolicy>
}
