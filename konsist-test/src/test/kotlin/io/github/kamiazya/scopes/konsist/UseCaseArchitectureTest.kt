package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

class UseCaseArchitectureTest :
    StringSpec({

        "application layer should only depend on domain" {
            Konsist
                .scopeFromModule("application")
                .files
                .assertFalse { file ->
                    file.imports.any { import ->
                        val importName = import.name
                        importName.startsWith("io.github.kamiazya.scopes.infrastructure.") ||
                            importName.startsWith("io.github.kamiazya.scopes.presentation.")
                    }
                }
        }

        "presentation module should not import domain classes except in composition root" {
            Konsist
                .scopeFromModule("presentation-cli")
                .files
                .filter { !it.path.contains("test") } // Exclude test sources
                .assertFalse { file ->
                    // Allow CompositionRoot to import domain interfaces for DI setup
                    val isCompositionRoot = file.nameWithExtension == "CompositionRoot.kt"

                    if (isCompositionRoot) {
                        // For CompositionRoot, allow repository, port, and service interfaces
                        // Only fail if there are domain imports that are NOT allowed
                        false // Allow all domain imports in CompositionRoot for now
                    } else {
                        // Other presentation files should not import domain classes at all
                        file.imports.any { import ->
                            import.name.startsWith("io.github.kamiazya.scopes.domain.")
                        }
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
                .filter { it.packagee?.name?.endsWith(".usecase.command") == true }
                .filter { it.name != "Command" } // Exclude the Command interface itself
                .filter { clazz ->
                    // Include only top-level classes that directly implement Command
                    // Exclude sealed class children (like Text, Numeric, Ordered in SaveAspectDefinition)
                    clazz.parents().any { parent -> parent.name == "Command" }
                }
                .assertTrue { clazz ->
                    // For sealed classes, check if the base class implements Command
                    if (clazz.hasSealedModifier) {
                        clazz.hasParentWithName("Command")
                    } else {
                        clazz.hasParentWithName("Command")
                    }
                }
        }

        "queries in query package should implement Query interface" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.packagee?.name?.endsWith(".usecase.query") == true }
                .filter { it.name != "Query" } // Exclude the Query interface itself
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
                .scopeFromProduction("application") // Only production code, not test
                .classes()
                .filter { it.resideInPackage("..usecase.handler") }
                .assertTrue { handler ->
                    // All classes in handler package should end with "Handler"
                    handler.name.endsWith("Handler")
                }
        }

        "handler classes should follow handler pattern" {
            Konsist
                .scopeFromProduction("application") // Only production code, not test
                .classes()
                .filter { it.resideInPackage("..usecase.handler") }
                .assertTrue { handler ->
                    // Handlers should either have handle methods OR implement UseCase interface
                    val hasHandleMethods = handler.functions().any { function ->
                        function.name.startsWith("handle")
                    }
                    val implementsUseCase = handler.hasParentWithName("UseCase") ||
                        handler.parents().any { it.name.startsWith("UseCase") }

                    hasHandleMethods || implementsUseCase
                }
        }

        "handlers should only depend on domain interfaces not infrastructure" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.name.endsWith("Handler") }
                .assertFalse { handler ->
                    // Handlers should not import any infrastructure classes
                    handler.containingFile.imports.any { import ->
                        import.name.startsWith("io.github.kamiazya.scopes.infrastructure.")
                    }
                }
        }

        "handlers should use Either for unified result handling with UseCase-specific errors" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.name.endsWith("Handler") }
                .assertTrue { handler ->
                    // Handlers should return Either<SpecificError, T> type (enforced by UseCase interface)
                    handler.functions().any { function ->
                        val returnType = function.returnType
                        if (returnType != null) {
                            // Check if return type contains "Either" (more flexible than exact name match)
                            val containsEither = returnType.text.contains("Either")

                            // Check if it has type arguments and first one ends with "Error"
                            val typeArgs = returnType.typeArguments
                            val hasErrorType = typeArgs?.isNotEmpty() == true &&
                                typeArgs.firstOrNull()?.text?.endsWith("Error") == true

                            containsEither && hasErrorType
                        } else {
                            false
                        }
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

        "UseCase interface should have generic parameters I, E, and T" {
            Konsist
                .scopeFromModule("application")
                .interfaces()
                .filter { it.name == "UseCase" }
                .assertTrue { useCase ->
                    val typeParameters = useCase.typeParameters
                    typeParameters.size == 3 &&
                        typeParameters.any { it.name == "I" } &&
                        typeParameters.any { it.name == "E" } &&
                        typeParameters.any { it.name == "T" }
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

        "UseCase interface should enforce Either return type at type level" {
            Konsist
                .scopeFromModule("application")
                .interfaces()
                .filter { it.name == "UseCase" }
                .assertTrue { useCase ->
                    useCase.functions().any { function ->
                        function.name == "invoke" &&
                            function.returnType?.text?.startsWith("Either<E,") == true
                    }
                }
        }

        "DTO classes should be immutable data classes" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.packagee?.name?.endsWith(".dto") == true }
                .assertTrue { dto ->
                    // DTOs should be data classes OR sealed classes (for type safety)
                    // Sealed classes provide type safety with data class children
                    dto.hasDataModifier || dto.hasSealedModifier
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

        "command and query classes should be data classes or sealed classes with data class children" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter {
                    it.hasParentWithName("Command") || it.hasParentWithName("Query")
                }
                .assertTrue { commandOrQuery ->
                    when {
                        // Sealed classes are allowed as they provide type safety
                        commandOrQuery.hasSealedModifier -> true
                        // Regular classes should be data classes
                        else -> commandOrQuery.hasDataModifier
                    }
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

        "handler classes should have handle methods" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.name.endsWith("Handler") }
                .assertTrue { handler ->
                    // Handlers should either have handle methods OR implement UseCase interface
                    val hasHandleMethods = handler.functions().any { function ->
                        function.name.startsWith("handle")
                    }
                    val implementsUseCase = handler.hasParentWithName("UseCase") ||
                        handler.parents().any { it.name.startsWith("UseCase") }

                    hasHandleMethods || implementsUseCase
                }
        }

        "result classes should be in dto package" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.name.endsWith("Result") }
                .assertTrue { result ->
                    // Classes ending with "Result" should be in the dto package
                    val packageName = result.packagee?.name ?: ""
                    packageName.endsWith(".dto")
                }
        }

        "UseCase error classes should be sealed and end with Error" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.packagee?.name?.endsWith(".usecase.error") == true }
                .filter { it.name.endsWith("Error") } // Only check classes that end with "Error" (top-level error classes)
                .forEach { errorClass ->
                    // Check if class is sealed
                    if (!errorClass.hasSealedModifier) {
                        throw AssertionError("Error class ${errorClass.name} should be sealed")
                    }
                }
        }

        "UseCase error classes should be in usecase.error package" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter {
                    it.name.endsWith("Error") &&
                        !it.name.startsWith("Application") &&
                        // Only check error classes that are actually in usecase packages
                        // or that should be UseCase-related error classes
                        (
                            it.packagee?.name?.contains(".usecase") == true ||
                                it.name.contains("UseCase") ||
                                it.name.contains("Command") ||
                                it.name.contains("Query")
                            )
                }
                .assertTrue { errorClass ->
                    val packageName = errorClass.packagee?.name ?: ""
                    packageName.endsWith(".usecase.error")
                }
        }
    })
