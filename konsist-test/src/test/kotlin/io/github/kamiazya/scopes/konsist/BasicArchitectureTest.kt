package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

class BasicArchitectureTest : StringSpec({
    
    "domain module should not import application layer" {
        Konsist
            .scopeFromModule("domain")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains("application")
                }
            }
    }
    
    "domain module should not import infrastructure layer" {
        Konsist
            .scopeFromModule("domain")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains("infrastructure")
                }
            }
    }
    
    "domain module should not import presentation layer" {
        Konsist
            .scopeFromModule("domain")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains("presentation")
                }
            }
    }
    
    "application module should not import infrastructure layer" {
        Konsist
            .scopeFromModule("application")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains("infrastructure")
                }
            }
    }
    
    "application module should not import presentation layer" {
        Konsist
            .scopeFromModule("application")
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.contains("presentation")
                }
            }
    }
    
    "classes should have PascalCase names" {
        Konsist
            .scopeFromProduction()
            .classes()
            .assertTrue { clazz ->
                clazz.name.first().isUpperCase()
            }
    }
    
    "package names should be lowercase" {
        Konsist
            .scopeFromProduction()
            .packages
            .assertTrue { pkg ->
                pkg.name.lowercase() == pkg.name
            }
    }
    
    "repository interfaces should end with Repository" {
        Konsist
            .scopeFromModule("domain")
            .interfaces()
            .filter { it.packagee?.name?.endsWith(".repository") == true }
            .assertTrue { repository ->
                repository.name.endsWith("Repository")
            }
    }
    
    "classes in 'usecase' package should end with UseCase" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".usecase") == true }
            .filter { !it.name.endsWith("Test") }
            .filter { !it.name.endsWith("Request") }
            .filter { !it.name.endsWith("Response") }
            .assertTrue { useCase ->
                useCase.name.endsWith("UseCase")
            }
    }
    
    "data classes should use val properties" {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.hasDataModifier }
            .assertTrue { dataClass ->
                dataClass.properties().all { property ->
                    property.hasValModifier
                }
            }
    }
})
