package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Tests to detect domain knowledge anti-patterns like anemic domain models.
 */
class DomainKnowledgeAntiPatternsTest :
    StringSpec({

        // TODO: Re-enable after fixing error class structure
        // "error classes should have rich structured information, not just message strings" {
        /*
            // Find all error classes
            val errorClasses = Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .filterNot { it.path.contains("/platform/") }
                .filterNot { it.path.contains("/infrastructure/") }
                .flatMap { it.classes() }
                .filter { clazz ->
                    clazz.name.endsWith("Error") || clazz.name.endsWith("Exception")
                }
                .filterNot { clazz ->
                    // Skip standard Kotlin/Java exceptions
                    clazz.packagee?.name?.contains("kotlin") == true ||
                        clazz.packagee?.name?.contains("java") == true
                }

            // Check for anemic error patterns
            errorClasses.assertFalse { clazz ->
                val properties = clazz.properties()
                val propertyNames = properties.map { it.name }

                // Pattern 1: Only has "message"
                val hasOnlyMessage = propertyNames.size == 1 && propertyNames.contains("message")

                // Pattern 2: Only has "message" and "cause"
                val hasOnlyMessageAndCause = propertyNames.size == 2 &&
                    propertyNames.containsAll(listOf("message", "cause"))

                // Pattern 3: Only has generic properties
                val hasOnlyGenericProperties = propertyNames.isNotEmpty() &&
                    propertyNames.all { it in listOf("message", "cause", "occurredAt", "timestamp", "throwable") }

                // It's anemic if it matches any of these patterns
                // Unless it's part of a sealed hierarchy (gets rich data from subtypes)
                val isAnemic = (hasOnlyMessage || hasOnlyMessageAndCause || hasOnlyGenericProperties) &&
                    !clazz.hasSealedModifier

                isAnemic
            }
        }
         */

        "contract error interfaces should be sealed" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .filter { it.path.contains("/contracts/") && it.path.contains("/errors/") }
                .flatMap { it.interfaces() }
                .filter { it.name.endsWith("Error") || it.name.endsWith("ContractError") }
                .assertTrue { it.hasSealedModifier }
        }

        "domain entities should have behavior methods" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .filter { file ->
                    file.path.contains("/domain/entity/") || file.path.contains("/domain/aggregate/")
                }
                .flatMap { it.classes() }
                .filterNot { it.hasAbstractModifier }
                .filterNot { it.hasDataModifier } // Data classes are OK for simple entities
                .assertTrue { clazz ->
                    // Should have at least one public method that's not a getter/setter
                    clazz.functions()
                        .filter { it.hasPublicOrDefaultModifier }
                        .any { func ->
                            !func.name.startsWith("get") &&
                                !func.name.startsWith("set") &&
                                !func.name.startsWith("component") &&
                                func.name !in listOf("copy", "equals", "hashCode", "toString")
                        }
                }
        }

        "value objects should be immutable" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .filter { file ->
                    file.path.contains("/domain/valueobject/") || file.path.contains("/domain/value/")
                }
                .flatMap { it.classes() }
                .assertTrue { clazz ->
                    // All properties should be val, not var
                    clazz.properties().all { !it.hasVarModifier }
                }
        }

        "avoid unsafe nullable assertions - use safe calls or proper error handling" {
            Konsist
                .scopeFromProduction()
                .files
                .filterNot { it.path.contains("/tmp/") }
                .filterNot { it.path.contains("/test/") }
                .assertFalse { file ->
                    val content = file.text
                    // Check for the specific anti-pattern of .getOrNull()!!
                    content.contains(".getOrNull()!!") ||
                        // Also check for other unsafe patterns
                        content.contains("?.let { }!!") ||
                        // Check for double-bang on nullable results
                        (content.contains(".firstOrNull()!!") && !file.path.contains("Test")) ||
                        (content.contains(".singleOrNull()!!") && !file.path.contains("Test")) ||
                        (content.contains(".find {") && content.contains("}!!") && !file.path.contains("Test"))
                }
        }
    })
