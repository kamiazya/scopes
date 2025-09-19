package io.github.kamiazya.scopes.apps.cli.test

import io.github.kamiazya.scopes.scopemanagement.domain.entity.Scope
import io.github.kamiazya.scopes.scopemanagement.domain.entity.ScopeAlias
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AliasName
import kotlinx.datetime.Clock

/**
 * Helper for creating test data with consistent patterns.
 * Reduces duplication in test setup code.
 */
object TestDataHelper {
    
    /**
     * Creates a scope with a canonical alias and saves both to their respective repositories.
     * This is a common pattern in tests that was duplicated across multiple test files.
     */
    suspend fun createScopeWithCanonicalAlias(
        scope: Scope,
        aliasName: String,
        scopeRepository: ScopeRepository,
        aliasRepository: ScopeAliasRepository,
        timestamp: kotlinx.datetime.Instant = Clock.System.now()
    ): ScopeAlias {
        // Save the scope
        scopeRepository.save(scope)
        
        // Create and save canonical alias
        val alias = ScopeAlias.createCanonical(
            scopeId = scope.id,
            aliasName = AliasName.create(aliasName).getOrNull()!!,
            timestamp = timestamp
        )
        aliasRepository.save(alias)
        
        return alias
    }
    
    /**
     * Creates multiple scopes with canonical aliases in a single operation.
     * Useful for test setup that requires multiple scopes.
     */
    suspend fun createMultipleScopesWithAliases(
        scopesWithAliases: List<Pair<Scope, String>>,
        scopeRepository: ScopeRepository,
        aliasRepository: ScopeAliasRepository,
        timestamp: kotlinx.datetime.Instant = Clock.System.now()
    ): List<ScopeAlias> {
        return scopesWithAliases.map { (scope, aliasName) ->
            createScopeWithCanonicalAlias(
                scope = scope,
                aliasName = aliasName,
                scopeRepository = scopeRepository,
                aliasRepository = aliasRepository,
                timestamp = timestamp
            )
        }
    }
}