package io.github.kamiazya.scopes.presentation.cli

fun main(args: Array<String>) {
    // Initialize all dependencies through the Composition Root
    CompositionRoot.initialize()
    
    // Start the CLI application
    ScopesCommand().main(args)
}

