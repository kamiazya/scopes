package io.github.kamiazya.scopes.apps.cli.di.userpreferences

import io.github.kamiazya.scopes.interfaces.shared.services.UserPreferencesService
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.userpreferences.application.handler.GetCurrentUserPreferencesHandler
import io.github.kamiazya.scopes.userpreferences.domain.repository.UserPreferencesRepository
import io.github.kamiazya.scopes.userpreferences.infrastructure.adapter.UserPreferencesServiceImpl
import io.github.kamiazya.scopes.userpreferences.infrastructure.repository.FileBasedUserPreferencesRepository
import kotlinx.datetime.Clock
import org.koin.dsl.module
import kotlin.io.path.Path

/**
 * DI module for User Preferences bounded context
 *
 * This module configures:
 * - Repository implementation (FileBasedUserPreferencesRepository)
 * - Application handlers (GetCurrentUserPreferencesHandler)
 * - Service adapter (UserPreferencesServiceImpl)
 */
val userPreferencesModule = module {
    // Clock provider
    single<Clock> { Clock.System }

    // Repository
    single<UserPreferencesRepository> {
        FileBasedUserPreferencesRepository(
            configPathStr = getConfigPath(),
            logger = get<Logger>(),
            clock = get<Clock>(),
        )
    }

    // Application Handlers
    factory {
        GetCurrentUserPreferencesHandler(
            repository = get(),
            clock = get<Clock>(),
        )
    }

    // Service Adapter
    single<UserPreferencesService> {
        UserPreferencesServiceImpl(
            repository = get(),
            logger = get<Logger>(),
            clock = get<Clock>(),
        )
    }
}

private fun getConfigPath(): String {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("win") -> {
            // Windows: Use APPDATA or fallback to user.home/.config
            val appData = System.getenv("APPDATA")
                ?: "${System.getProperty("user.home")}/.config"
            Path(appData, "scopes").toString()
        }
        else -> {
            // Unix-like (Linux, macOS): Use XDG_CONFIG_HOME or fallback
            val configHome = System.getenv("XDG_CONFIG_HOME")
                ?: "${System.getProperty("user.home")}/.config"
            Path(configHome, "scopes").toString()
        }
    }
}
