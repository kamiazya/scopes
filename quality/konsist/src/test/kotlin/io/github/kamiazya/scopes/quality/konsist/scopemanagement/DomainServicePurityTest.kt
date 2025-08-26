package io.github.kamiazya.scopes.quality.konsist.scopemanagement

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/**
 * Tests to ensure domain services follow DDD principles of purity.
 * Domain services should contain only pure business logic without I/O operations.
 */
class DomainServicePurityTest {

    /**
     * Domain services should not have suspend functions as they indicate I/O operations.
     * All I/O should be handled in the application layer.
     */
    @Test
    fun `domain services should not have suspend functions`() {
        Konsist.scopeFromModule("contexts/scope-management/domain")
            .classes()
            .withNameEndingWith("Service")
            .flatMap { it.functions() }
            .assertFalse { function ->
                function.hasSuspendModifier
            }
    }

    /**
     * Domain services should not depend on repositories directly.
     * Repository access should be handled by application services.
     */
    @Test
    fun `domain services should not depend on repositories`() {
        Konsist.scopeFromModule("contexts/scope-management/domain")
            .classes()
            .withNameEndingWith("Service")
            .assertFalse { clazz ->
                // Check constructor parameters
                clazz.primaryConstructor?.parameters?.any { parameter ->
                    parameter.type?.name?.endsWith("Repository") ?: false
                } ?: false ||
                    // Check all constructors
                    clazz.secondaryConstructors.any { constructor ->
                        constructor.parameters.any { parameter ->
                            parameter.type?.name?.endsWith("Repository") ?: false
                        }
                    }
            }
    }

    /**
     * Domain services should not have function parameters that are suspend functions.
     * This ensures domain logic remains pure and I/O-free.
     */
    @Test
    fun `domain services should not have I-O function parameters`() {
        Konsist.scopeFromModule("contexts/scope-management/domain")
            .classes()
            .withNameEndingWith("Service")
            .flatMap { it.functions() }
            .flatMap { it.parameters }
            .assertFalse { parameter ->
                // Check for function types with suspend modifier
                parameter.type?.text?.contains("suspend") ?: false
            }
    }

    /**
     * Domain service interfaces should not define suspend functions.
     * Interface contracts should represent pure business operations.
     */
    @Test
    fun `domain service interfaces should not have suspend functions`() {
        Konsist.scopeFromModule("contexts/scope-management/domain")
            .interfaces()
            .withNameEndingWith("Service")
            .flatMap { it.functions() }
            .assertFalse { function ->
                function.hasSuspendModifier
            }
    }

    /**
     * Domain services should only import domain types and pure libraries.
     * No infrastructure or application layer dependencies allowed.
     */
    @Test
    fun `domain services should only use domain types and pure libraries`() {
        Konsist.scopeFromModule("contexts/scope-management/domain")
            .files
            .filter { file ->
                file.classes().any { it.name?.endsWith("Service") ?: false } ||
                    file.interfaces().any { it.name?.endsWith("Service") ?: false }
            }
            .assertFalse { file ->
                file.imports.any { import ->
                    val importPath = import.name
                    // Check for infrastructure or application imports
                    importPath.contains(".infrastructure.") ||
                        importPath.contains(".application.") ||
                        importPath.contains(".repository.") ||
                        importPath.contains(".database.") ||
                        importPath.contains(".api.") ||
                        importPath.contains(".http.") ||
                        // Check for I/O related imports
                        importPath.contains("kotlinx.coroutines") ||
                        importPath.contains("java.io") ||
                        importPath.contains("java.nio")
                }
            }
    }

    /**
     * Publisher interfaces in domain should not have suspend functions.
     * Event publishing is an infrastructure concern that should be abstracted.
     */
    @Test
    fun `domain event publishers should not have suspend functions`() {
        Konsist.scopeFromModule("contexts/scope-management/domain")
            .interfaces()
            .withNameEndingWith("Publisher")
            .flatMap { it.functions() }
            .assertFalse { function ->
                function.hasSuspendModifier
            }
    }
}
