package io.github.kamiazya.scopes.presentation.cli

import io.github.kamiazya.scopes.presentation.cli.di.cliModule
import org.koin.core.context.startKoin

fun main(args: Array<String>) {
    startKoin {
        modules(cliModule)
    }

    ScopesCommand().main(args)
}

