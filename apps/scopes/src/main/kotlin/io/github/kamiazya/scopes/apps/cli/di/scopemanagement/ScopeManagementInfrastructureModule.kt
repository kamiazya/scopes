package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.infrastructure.transaction.SqlDelightTransactionManager
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.WordProvider
import io.github.kamiazya.scopes.scopemanagement.domain.service.validation.FilterExpressionValidator
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ErrorMapper
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.EventStoreErrorMapper
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.ActiveContextRepositoryImpl
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.service.AspectQueryFilterValidator
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import org.koin.core.qualifier.named
import org.koin.dsl.module

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
        val dbPath = if (databasePath == ":memory:") {
            ":memory:"
        } else {
            "$databasePath/scope-management.db"
        }
        SqlDelightDatabaseProvider.createDatabase(dbPath)
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

    // Active Context Repository
    single<ActiveContextRepository> {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        ActiveContextRepositoryImpl(database)
    }

    // TransactionManager for this bounded context
    single<TransactionManager> {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        SqlDelightTransactionManager(database)
    }

    // Alias Generation
    single<WordProvider> { DefaultWordProvider() }
    single<AliasGenerationStrategy> { HaikunatorStrategy() }
    single<AliasGenerationService> {
        DefaultAliasGenerationService(get(), get())
    }

    // Domain service implementations
    single<FilterExpressionValidator> {
        AspectQueryFilterValidator(aspectQueryParser = get())
    }

    // Error mappers
    single<ErrorMapper> {
        ErrorMapper(logger = get())
    }

    single<ApplicationErrorMapper> {
        ApplicationErrorMapper(logger = get())
    }

    single<EventStoreErrorMapper> {
        EventStoreErrorMapper(logger = get())
    }

    // External Services are now provided by their own modules
    // UserPreferencesService is provided by UserPreferencesModule

    // Bootstrap services - registered as ApplicationBootstrapper for lifecycle management
    single<io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationBootstrapper>(qualifier = named("AspectPresetBootstrap")) {
        io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap.AspectPresetBootstrap(
            aspectDefinitionRepository = get(),
            logger = get(),
        )
    }
}
