package io.github.kamiazya.scopes.presentation.cli.di

import io.github.kamiazya.scopes.application.service.ApplicationScopeValidationService
import io.github.kamiazya.scopes.application.usecase.CreateScopeUseCase
import io.github.kamiazya.scopes.domain.repository.ScopeRepository
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeRepository
import org.koin.dsl.module

/**
 * Koin dependency injection modules for the CLI application.
 * Composition root pattern - defines all dependency injection configuration.
 * Follows Clean Architecture principles by depending only on abstractions.
 */
val cliModule = module {
    // Infrastructure layer - Repository implementations
    single<ScopeRepository> { InMemoryScopeRepository() }

    // Application layer - Services
    single { ApplicationScopeValidationService(get()) }

    // Application layer - Use cases
    single { CreateScopeUseCase(get(), get()) }
}
