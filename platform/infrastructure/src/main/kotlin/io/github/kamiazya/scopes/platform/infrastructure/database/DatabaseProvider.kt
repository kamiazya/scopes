package io.github.kamiazya.scopes.platform.infrastructure.database

/**
 * @deprecated Use databaseModule with Koin dependency injection
 */
@Deprecated(
    "Use databaseModule with Koin dependency injection",
    ReplaceWith("databaseModule", "io.github.kamiazya.scopes.apps.cli.di.platform.databaseModule"),
)
object DatabaseProvider
