package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.MigrationAwareDatabaseProvider
import io.github.kamiazya.scopes.platform.infrastructure.database.migration.MigrationConfig
import io.github.kamiazya.scopes.platform.infrastructure.transaction.SqlDelightTransactionManager
import io.github.kamiazya.scopes.scopemanagement.db.ScopeManagementDatabase
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.EventSourcingRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.AliasGenerationStrategy
import io.github.kamiazya.scopes.scopemanagement.domain.service.alias.WordProvider
import io.github.kamiazya.scopes.scopemanagement.domain.service.validation.FilterExpressionValidator
import io.github.kamiazya.scopes.scopemanagement.infrastructure.adapters.ErrorMapper
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.migration.ScopeManagementMigrationProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.service.AspectQueryFilterValidator
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
    // Migration configuration
    single<MigrationConfig>(named("scopeManagementMigrationConfig")) {
        MigrationConfig(
            maxRetries = 3,
        )
    }

    // Migration provider
    single(named("scopeManagementMigrationProvider")) {
        { ScopeManagementMigrationProvider(logger = get()).getMigrations() }
    }

    // Migration-aware database provider
    single<MigrationAwareDatabaseProvider<ScopeManagementDatabase>>(named("scopeManagementDatabaseProvider")) {
        val migrationProvider: () -> List<io.github.kamiazya.scopes.platform.infrastructure.database.migration.Migration> =
            get(named("scopeManagementMigrationProvider"))
        val config: MigrationConfig = get(named("scopeManagementMigrationConfig"))
        val logger: io.github.kamiazya.scopes.platform.observability.logging.Logger = get()

        MigrationAwareDatabaseProvider(
            migrationProvider = migrationProvider,
            config = config,
            logger = logger,
            databaseFactory = { driver ->
                // Schema is created by migrations; avoid double-creation here
                ScopeManagementDatabase(driver)
            },
        )
    }

    // SQLDelight Database using the migration-aware provider
    single<ScopeManagementDatabase>(named("scopeManagement")) {
        val databasePath: String = get<String>(named("databasePath"))
        val provider: MigrationAwareDatabaseProvider<ScopeManagementDatabase> = get(named("scopeManagementDatabaseProvider"))

        val dbPath = if (databasePath == ":memory:") {
            ":memory:"
        } else {
            "$databasePath/scope-management.db"
        }

        // Create database with migrations applied
        kotlinx.coroutines.runBlocking {
            provider.createDatabase(dbPath).fold(
                ifLeft = { err ->
                    // Fail-fast using Kotlin's error() function
                    error("Failed to create database: ${err.message}")
                },
                ifRight = { database ->
                    database
                },
            )
        }
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
    single<SqlDelightActiveContextRepository> {
        val database: ScopeManagementDatabase = get(named("scopeManagement"))
        SqlDelightActiveContextRepository(database)
    }
    single<ActiveContextRepository> { get<SqlDelightActiveContextRepository>() }

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

    // Event Sourcing Repository using contracts
    single<EventSourcingRepository<io.github.kamiazya.scopes.platform.domain.event.DomainEvent>> {
        val eventStoreCommandPort: io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort = get()
        val eventStoreQueryPort: io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort = get()
        val logger: io.github.kamiazya.scopes.platform.observability.logging.Logger = get()

        io.github.kamiazya.scopes.scopemanagement.infrastructure.factory.EventSourcingRepositoryFactory.createContractBased(
            eventStoreCommandPort = eventStoreCommandPort,
            eventStoreQueryPort = eventStoreQueryPort,
            logger = logger,
        )
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

    single<io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationBootstrapper>(qualifier = named("ActiveContextBootstrap")) {
        io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap.ActiveContextBootstrap(
            activeContextRepository = get(),
            logger = get(),
        )
    }

    // Note: Schema version repository and migration executor are no longer needed here
    // They are handled internally by MigrationAwareDatabaseProvider

    // Note: Migrations are now automatically run when creating the ManagedSqlDriver
    // No need for separate MigrationManager or DatabaseMigrationBootstrapper
}
