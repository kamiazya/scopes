package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

class UseCaseArchitectureTest : StringSpec({

    "application layer should only depend on domain" {
        Konsist
            .scopeFromModule("application")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    val importName = import.name
                    importName.contains("infrastructure") || 
                    importName.contains("presentation") ||
                    importName.contains("javax.persistence") || 
                    importName.contains("org.springframework.data")
                }
            }
    }

    "presentation module should not import domain classes except abstractions" {
        Konsist
            .scopeFromModule("presentation-cli")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("io.github.kamiazya.scopes.domain.") &&
                    // Allow repository interfaces for DI configuration
                    !import.name.contains(".repository.")
                }
            }
    }

    "application DTO classes should not import domain types" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".dto") == true }
            .assertFalse { dtoClass ->
                // Check all imports in files containing DTO classes
                dtoClass.containingFile.imports.any { import ->
                    import.name.startsWith("io.github.kamiazya.scopes.domain.")
                }
            }
    }

    "commands in command package should implement Command interface" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".command") == true }
            .assertTrue { it.hasParentWithName("Command") }
    }

    "classes in query package should follow Query naming convention" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".usecase.query") == true }
            .filter { it.name != "Query" } // Exclude the Query interface itself
            .assertTrue { queryClass ->
                // All classes in query package should end with Query or be query types
                queryClass.name.endsWith("Query") || 
                queryClass.name.contains("Query")
            }
    }

    "UseCase interface should be functional interface" {
        Konsist
            .scopeFromModule("application")
            .interfaces()
            .filter { it.name == "UseCase" }
            .assertTrue { useCase ->
                useCase.hasFunModifier
            }
    }

    "handler classes should end with Handler and be in handler package" {
        Konsist
            .scopeFromModule("application") 
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertTrue { handler ->
                // Handlers should be in the .usecase.handler package
                val packageName = handler.packagee?.name ?: ""
                packageName.endsWith(".usecase.handler")
            }
    }

    "classes in handler package should follow Handler naming convention" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".usecase.handler") == true }
            .assertTrue { handler ->
                // All classes in handler package should end with "Handler"
                handler.name.endsWith("Handler")
            }
    }
})
