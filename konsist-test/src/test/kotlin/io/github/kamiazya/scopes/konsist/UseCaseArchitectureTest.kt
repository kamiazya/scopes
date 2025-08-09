package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.StringSpec

class UseCaseArchitectureTest : StringSpec({

    "handlers should end with Handler" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".handler") == true }
            .assertTrue { handler ->
                handler.name.endsWith("Handler")
            }
    }

    "commands should be in command package" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".command") == true }
            .assertTrue { command ->
                command.name.isNotEmpty()
            }
    }

    "application layer should only depend on domain" {
        Konsist
            .scopeFromModule("application")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    val importName = import.name
                    importName.contains("infrastructure") || 
                    importName.contains("presentation") ||
                    (importName.contains("javax.persistence") || 
                     importName.contains("org.springframework.data"))
                }
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
})