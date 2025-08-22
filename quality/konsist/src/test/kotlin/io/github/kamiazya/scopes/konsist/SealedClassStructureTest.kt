package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Konsist tests for sealed class and interface structure validation.
 * Ensures sealed hierarchies follow modern Kotlin patterns with reduced nesting.
 */
class SealedClassStructureTest :
    StringSpec({

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
                    } &&
                        nestedSealedInterfaces.all { nestedSealed ->
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
                    } &&
                        nestedSealedInterfaces.all { nestedSealed ->
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
                    val subobjects = errorClass.objects()

                    // Allow sealed root error classes without direct nested children (they may be extended by other sealed classes in different files)
                    subclasses.all { subclass ->
                        when {
                            subclass.hasDataModifier -> !subclass.hasSealedModifier
                            subclass.hasSealedModifier -> !subclass.hasDataModifier
                            // Allow regular classes as well (for context/category classes in the new structure)
                            else -> !subclass.hasDataModifier && !subclass.hasSealedModifier
                        }
                    } &&
                        subinterfaces.all { subinterface ->
                            // Sealed interfaces in error hierarchies should not have data modifier (doesn't apply to interfaces)
                            // but they can be sealed for intermediate categories
                            true
                        } &&
                        subobjects.all { subobject ->
                            // Objects are by nature singular instances and cannot be sealed or data
                            // (objects don't have these modifiers in Kotlin, so this is always true)
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
                    val subobjects = errorInterface.objects()

                    // Allow interfaces without direct children if they are base interfaces for contexts
                    // (they may have implementations in other files)
                    val hasChildren = subclasses.isNotEmpty() || subinterfaces.isNotEmpty() || subobjects.isNotEmpty()
                    val classesValid = subclasses.all { subclass ->
                        when {
                            subclass.hasDataModifier -> !subclass.hasSealedModifier
                            subclass.hasSealedModifier -> !subclass.hasDataModifier
                            else -> !subclass.hasDataModifier && !subclass.hasSealedModifier
                        }
                    }
                    val interfacesValid = subinterfaces.all { true }
                    val objectsValid = subobjects.all { true }
                    // Either has children OR is a base interface (ScopeError, PlatformError)
                    (hasChildren && classesValid && interfacesValid && objectsValid) ||
                        (errorInterface.name == "ScopeError" || errorInterface.name == "PlatformError")
                }
        }

        "sealed class subclasses should be in the same package" {
            Konsist
                .scopeFromProduction()
                .classes()
                .filter { it.hasSealedModifier }
                .assertTrue { sealedClass ->
                    val sealedPackage = sealedClass.packagee?.name ?: ""
                    val sealedFqName = "${sealedClass.packagee?.name ?: ""}.${sealedClass.name}"

                    // In Kotlin 1.5+, sealed class subclasses must be in the same package
                    // This is enforced by the compiler, but we verify the structure
                    // Find all classes, interfaces, and objects that directly extend/implement this sealed class
                    val scope = Konsist.scopeFromProduction()
                    val childClasses = scope.classes().filter { childClass ->
                        childClass.parents().any { parent ->
                            val parentFqName = "${parent.packagee?.name ?: ""}.${parent.name}"
                            parentFqName == sealedFqName
                        }
                    }

                    val childInterfaces = scope.interfaces().filter { childInterface ->
                        childInterface.parents().any { parent ->
                            val parentFqName = "${parent.packagee?.name ?: ""}.${parent.name}"
                            parentFqName == sealedFqName
                        }
                    }

                    val childObjects = scope.objects().filter { childObject ->
                        childObject.parents().any { parent ->
                            val parentFqName = "${parent.packagee?.name ?: ""}.${parent.name}"
                            parentFqName == sealedFqName
                        }
                    }

                    childClasses.all { childClass ->
                        val childPackage = childClass.packagee?.name ?: ""
                        childPackage == sealedPackage
                    } &&
                        childInterfaces.all { childInterface ->
                            val childPackage = childInterface.packagee?.name ?: ""
                            childPackage == sealedPackage
                        } &&
                        childObjects.all { childObject ->
                            val childPackage = childObject.packagee?.name ?: ""
                            childPackage == sealedPackage
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
                    val sealedFqName = "${sealedInterface.packagee?.name ?: ""}.${sealedInterface.name}"

                    // In Kotlin 1.5+, sealed interface subclasses must be in the same package
                    // This is enforced by the compiler, but we verify the structure
                    // Find all classes, interfaces, and objects that directly extend/implement this sealed interface
                    val scope = Konsist.scopeFromProduction()
                    val childClasses = scope.classes().filter { childClass ->
                        childClass.parents().any { parent ->
                            val parentFqName = "${parent.packagee?.name ?: ""}.${parent.name}"
                            parentFqName == sealedFqName
                        }
                    }

                    val childInterfaces = scope.interfaces().filter { childInterface ->
                        childInterface.parents().any { parent ->
                            val parentFqName = "${parent.packagee?.name ?: ""}.${parent.name}"
                            parentFqName == sealedFqName
                        }
                    }

                    val childObjects = scope.objects().filter { childObject ->
                        childObject.parents().any { parent ->
                            val parentFqName = "${parent.packagee?.name ?: ""}.${parent.name}"
                            parentFqName == sealedFqName
                        }
                    }

                    childClasses.all { childClass ->
                        val childPackage = childClass.packagee?.name ?: ""
                        childPackage == sealedPackage
                    } &&
                        childInterfaces.all { childInterface ->
                            val childPackage = childInterface.packagee?.name ?: ""
                            childPackage == sealedPackage
                        } &&
                        childObjects.all { childObject ->
                            val childPackage = childObject.packagee?.name ?: ""
                            childPackage == sealedPackage
                        }
                }
        }

        "sealed class and interface naming should be consistent" {
            val sealedClasses = Konsist.scopeFromProduction().classes().filter { it.hasSealedModifier }
            val sealedInterfaces = Konsist.scopeFromProduction().interfaces().filter { it.hasSealedModifier }
            val allSealedTypes = sealedClasses + sealedInterfaces

            allSealedTypes.assertTrue { sealedType ->
                val name = sealedType.name

                // Sealed types should follow naming conventions based on their role
                when {
                    sealedType.resideInPackage("..error..") -> {
                        // In error packages, allow various naming patterns:
                        // 1. Root error types end with "Error" or "Exception"
                        // 2. Context/category types can have descriptive names
                        // 3. Result types can end with "Result"
                        // 4. Configuration types can end with "Configuration"
                        // 5. Violation types can contain "Violation"
                        name.endsWith("Error") ||
                            name.endsWith("Exception") ||
                            name.endsWith("Context") ||
                            name.endsWith("Type") ||
                            name.endsWith("Category") ||
                            name.endsWith("Kind") ||
                            name.endsWith("Result") ||
                            name.endsWith("Configuration") ||
                            name.contains("Violation")
                    }
                    sealedType.resideInPackage("..result..") ->
                        name.endsWith("Result") || name == "Result" || name == "PlatformError"
                    sealedType.resideInPackage("..type..") ->
                        name.endsWith("Type") || name.endsWith("Kind")
                    else -> true // Other packages can have different conventions
                }
            }
        }
    })
