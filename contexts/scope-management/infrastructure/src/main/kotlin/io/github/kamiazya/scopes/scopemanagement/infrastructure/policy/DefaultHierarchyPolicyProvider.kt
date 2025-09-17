package io.github.kamiazya.scopes.scopemanagement.infrastructure.policy

import arrow.core.Either
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.port.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.HierarchyPolicy

/**
 * Default implementation that always returns the default hierarchy policy.
 *
 * This can be used as a fallback when the external user preferences
 * service is not available or not configured. It ensures the system
 * can always function with reasonable defaults.
 */
class DefaultHierarchyPolicyProvider : HierarchyPolicyProvider {

    override suspend fun getPolicy(): Either<ScopeManagementApplicationError, HierarchyPolicy> = HierarchyPolicy.default().right()
}
