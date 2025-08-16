package io.github.kamiazya.scopes.presentation.cli

import io.github.kamiazya.scopes.application.error.ErrorMessageFormatter
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.service.HaikunatorService
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeAliasRepository
import io.github.kamiazya.scopes.infrastructure.transaction.NoopTransactionManager
import io.github.kamiazya.scopes.presentation.cli.commands.CreateScopeCommand
import io.github.kamiazya.scopes.presentation.cli.error.CliErrorMessageFormatter
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Composition Root for the CLI application.
 * 
 * This is the single place where all dependencies are wired together.
 * It follows the Composition Root pattern from Clean Architecture,
 * where infrastructure dependencies are resolved at the application's entry point.
 * 
 * The CLI presentation layer should only consume DTOs and application interfaces,
 * never domain entities or infrastructure implementations directly.
 */
object CompositionRoot {
    
    /**
     * Initialize the dependency injection container with all required modules.
     * This is idempotent and safe to call multiple times - it only initializes
     * Koin if it's not already running.
     */
    fun initialize() {
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(
                    infrastructureModule,
                    domainModule,
                    applicationModule,
                    presentationModule
                )
            }
        }
    }
    
    /**
     * Infrastructure layer dependencies.
     * Contains concrete implementations of ports/adapters.
     */
    private val infrastructureModule = module {
        single<ScopeRepository> { InMemoryScopeRepository() }
        single<ScopeAliasRepository> { InMemoryScopeAliasRepository() }
        single<TransactionManager> { NoopTransactionManager() }
    }
    
    /**
     * Domain layer dependencies.
     * Domain services that encapsulate business logic.
     */
    private val domainModule = module {
        single { ScopeHierarchyService() }
        single { HaikunatorService() }
        single { ScopeAliasManagementService(get(), get()) }
    }
    
    /**
     * Application layer dependencies.  
     * Contains use case handlers, services, and application-specific implementations.
     */
    private val applicationModule = module {
        single { CrossAggregateValidationService(get()) }
        single { CreateScopeHandler(get(), get(), get(), get(), get()) }
    }
    
    /**
     * Presentation layer dependencies.
     * Should be minimal - mainly configuration and presentation-specific services.
     */
    private val presentationModule = module {
        single<ErrorMessageFormatter> { CliErrorMessageFormatter }
        single { CreateScopeCommand(get(), get()) }
    }
}
