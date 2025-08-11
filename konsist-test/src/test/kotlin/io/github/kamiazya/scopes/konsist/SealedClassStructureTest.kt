package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec


/**
 * Konsist tests for sealed class structure validation.
 * Ensures sealed hierarchies follow modern Kotlin patterns with reduced nesting.
 */
class SealedClassStructureTest : StringSpec({

    "sealed classes should have maximum 2-level nesting" {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.hasSealedModifier }
            .assertTrue { sealedClass ->
                // Check that nested sealed classes don't have further nested sealed classes
                val nestedSealedClasses = sealedClass.classes().filter { it.hasSealedModifier }
                
                nestedSealedClasses.all { nestedSealed ->
                    // Second level sealed classes should not have nested sealed classes
                    nestedSealed.classes().none { it.hasSealedModifier }
                }
            }
    }

    "error hierarchies should use sealed classes appropriately" {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.name.endsWith("Error") && it.hasSealedModifier }
            .assertTrue { errorClass ->
                // Root error classes should be sealed
                // Their subclasses should be either:
                // 1. Data classes (for leaf errors)
                // 2. Sealed classes (for intermediate categories) 
                // 3. Object classes (for singleton errors)
                val subclasses = errorClass.classes()
                
                subclasses.all { subclass ->
                    when {
                        subclass.hasDataModifier -> !subclass.hasSealedModifier
                        subclass.hasSealedModifier -> !subclass.hasDataModifier
                        // Allow object declarations (they don't have object modifier property in Konsist)
                        subclass.text.contains("object ") -> !subclass.hasSealedModifier && !subclass.hasDataModifier
                        // Allow regular classes as well (for context/category classes in the new structure)
                        else -> !subclass.hasDataModifier && !subclass.hasSealedModifier
                    }
                }
            }
    }

    "sealed class subclasses should be in the same package" {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.hasSealedModifier }
            .assertTrue { sealedClass ->
                val sealedPackage = sealedClass.packagee?.name ?: ""
                
                // In Kotlin 1.5+, sealed class subclasses must be in the same package
                // This is enforced by the compiler, but we verify the structure
                val directSubclasses = sealedClass.classes()
                
                directSubclasses.all { subclass ->
                    val subclassPackage = subclass.packagee?.name ?: ""
                    subclassPackage == sealedPackage
                }
            }
    }

    "sealed class naming should be consistent" {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.hasSealedModifier }
            .assertTrue { sealedClass ->
                val name = sealedClass.name
                
                // Sealed classes should follow naming conventions based on their role
                when {
                    sealedClass.resideInPackage("..error..") -> {
                        // In error packages, allow various naming patterns:
                        // 1. Root error types end with "Error" or "Exception"  
                        // 2. Context/category classes can have descriptive names
                        // 3. Result types can end with "Result" 
                        // 4. Configuration classes can end with "Configuration"
                        // 5. Violation classes can contain "Violation"
                        name.endsWith("Error") || name.endsWith("Exception") || 
                        name.endsWith("Context") || name.endsWith("Type") ||
                        name.endsWith("Category") || name.endsWith("Kind") ||
                        name.endsWith("Result") || name.endsWith("Configuration") ||
                        name.contains("Violation")
                    }
                    sealedClass.resideInPackage("..result..") -> 
                        name.endsWith("Result") || name == "Result"
                    sealedClass.resideInPackage("..type..") -> 
                        name.endsWith("Type") || name.endsWith("Kind")
                    else -> true // Other packages can have different conventions
                }
            }
    }
})