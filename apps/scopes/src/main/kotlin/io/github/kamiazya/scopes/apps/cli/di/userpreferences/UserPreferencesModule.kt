package io.github.kamiazya.scopes.apps.cli.di.userpreferences

import io.github.kamiazya.scopes.interfaces.shared.services.UserPreferencesService
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.application.handler.GetCurrentUserPreferencesHandler
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import io.github.kamiazya.scopes.userpreferences.infrastructure.adapter.UserPreferencesServiceImpl
import io.github.kamiazya.scopes.userpreferences.infrastructure.repository.FileBasedUserPreferencesRepository
import kotlinx.datetime.Clock
import org.koin.dsl.module
import java.nio.file.Path
import java.nio.file.Paths

/**
 * DI module for User Preferences bounded context
 *
 * This module configures:
 * - Repository implementation (FileBasedUserPreferencesRepository)
 * - Application handlers (GetCurrentUserPreferencesHandler)
 * - Service adapter (UserPreferencesServiceImpl)
 */
val userPreferencesModule = module {
    // Repository
    single<UserPreferencesRepository> {
        FileBasedUserPreferencesRepository(
            configPath = getConfigPath(),
            logger = get<Logger>(),
            clock = Clock.System,
        )
    }

    // Application Handlers
    factory {
        GetCurrentUserPreferencesHandler(
            repository = get(),
            clock = Clock.System,
        )
    }

    // Service Adapter
    single<UserPreferencesService> {
        UserPreferencesServiceImpl(
            repository = get(),
            logger = get<Logger>(),
            clock = Clock.System,
        )
    }
}

private fun getConfigPath(): Path {
    val xdgConfigHome = System.getenv("XDG_CONFIG_HOME")
        ?: "${System.getProperty("user.home")}/.config"
    return Paths.get(xdgConfigHome, "scopes")
}
