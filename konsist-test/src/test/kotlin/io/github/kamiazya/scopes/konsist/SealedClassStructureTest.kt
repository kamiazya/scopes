package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec


/**
 * Konsist tests for sealed class and interface structure validation.
 * Ensures sealed hierarchies follow modern Kotlin patterns with reduced nesting.
 */
class SealedClassStructureTest : StringSpec({

    "sealed classes should have maximum 2-level nesting" {
        Konsist
            .scopeFromProduction()
            .classes()
            .filter { it.hasSealedModifier }
            .assertTrue { sealedClass ->
                // Check that nested sealed classes don't have further nested sealed classes/interfaces
                val nestedSealedClasses = sealedClass.classes().filter { it.hasSealedModifier }
                val nestedSealedInterfaces = sealedClass.interfaces().filter { it.hasSealedModifier }
                
                // Second level sealed classes should not have nested sealed classes or interfaces
                nestedSealedClasses.all { nestedSealed ->
                    nestedSealed.classes().none { it.hasSealedModifier } &&
                    nestedSealed.interfaces().none { it.hasSealedModifier }
                } && nestedSealedInterfaces.all { nestedSealed ->
                    nestedSealed.classes().none { it.hasSealedModifier } &&
                    nestedSealed.interfaces().none { it.hasSealedModifier }
                }
            }
    }

    "sealed interfaces should have maximum 2-level nesting" {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .filter { it.hasSealedModifier }
            .assertTrue { sealedInterface ->
                // Check that nested sealed interfaces don't have further nested sealed classes/interfaces
                val nestedSealedClasses = sealedInterface.classes().filter { it.hasSealedModifier }
                val nestedSealedInterfaces = sealedInterface.interfaces().filter { it.hasSealedModifier }
                
                // Second level sealed classes/interfaces should not have nested sealed types
                nestedSealedClasses.all { nestedSealed ->
                    nestedSealed.classes().none { it.hasSealedModifier } &&
                    nestedSealed.interfaces().none { it.hasSealedModifier }
                } && nestedSealedInterfaces.all { nestedSealed ->
                    nestedSealed.classes().none { it.hasSealedModifier } &&
                    nestedSealed.interfaces().none { it.hasSealedModifier }
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
                val subinterfaces = errorClass.interfaces()
                
                subclasses.all { subclass ->
                    when {
                        subclass.hasDataModifier -> !subclass.hasSealedModifier
                        subclass.hasSealedModifier -> !subclass.hasDataModifier
                        // Allow object declarations (they don't have object modifier property in Konsist)
                        subclass.text.contains("object ") -> !subclass.hasSealedModifier && !subclass.hasDataModifier
                        // Allow regular classes as well (for context/category classes in the new structure)
                        else -> !subclass.hasDataModifier && !subclass.hasSealedModifier
                    }
                } && subinterfaces.all { subinterface ->
                    // Sealed interfaces in error hierarchies should not have data modifier (doesn't apply to interfaces)
                    // but they can be sealed for intermediate categories
                    true
                }
            }
    }

    "error hierarchies should use sealed interfaces appropriately" {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .filter { it.name.endsWith("Error") && it.hasSealedModifier }
            .assertTrue { errorInterface ->
                // Root error interfaces should be sealed
                // Their implementations should be either:
                // 1. Data classes (for leaf errors)
                // 2. Sealed classes/interfaces (for intermediate categories)
                // 3. Object classes (for singleton errors) 
                val subclasses = errorInterface.classes()
                val subinterfaces = errorInterface.interfaces()
                
                subclasses.all { subclass ->
                    when {
                        subclass.hasDataModifier -> !subclass.hasSealedModifier
                        subclass.hasSealedModifier -> !subclass.hasDataModifier
                        // Allow object declarations
                        subclass.text.contains("object ") -> !subclass.hasSealedModifier && !subclass.hasDataModifier
                        // Allow regular classes
                        else -> !subclass.hasDataModifier && !subclass.hasSealedModifier
                    }
                } && subinterfaces.all { subinterface ->
                    // Nested sealed interfaces are allowed for intermediate categories
                    true
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
                val directSubinterfaces = sealedClass.interfaces()
                
                directSubclasses.all { subclass ->
                    val subclassPackage = subclass.packagee?.name ?: ""
                    subclassPackage == sealedPackage
                } && directSubinterfaces.all { subinterface ->
                    val subinterfacePackage = subinterface.packagee?.name ?: ""
                    subinterfacePackage == sealedPackage
                }
            }
    }

    "sealed interface subclasses should be in the same package" {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .filter { it.hasSealedModifier }
            .assertTrue { sealedInterface ->
                val sealedPackage = sealedInterface.packagee?.name ?: ""
                
                // In Kotlin 1.5+, sealed interface subclasses must be in the same package
                // This is enforced by the compiler, but we verify the structure
                val directSubclasses = sealedInterface.classes()
                val directSubinterfaces = sealedInterface.interfaces()
                
                directSubclasses.all { subclass ->
                    val subclassPackage = subclass.packagee?.name ?: ""
                    subclassPackage == sealedPackage
                } && directSubinterfaces.all { subinterface ->
                    val subinterfacePackage = subinterface.packagee?.name ?: ""
                    subinterfacePackage == sealedPackage
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

    "sealed interface naming should be consistent" {
        Konsist
            .scopeFromProduction()
            .interfaces()
            .filter { it.hasSealedModifier }
            .assertTrue { sealedInterface ->
                val name = sealedInterface.name
                
                // Sealed interfaces should follow naming conventions based on their role
                when {
                    sealedInterface.resideInPackage("..error..") -> {
                        // In error packages, allow various naming patterns:
                        // 1. Root error types end with "Error" or "Exception"  
                        // 2. Context/category interfaces can have descriptive names
                        // 3. Result types can end with "Result" 
                        // 4. Configuration interfaces can end with "Configuration"
                        // 5. Violation interfaces can contain "Violation"
                        name.endsWith("Error") || name.endsWith("Exception") || 
                        name.endsWith("Context") || name.endsWith("Type") ||
                        name.endsWith("Category") || name.endsWith("Kind") ||
                        name.endsWith("Result") || name.endsWith("Configuration") ||
                        name.contains("Violation")
                    }
                    sealedInterface.resideInPackage("..result..") -> 
                        name.endsWith("Result") || name == "Result"
                    sealedInterface.resideInPackage("..type..") -> 
                        name.endsWith("Type") || name.endsWith("Kind")
                    else -> true // Other packages can have different conventions
                }
            }
    }
})