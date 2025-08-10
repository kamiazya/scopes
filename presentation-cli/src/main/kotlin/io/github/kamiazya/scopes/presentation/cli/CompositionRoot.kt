package io.github.kamiazya.scopes.presentation.cli

import io.github.kamiazya.scopes.application.error.AppErrorTranslator
import io.github.kamiazya.scopes.application.error.DefaultAppErrorTranslator
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.infrastructure.transaction.NoopTransactionManager
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
     * This should be called once at application startup.
     */
    fun initialize() {
        startKoin {
            modules(
                infrastructureModule,
                applicationModule,
                presentationModule
            )
        }
    }
    
    /**
     * Infrastructure layer dependencies.
     * Contains concrete implementations of ports/adapters.
     */
    private val infrastructureModule = module {
        single<ScopeRepository> { InMemoryScopeRepository() }
        single<TransactionManager> { NoopTransactionManager() }
    }
    
    /**
     * Application layer dependencies.  
     * Contains use case handlers, services, and application-specific implementations.
     */
    private val applicationModule = module {
        single { ApplicationScopeValidationService(get()) }
        single<AppErrorTranslator> { DefaultAppErrorTranslator() }
        single { CreateScopeHandler(get(), get()) }
    }
    
    /**
     * Presentation layer dependencies.
     * Should be minimal - mainly configuration and presentation-specific services.
     */
    private val presentationModule = module {
        // Add presentation-specific dependencies here if needed
        // For now, everything is handled by application and infrastructure layers
    }
}