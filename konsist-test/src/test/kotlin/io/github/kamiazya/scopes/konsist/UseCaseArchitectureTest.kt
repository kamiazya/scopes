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
                    importName.contains("presentation")
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

    "queries in query package should implement Query interface" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".query") == true }
            .assertTrue { it.hasParentWithName("Query") }
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

    "Koin modules should be defined in di packages" {
        Konsist
            .scopeFromProduction()
            .properties()
            .filter { it.name.endsWith("Module") }
            .filter { it.type?.name == "Module" }
            .assertTrue { module ->
                val packageName = module.containingFile.packagee?.name ?: ""
                packageName.endsWith(".di")
            }
    }

    "Koin module properties should use val modifier" {
        Konsist
            .scopeFromProduction()
            .properties()
            .filter { it.name.endsWith("Module") }
            .filter { it.type?.name == "Module" }
            .assertTrue { module ->
                module.hasValModifier || !module.hasVarModifier
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

    // TODO: Fix UseCase interface implementation check
    // "handler classes should implement UseCase interface" {
    //     Konsist
    //         .scopeFromModule("application")
    //         .classes()
    //         .filter { it.name.endsWith("Handler") }
    //         .assertTrue { handler ->
    //             // Handlers should implement UseCase<I, O> interface
    //             handler.parents().any { parent -> parent.name == "UseCase" }
    //         }
    // }

    "queries in query package should implement Query interface" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".query") == true }
            .filter { it.name != "Query" }
            .assertTrue { it.hasParentWithName("Query") }
    }

    "handlers should only depend on domain interfaces not infrastructure" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertFalse { handler ->
                // Handlers should not import infrastructure classes
                handler.containingFile.imports.any { import ->
                    import.name.contains("infrastructure") &&
                    !import.name.endsWith(".repository.") // Allow repository interfaces
                }
            }
    }

    "handlers should use Either for error handling" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertTrue { handler ->
                // Handlers should return Either<Error, Result> type
                handler.functions().any { function ->
                    function.returnType?.text?.contains("Either") == true
                }
            }
    }

    "mappers should be in mapper package" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Mapper") }
            .assertTrue { mapper ->
                val packageName = mapper.packagee?.name ?: ""
                packageName.endsWith(".mapper")
            }
    }

    "classes in mapper package should end with Mapper" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".mapper") == true }
            .assertTrue { mapper ->
                mapper.name.endsWith("Mapper")
            }
    }

    "UseCase interface should have generic parameters I and O" {
        Konsist
            .scopeFromModule("application")
            .interfaces()
            .filter { it.name == "UseCase" }
            .assertTrue { useCase ->
                val typeParameters = useCase.typeParameters
                typeParameters.size == 2 && 
                typeParameters.any { it.name == "I" } &&
                typeParameters.any { it.name == "O" }
            }
    }

    "UseCase interface should have suspend operator invoke function" {
        Konsist
            .scopeFromModule("application")
            .interfaces()
            .filter { it.name == "UseCase" }
            .assertTrue { useCase ->
                useCase.functions().any { function ->
                    function.name == "invoke" &&
                    function.hasOperatorModifier &&
                    function.hasSuspendModifier
                }
            }
    }

    "DTO classes should be immutable data classes" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".dto") == true }
            .assertTrue { dto ->
                // DTOs should be data classes
                dto.hasDataModifier
            }
    }

    "DTO properties should use val modifier for immutability" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.packagee?.name?.endsWith(".dto") == true }
            .assertTrue { dto ->
                // All DTO properties should be immutable (val)
                dto.properties().all { property ->
                    property.hasValModifier || !property.hasVarModifier
                }
            }
    }

    "command and query classes should be data classes" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { 
                it.hasParentWithName("Command") || it.hasParentWithName("Query")
            }
            .assertTrue { commandOrQuery ->
                commandOrQuery.hasDataModifier
            }
    }

    "command and query properties should be immutable" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { 
                it.hasParentWithName("Command") || it.hasParentWithName("Query")
            }
            .assertTrue { commandOrQuery ->
                commandOrQuery.properties().all { property ->
                    property.hasValModifier || !property.hasVarModifier
                }
            }
    }

    "handler classes should have invoke function" {
        Konsist
            .scopeFromModule("application")
            .classes()
            .filter { it.name.endsWith("Handler") }
            .assertTrue { handler ->
                // Handlers should have invoke function (from UseCase interface)
                handler.functions().any { function ->
                    function.name == "invoke"
                }
            }
    }
})
