package io.github.kamiazya.scopes.scopemanagement.domain.valueobject

import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError

/**
 * Result of alias operation determination.
 */
sealed class AliasOperation {
    data class Create(val alias: ScopeAlias) : AliasOperation()
    data class Replace(val oldAlias: ScopeAlias, val newAlias: ScopeAlias) : AliasOperation()
    data class Promote(val existingAlias: ScopeAlias) : AliasOperation()
    data class NoChange(val reason: String) : AliasOperation()
    data class Failure(val error: ScopesError) : AliasOperation()
}

/**
 * Result of conflict resolution.
 */
data class ConflictResolution(val toCreate: List<AliasName>, val alreadyExists: List<AliasName>, val toKeep: List<ScopeAlias>)
