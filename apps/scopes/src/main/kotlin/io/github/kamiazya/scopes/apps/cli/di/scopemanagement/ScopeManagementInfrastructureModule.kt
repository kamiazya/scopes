package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.WordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction.NoopTransactionManager
import org.koin.dsl.module

/**
 * Koin module for Scope Management infrastructure
 *
 * This module provides concrete implementations for:
 * - Repositories
 * - Transaction managers
 * - External integrations
 * - Alias generation services
 * - Default services following Zero-Configuration principle
 */
val scopeManagementInfrastructureModule = module {
    // Repositories
    single<ScopeRepository> { InMemoryScopeRepository() }
    single<ScopeAliasRepository> { InMemoryScopeAliasRepository() }
    single<io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository> {
        io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryContextViewRepository()
    }
    single<AspectDefinitionRepository> { InMemoryAspectDefinitionRepository() }

    // Transaction Management
    single<TransactionManager> { NoopTransactionManager() }

    // Alias Generation
    single<WordProvider> { DefaultWordProvider() }
    single<AliasGenerationStrategy> { HaikunatorStrategy() }
    single<AliasGenerationService> {
        DefaultAliasGenerationService(get(), get())
    }

    // External Services are now provided by their own modules
    // UserPreferencesService is provided by UserPreferencesModule
}
