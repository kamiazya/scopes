package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.platform.infrastructure.transaction.SqlDelightTransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.WordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction.TransactionManagerAdapter
import org.koin.core.qualifier.named
import org.koin.dsl.module
import io.github.kamiazya.scopes.platform.application.port.TransactionManager as PlatformTransactionManager

/**
 * Koin module for Scope Management infrastructure
 *
 * This module provides concrete implementations for:
 * - SQLDelight database
 * - Repositories
 * - Transaction managers
 * - Alias generation services
 * - Default services following Zero-Configuration principle
 */
val scopeManagementInfrastructureModule = module {
    // SQLDelight Database
    single<ScopeManagementDatabase>(named("scopeManagement")) {
        val databasePath: String = get<String>(named("databasePath"))
        SqlDelightDatabaseProvider.createDatabase("$databasePath/scope-management.db")
    }

    // Repository implementations - mix of SQLDelight and legacy SQLite
    single<ScopeRepository> {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        SqlDelightScopeRepository(database)
    }
    // TODO: Migrate these to SQLDelight when needed
    single<ScopeAliasRepository> {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        SqlDelightScopeAliasRepository(database)
    }
    single<ContextViewRepository> {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        SqlDelightContextViewRepository(database)
    }
    single<AspectDefinitionRepository> {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        SqlDelightAspectDefinitionRepository(database)
    }

    // Platform TransactionManager for this bounded context
    single<PlatformTransactionManager>(named("scopeManagement")) {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        SqlDelightTransactionManager(database)
    }

    // Transaction Manager Adapter
    single<TransactionManager> {
        val platformTxManager: PlatformTransactionManager = get(named("scopeManagement"))
        TransactionManagerAdapter(platformTxManager)
    }

    // Alias Generation
    single<WordProvider> { DefaultWordProvider() }
    single<AliasGenerationStrategy> { HaikunatorStrategy() }
    single<AliasGenerationService> {
        DefaultAliasGenerationService(get(), get())
    }

    // External Services are now provided by their own modules
    // UserPreferencesService is provided by UserPreferencesModule
}
