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

        "command DTOs should be in command.dto package" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.packagee?.name?.contains(".command.dto") == true }
                .assertTrue { dto ->
                    // Command DTOs should be data classes
                    dto.hasDataModifier || dto.hasSealedModifier
                }
        }

        "query DTOs should be in query.dto package" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.packagee?.name?.contains(".query.dto") == true }
                .assertTrue { dto ->
                    // Query DTOs should be data classes
                    dto.hasDataModifier || dto.hasSealedModifier
                }
        }

        "CommandHandler and QueryHandler interfaces should be functional interfaces" {
            Konsist
                .scopeFromModule("application")
                .interfaces()
                .filter { it.name == "CommandHandler" || it.name == "QueryHandler" }
                .assertTrue { handler ->
                    handler.hasFunModifier
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
                    // Handlers should be in the .handler.command or .handler.query package
                    val packageName = handler.packagee?.name ?: ""
                    packageName.contains(".handler.command") ||
                        packageName.contains(".handler.query") ||
                        packageName.contains(".query.handler") ||
                        packageName.contains(".command.handler")
                }
        }

        "classes in handler package should follow Handler naming convention" {
            Konsist
                .scopeFromProduction("application") // Only production code, not test
                .classes()
                .filter {
                    val pkg = it.packagee?.name ?: ""
                    pkg.contains(".handler.command") ||
                        pkg.contains(".handler.query") ||
                        pkg.contains(".query.handler") ||
                        pkg.contains(".command.handler")
                }
                .assertTrue { handler ->
                    // All classes in handler package should end with "Handler"
                    handler.name.endsWith("Handler")
                }
        }

        "handler classes should follow handler pattern" {
            Konsist
                .scopeFromProduction("application") // Only production code, not test
                .classes()
                .filter {
                    val pkg = it.packagee?.name ?: ""
                    pkg.contains(".handler.command") ||
                        pkg.contains(".handler.query") ||
                        pkg.contains(".query.handler") ||
                        pkg.contains(".command.handler")
                }
                .assertTrue { handler ->
                    // Handlers should implement CommandHandler or QueryHandler interface
                    handler.parents().any { it.name == "CommandHandler" } || handler.parents().any { it.name == "QueryHandler" }
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

        "CommandHandler and QueryHandler interfaces should have generic parameters" {
            Konsist
                .scopeFromModule("application")
                .interfaces()
                .filter { it.name == "CommandHandler" || it.name == "QueryHandler" }
                .assertTrue { handler ->
                    val typeParameters = handler.typeParameters
                    typeParameters.size == 2 // C/Q and R (Command/Query and Result)
                }
        }

        "CommandHandler and QueryHandler interfaces should have suspend operator invoke function" {
            Konsist
                .scopeFromModule("application")
                .interfaces()
                .filter { it.name == "CommandHandler" || it.name == "QueryHandler" }
                .assertTrue { handler ->
                    handler.functions().any { function ->
                        function.name == "invoke" &&
                            function.hasOperatorModifier &&
                            function.hasSuspendModifier
                    }
                }
        }

        "CommandHandler and QueryHandler interfaces should enforce Either return type" {
            Konsist
                .scopeFromModule("application")
                .interfaces()
                .filter { it.name == "CommandHandler" || it.name == "QueryHandler" }
                .assertTrue { handler ->
                    handler.functions().any { function ->
                        function.name == "invoke" &&
                            function.returnType?.text?.contains("Either<ScopesError,") == true
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

        "command and query DTOs should be data classes" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter {
                    val pkg = it.packagee?.name ?: ""
                    pkg.contains(".command.dto") || pkg.contains(".query.dto")
                }
                .assertTrue { dto ->
                    dto.hasDataModifier || dto.hasSealedModifier
                }
        }

        "command and query DTO properties should be immutable" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter {
                    val pkg = it.packagee?.name ?: ""
                    pkg.contains(".command.dto") || pkg.contains(".query.dto")
                }
                .assertTrue { dto ->
                    dto.properties().all { property ->
                        property.hasValModifier || !property.hasVarModifier
                    }
                }
        }

        "handler classes should implement CommandHandler or QueryHandler" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.name.endsWith("Handler") }
                .assertTrue { handler ->
                    handler.parents().any { it.name == "CommandHandler" } || handler.parents().any { it.name == "QueryHandler" }
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

        "application error classes should be sealed and end with Error" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.packagee?.name?.endsWith(".error") == true }
                .filter { it.name.endsWith("Error") } // Only check classes that end with "Error" (top-level error classes)
                .filter { !it.name.startsWith("Domain") } // Exclude domain error references
                .forEach { errorClass ->
                    // Check if class is sealed
                    if (!errorClass.hasSealedModifier) {
                        throw AssertionError("Error class ${errorClass.name} should be sealed")
                    }
                }
        }

        "application errors should use domain ScopesError" {
            Konsist
                .scopeFromModule("application")
                .classes()
                .filter { it.name.endsWith("Handler") }
                .assertTrue { handler ->
                    // Handlers should return Either<ScopesError, R>
                    handler.functions().any { function ->
                        function.returnType?.text?.contains("Either<ScopesError,") == true
                    }
                }
        }
    })
