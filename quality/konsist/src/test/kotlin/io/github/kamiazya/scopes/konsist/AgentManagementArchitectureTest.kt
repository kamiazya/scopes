package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Architecture tests for Agent Management bounded context.
 *
 * This test ensures that the agent-management context follows:
 * - Clean Architecture principles with proper layer separation
 * - Domain-Driven Design (DDD) patterns and boundaries
 * - Functional programming patterns with Arrow Either
 * - Strong typing with value objects instead of raw strings
 * - Proper error hierarchy and handling patterns
 *
 * Key architectural principles enforced:
 * 1. Domain layer is pure - no external dependencies
 * 2. Application layer orchestrates domain logic without infrastructure concerns
 * 3. Infrastructure layer implements adapters and technical concerns
 * 4. Proper separation between bounded contexts
 * 5. Agent lifecycle management patterns
 */
class AgentManagementArchitectureTest :
    StringSpec({

        val contextPath = "contexts/agent-management"

        // ========== Layer Dependency Rules ==========

        "agent-management domain should not depend on application or infrastructure layers" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains(".application.") ||
                            import.name.contains(".infrastructure.") ||
                            import.name.contains(".apps.") ||
                            import.name.contains(".boot.") ||
                            // Also check for framework dependencies
                            import.name.contains("org.springframework") ||
                            import.name.contains("jakarta.") ||
                            import.name.contains("javax.") ||
                            import.name.contains("org.koin") ||
                            import.name.contains("kotlinx.serialization.Serializable") ||
                            // Database dependencies
                            import.name.contains("sqldelight") ||
                            import.name.contains("jdbc") ||
                            import.name.contains("sql")
                    }
                }
        }

        "agent-management application should not depend on infrastructure layer" {
            Konsist
                .scopeFromDirectory("$contextPath/application")
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains(".infrastructure.") ||
                            import.name.contains(".boot.") ||
                            import.name.contains("sqldelight")
                    }
                }
        }

        // ========== Domain Purity Rules ==========

        "domain entities should be immutable" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.resideInPackage("..entity..") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { entity ->
                    // All entities should be data classes
                    entity.hasDataModifier &&
                        // All properties should be val (immutable)
                        entity.properties().all { prop ->
                            prop.isVal
                        }
                }
        }

        "domain services should not exist in agent-management" {
            // Agent management is a simple CRUD context, should not have complex domain services
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.name.endsWith("Service") }
                .filter { it.resideInPackage("..service..") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { _ ->
                    false // Should not have any domain services
                }
        }

        // ========== Value Objects and Strong Typing ==========

        "AgentId should be a value class" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.name == "AgentId" }
                .assertTrue { agentId ->
                    agentId.hasValueModifier
                }
        }

        "value objects should be in valueobject package" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter {
                    it.name == "AgentId" ||
                        it.name == "AgentName" ||
                        it.name == "AgentType"
                }
                .assertTrue { valueObject ->
                    valueObject.resideInPackage("..valueobject..")
                }
        }

        "value objects should have validation" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.name == "AgentName" }
                .assertTrue { agentName ->
                    // AgentName should have validation in companion object
                    agentName.objects().any { obj ->
                        obj.name == "Companion" &&
                            obj.functions().any { func ->
                                func.name == "from" || func.name == "create"
                            }
                    }
                }
        }

        // ========== Error Hierarchy ==========

        "all errors should extend AgentManagementError" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.resideInPackage("..error..") }
                .filter { it.name.endsWith("Error") }
                .filter { it.name != "AgentManagementError" }
                .filter { !it.hasSealedModifier }
                .filter { !it.hasEnumModifier }
                .assertTrue { error ->
                    error.parents().any { parent ->
                        parent.name == "AgentManagementError" ||
                            parent.name.endsWith("Error")
                    }
                }
        }

        "repository errors should be properly structured" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.resideInPackage("..error..") }
                .filter { it.name.contains("Repository") }
                .assertTrue { error ->
                    // Repository errors should be sealed classes with specific cases
                    error.hasSealedModifier ||
                        error.parents().any { it.name.contains("RepositoryError") }
                }
        }

        // ========== Repository Pattern ==========

        "AgentRepository should be an interface in domain layer" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .interfaces()
                .filter { it.name == "AgentRepository" }
                .assertTrue { repository ->
                    repository.resideInPackage("..repository..") &&
                        repository.functions().all { func ->
                            // All repository methods should return Either
                            func.returnType?.sourceType?.contains("Either") == true
                        }
                }
        }

        "repository should follow CQRS pattern" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .interfaces()
                .filter { it.name == "AgentRepository" }
                .assertTrue { repository ->
                    repository.functions().any { it.name.startsWith("find") } &&
                        repository.functions().any { it.name == "save" || it.name == "create" }
                }
        }

        // ========== Entity Rules ==========

        "Agent entity should have proper structure" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.name == "Agent" }
                .assertTrue { agent ->
                    agent.hasDataModifier &&
                        agent.properties().any { it.name == "id" && it.type?.sourceType?.contains("AgentId") == true } &&
                        agent.properties().any { it.name == "name" && it.type?.sourceType?.contains("AgentName") == true } &&
                        agent.properties().any { it.name == "type" && it.type?.sourceType?.contains("AgentType") == true }
                }
        }

        "entities should have timestamp fields" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.resideInPackage("..entity..") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { entity ->
                    entity.properties().any { it.name == "createdAt" } &&
                        entity.properties().any { it.name == "updatedAt" }
                }
        }

        // ========== Infrastructure Layer Patterns ==========

        "infrastructure should not contain business logic" {
            Konsist
                .scopeFromDirectory("$contextPath/infrastructure")
                .classes()
                .filter { !it.name.endsWith("Test") }
                .assertFalse { infraClass ->
                    // Infrastructure should not have business rule validations
                    infraClass.functions().any { func ->
                        func.name.contains("validate") &&
                            !func.name.contains("validateDatabase") &&
                            !func.name.contains("validateConnection")
                    }
                }
        }

        "SQLDelight generated code should be in infrastructure" {
            Konsist
                .scopeFromDirectory("$contextPath/infrastructure")
                .files
                .filter { it.path.contains("generated/sqldelight") }
                .assertTrue { file ->
                    // Generated code should be in infrastructure layer
                    file.path.contains("/infrastructure/")
                }
        }

        // ========== Cross-Context Boundaries ==========

        "should not directly depend on other bounded contexts" {
            val otherContexts = listOf(
                "scope-management",
                "user-preferences",
                "collaborative-versioning",
                "event-store",
                "device-synchronization",
            )

            Konsist
                .scopeFromDirectory(contextPath)
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        otherContexts.any { context ->
                            import.name.contains(".$context.domain.")
                        }
                    }
                }
        }

        // ========== Naming Conventions ==========

        "enums should use UPPER_CASE naming" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.name == "AgentType" }
                .filter { it.hasEnumModifier }
                .assertTrue { enum ->
                    // Enum entries should be UPPER_CASE
                    enum.enumConstants.all { constant ->
                        constant.name == constant.name.uppercase() ||
                            constant.name.matches(Regex("[A-Z_]+"))
                    }
                }
        }

        "repository implementations should end with Impl" {
            Konsist
                .scopeFromDirectory("$contextPath/infrastructure")
                .classes()
                .filter { it.parents().any { parent -> parent.name == "AgentRepository" } }
                .assertTrue { impl ->
                    impl.name.endsWith("Impl") || impl.name.endsWith("RepositoryImpl")
                }
        }

        // ========== Application Layer Patterns ==========

        "commands and queries should be in separate packages" {
            Konsist
                .scopeFromDirectory("$contextPath/application")
                .classes()
                .filter { it.name.endsWith("Command") || it.name.endsWith("Query") }
                .assertTrue { commandOrQuery ->
                    when {
                        commandOrQuery.name.endsWith("Command") ->
                            commandOrQuery.resideInPackage("..command..")
                        commandOrQuery.name.endsWith("Query") ->
                            commandOrQuery.resideInPackage("..query..")
                        else -> false
                    }
                }
        }

        "handlers should match their command/query names" {
            Konsist
                .scopeFromDirectory("$contextPath/application")
                .classes()
                .filter { it.name.endsWith("Handler") }
                .assertTrue { handler ->
                    // Handler name should match command/query name
                    val expectedCommandOrQuery = handler.name.removeSuffix("Handler")
                    handler.functions().any { func ->
                        func.parameters.any { param ->
                            param.type.sourceType?.contains(expectedCommandOrQuery) == true
                        }
                    }
                }
        }

        // ========== Functional Programming Patterns ==========

        "use Arrow Either for error handling" {
            Konsist
                .scopeFromDirectory(contextPath)
                .functions()
                .filter { !it.name.endsWith("Test") }
                .filter { it.hasReturnType() }
                .assertFalse { function ->
                    // Should not throw exceptions
                    function.text.contains("throw ") &&
                        !function.text.contains("TODO") &&
                        !function.text.contains("NotImplementedError")
                }
        }

        "repository methods should return Either types" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .interfaces()
                .filter { it.name.endsWith("Repository") }
                .assertTrue { repository ->
                    repository.functions().all { function ->
                        function.returnType?.sourceType?.contains("Either") == true ||
                            function.name == "toString" ||
                            // Object methods
                            function.name == "equals" ||
                            function.name == "hashCode"
                    }
                }
        }

        // ========== Testing Patterns ==========

        "test classes should be in test directories" {
            Konsist
                .scopeFromDirectory(contextPath)
                .classes()
                .filter { it.name.endsWith("Test") }
                .assertTrue { testClass ->
                    testClass.containingFile.path.contains("src/test")
                }
        }

        "production code should not reference test classes" {
            Konsist
                .scopeFromDirectory(contextPath)
                .files
                .filter { it.path.contains("src/main") }
                .assertFalse { file ->
                    file.imports.any { import ->
                        import.name.contains("Test") ||
                            import.name.contains("mockk") ||
                            import.name.contains("kotest") ||
                            import.name.contains("junit")
                    }
                }
        }

        // ========== Package Structure Validation ==========

        "domain layer should have proper package structure" {
            val requiredPackages = setOf("entity", "valueobject", "repository", "error")
            val domainPackages = Konsist
                .scopeFromDirectory("$contextPath/domain")
                .packages
                .filter { it.name.contains("agentmanagement.domain") }
                .map { it.name.substringAfterLast(".domain.") }
                .filter { it.isNotEmpty() }
                .toSet()

            // All required packages should exist
            requiredPackages.forEach { required ->
                assert(domainPackages.contains(required)) {
                    "Domain layer missing required package: $required"
                }
            }
        }

        // ========== Anti-Patterns Detection ==========

        "avoid anemic domain models" {
            Konsist
                .scopeFromDirectory("$contextPath/domain")
                .classes()
                .filter { it.resideInPackage("..entity..") }
                .filter { !it.name.endsWith("Test") }
                .assertTrue { entity ->
                    // Entities should have behavior, not just getters/setters
                    entity.functions().any { func ->
                        func.name != "get" &&
                            !func.name.startsWith("get") &&
                            !func.name.startsWith("set") &&
                            !func.name.startsWith("component") &&
                            // Data class methods
                            func.name != "copy" &&
                            func.name != "toString" &&
                            func.name != "equals" &&
                            func.name != "hashCode"
                    } ||
                        entity.properties().size < 3 // Simple entities are OK
                }
        }
    })
