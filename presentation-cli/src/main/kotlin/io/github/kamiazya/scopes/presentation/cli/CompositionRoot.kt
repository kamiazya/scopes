package io.github.kamiazya.scopes.presentation.cli

import io.github.kamiazya.scopes.application.error.ErrorMessageFormatter
import io.github.kamiazya.scopes.application.port.Logger
import io.github.kamiazya.scopes.application.port.TransactionManager
import io.github.kamiazya.scopes.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.domain.repository.ScopeAliasRepository
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.domain.service.WordProvider
import io.github.kamiazya.scopes.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.infrastructure.logger.ConsoleLogger
import io.github.kamiazya.scopes.infrastructure.coroutine.CoroutineLoggingContextScope
import io.github.kamiazya.scopes.application.port.loggingContextScope
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeAliasRepository
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeRepository
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
            // Configure logging context for coroutine-based context propagation
            loggingContextScope = CoroutineLoggingContextScope()
            
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

        // Alias generation infrastructure
        single { HaikunatorStrategy() }
        single<WordProvider> { DefaultWordProvider() }
        single<AliasGenerationService> {
            DefaultAliasGenerationService(get<HaikunatorStrategy>(), get())
        }
    }

    /**
     * Domain layer dependencies.
     * Domain services that encapsulate business logic.
     */
    private val domainModule = module {
        single { ScopeHierarchyService() }
        single { ScopeAliasManagementService(get(), get()) }
    }

    /**
     * Application layer dependencies.
     * Contains use case handlers, services, and application-specific implementations.
     */
    private val applicationModule = module {
        single<Logger> { ConsoleLogger("ScopesApp") }
        single { CrossAggregateValidationService(get()) }
        single {
            CreateScopeHandler(
                scopeRepository = get(),
                transactionManager = get(),
                hierarchyService = get(),
                crossAggregateValidationService = get(),
                aliasManagementService = get(),
                logger = get()
            )
        }
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
