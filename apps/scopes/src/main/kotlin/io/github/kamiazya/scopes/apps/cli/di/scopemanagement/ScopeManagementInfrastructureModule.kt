package io.github.kamiazya.scopes.apps.cli.di.scopemanagement

import io.github.kamiazya.scopes.contracts.eventstore.EventStoreCommandPort
import io.github.kamiazya.scopes.contracts.eventstore.EventStoreQueryPort
import io.github.kamiazya.scopes.platform.application.lifecycle.ApplicationBootstrapper
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.platform.domain.event.DomainEvent
import io.github.kamiazya.scopes.platform.infrastructure.transaction.SqlDelightTransactionManager
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.scopemanagement.application.port.EventPublisher
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
import io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap.ActiveContextBootstrap
import io.github.kamiazya.scopes.scopemanagement.infrastructure.bootstrap.AspectPresetBootstrap
import io.github.kamiazya.scopes.scopemanagement.infrastructure.factory.EventSourcingRepositoryFactory
import io.github.kamiazya.scopes.scopemanagement.infrastructure.projection.EventProjectionService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightActiveContextRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightAspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightContextViewRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.SqlDelightScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.service.AspectQueryFilterValidator
import io.github.kamiazya.scopes.scopemanagement.infrastructure.sqldelight.SqlDelightDatabaseProvider
import kotlinx.serialization.modules.SerializersModule
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

    // Event Projector for RDB projection
    single<EventPublisher> {
        EventProjectionService(
            scopeRepository = get(),
            scopeAliasRepository = get(),
            logger = get(),
        )
    }

    // Event Sourcing Repository using contracts
    single<EventSourcingRepository<DomainEvent>> {
        val eventStoreCommandPort: EventStoreCommandPort = get()
        val eventStoreQueryPort: EventStoreQueryPort = get()
        val logger: Logger = get()
        val serializersModule: SerializersModule? = getOrNull()

        EventSourcingRepositoryFactory.createContractBased(
            eventStoreCommandPort = eventStoreCommandPort,
            eventStoreQueryPort = eventStoreQueryPort,
            logger = logger,
            serializersModule = serializersModule,
        )
    }

    // External Services are now provided by their own modules
    // UserPreferencesService is provided by UserPreferencesModule

    // Bootstrap services - registered as ApplicationBootstrapper for lifecycle management
    single<ApplicationBootstrapper>(qualifier = named("AspectPresetBootstrap")) {
        AspectPresetBootstrap(
            aspectDefinitionRepository = get(),
            logger = get(),
        )
    }

    single<ApplicationBootstrapper>(qualifier = named("ActiveContextBootstrap")) {
        ActiveContextBootstrap(
            activeContextRepository = get(),
            logger = get(),
        )
    }
}
