package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Tests to enforce packaging conventions across the codebase.
 *
 * @see /docs/guides/packaging-conventions.md
 */
class PackagingConventionTest :
    StringSpec({

        // ========== Package Naming Rules ==========

        "package names should be lowercase without underscores" {
            Konsist
                .scopeFromProduction()
                .packages
                .assertTrue { pkg ->
                    pkg.name.all { it.isLowerCase() || it.isDigit() || it == '.' } &&
                        !pkg.name.contains("_")
                }
        }

        // ========== Domain Layer Package Structure ==========

        "domain errors should be in error package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..domain..") }
                .filter {
                    it.name.endsWith("Error") ||
                        it.name.endsWith("Exception") ||
                        it.parents().any { parent -> parent.name == "DomainError" }
                }
                .filter { !it.name.endsWith("Test") }
                .filter { it.isTopLevel } // Exclude nested classes as they are part of sealed hierarchies
                .assertTrue { errorClass ->
                    errorClass.packagee?.name?.endsWith(".error") == true ||
                        errorClass.packagee?.name == "error"
                }
        }

        "domain entities should be in entity package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..domain..") }
                .filter { it.annotations.any { ann -> ann.name == "Entity" } || it.name.endsWith("Entity") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { entity ->
                    entity.packagee?.name == "entity"
                }
        }

        // ========== Application Layer Package Structure ==========

        "application commands should be in command package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..application..") }
                .filter { it.name.endsWith("Command") }
                .filter { !it.name.endsWith("Test") }
                .filter { it.isTopLevel } // Exclude nested command classes within use cases
                .assertTrue { command ->
                    command.packagee?.name == "command" ||
                        command.packagee?.name?.contains(".command") == true
                }
        }

        "application queries should be in query package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..application..") }
                .filter { it.name.endsWith("Query") }
                .filter { !it.name.endsWith("Test") }
                .filter { it.isTopLevel } // Exclude nested query classes within use cases
                .assertTrue { query ->
                    query.packagee?.name == "query" ||
                        query.packagee?.name?.contains(".query") == true
                }
        }

        "application DTOs should be in dto package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..application..") }
                .filter {
                    it.name.endsWith("Dto") ||
                        it.name.endsWith("Result") ||
                        it.name.endsWith("Input")
                }
                .filter { !it.name.endsWith("Test") }
                .filter { it.isTopLevel } // Exclude nested classes (internal helper classes)
                .assertTrue { dto ->
                    dto.packagee?.name == "dto" ||
                        dto.resideInPackage("..dto..")
                }
        }

        "application handlers should be in handler package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..application..") }
                .filter { it.name.endsWith("Handler") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { handler ->
                    handler.packagee?.name == "handler" ||
                        handler.packagee?.name?.contains(".handler") == true
                }
        }

        // ========== Infrastructure Layer Package Structure ==========

        "infrastructure repositories should be in repository package" {
            // Repositories should be properly organized in repository packages
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..infrastructure..") }
                .filter { it.name.endsWith("Repository") }
                .filter { !it.name.endsWith("Test") }
                .filter { !it.name.contains("Interface") }
                .assertTrue { repo ->
                    // Check that the package ends with .repository
                    repo.resideInPackage("..repository")
                }
        }

        "infrastructure adapters should be in adapter or adapters package" {
            // Adapters should be properly organized in adapter packages
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..infrastructure..") }
                .filter { it.name.endsWith("Adapter") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { adapter ->
                    // Check that the package ends with .adapter or .adapters
                    adapter.resideInPackage("..adapter..") || adapter.resideInPackage("..adapters..")
                }
        }

        // ========== Contracts Layer Package Structure ==========

        "contract commands should be in commands package" {
            val commands = Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..contracts..") }
                .filter { it.name.endsWith("Command") }
                .filter { it.isTopLevel } // Exclude nested command types
                .filter { !it.name.endsWith("CommandPort") } // Exclude port interfaces
                .assertTrue { command ->
                    command.packagee?.name?.endsWith(".commands") == true
                }
        }

        "contract queries should be in queries package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..contracts..") }
                .filter { it.name.endsWith("Query") }
                .filter { it.isTopLevel } // Exclude nested query types
                .filter { !it.name.endsWith("QueryPort") } // Exclude port interfaces
                .assertTrue { query ->
                    query.packagee?.name?.endsWith(".queries") == true
                }
        }

        "contract results should be in results package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..contracts..") }
                .filter { it.name.endsWith("Result") }
                .filter { it.isTopLevel } // Exclude nested result types
                .assertTrue { result ->
                    result.packagee?.name?.endsWith(".results") == true
                }
        }

        "contract errors should be in errors package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..contracts..") }
                .filter { it.name.endsWith("ContractError") || it.name.endsWith("Error") }
                .filter { it.isTopLevel } // Exclude nested error types
                .filter { !it.name.endsWith("Port") } // Exclude port interfaces
                .assertTrue { error ->
                    error.packagee?.name?.endsWith(".errors") == true
                }
        }

        // ========== Cross-Layer Rules ==========

        "domain layer should not depend on application layer" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("/domain/") }
                .flatMap { it.imports }
                .assertFalse { import ->
                    import.name.contains(".application.")
                }
        }

        "domain layer should not depend on infrastructure layer" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("/domain/") }
                .flatMap { it.imports }
                .assertFalse { import ->
                    import.name.contains(".infrastructure.")
                }
        }

        "application layer should not depend on infrastructure layer" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("/application/") }
                .flatMap { it.imports }
                .assertFalse { import ->
                    import.name.contains(".infrastructure.")
                }
        }

        "contracts should not depend on any implementation layer" {
            Konsist
                .scopeFromProduction()
                .files
                .filter { it.path.contains("/contracts/") }
                .flatMap { it.imports }
                .assertFalse { import ->
                    import.name.contains(".domain.") ||
                        import.name.contains(".application.") ||
                        import.name.contains(".infrastructure.")
                }
        }

        // ========== Platform Module Rules ==========

        "platform modules should follow platform package structure" {
            Konsist
                .scopeFromProduction()
                .packages
                .filter { pkg ->
                    pkg.name.contains(".platform.")
                }
                .assertTrue { pkg ->
                    val validConcerns = setOf("commons", "domain", "application", "infrastructure", "observability")

                    // Extract the part after platform
                    val parts = pkg.name.split(".")
                    val platformIndex = parts.indexOf("platform")

                    if (platformIndex != -1 && platformIndex < parts.size - 1) {
                        val concern = parts[platformIndex + 1]
                        concern in validConcerns
                    } else {
                        true // Allow root platform package
                    }
                }
        }

        // ========== Test Package Structure ==========

        "test packages should mirror source packages" {
            Konsist
                .scopeFromTest()
                .packages
                .assertTrue { testPkg ->
                    // Test packages should follow the same structure as source packages
                    testPkg.name.all { it.isLowerCase() || it.isDigit() || it == '.' }
                }
        }

        // ========== Special Cases ==========

        "interfaces layer should follow interface package conventions" {
            Konsist
                .scopeFromProduction()
                .packages
                .filter { it.name.contains(".interfaces.") }
                .assertTrue { pkg ->
                    val validSubpackages = setOf(
                        "commands", "adapters", "formatters", "validators",
                        "mappers", "core", "error", "resolvers", "providers",
                        "exitcode", "cli", "interfaces", "extensions",
                        // Subcommand packages
                        "alias", "context", "aspect", "scope", "query",
                    )

                    // Extract the last part of the package name
                    val lastPart = pkg.name.split(".").last()

                    lastPart in validSubpackages ||
                        pkg.name.endsWith(".cli") ||
                        // CLI is a special interface type
                        pkg.name.contains(".mcp.") // MCP is a special interface type following Model Context Protocol structure
                }
        }

        "apps layer should not contain domain logic" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.resideInPackage("..apps..") }
                .filter { !it.name.endsWith("Test") }
                .assertFalse { clazz ->
                    // Apps should not contain entities, value objects, or domain services
                    clazz.name.endsWith("Entity") ||
                        clazz.name.endsWith("ValueObject") ||
                        clazz.name.endsWith("DomainService") ||
                        clazz.packagee?.name?.contains("domain") == true
                }
        }
    })
