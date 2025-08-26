package io.github.kamiazya.scopes.quality.konsist.testing

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import io.kotest.core.spec.style.DescribeSpec

/**
 * Tests to ensure consistent test framework usage across the project.
 * All tests should use Kotest instead of mixing JUnit and Kotest.
 */
class TestFrameworkConsistencyTest :
    DescribeSpec({

        // Temporarily skip these tests while we migrate remaining files
        xdescribe("Test Framework Consistency Rules (Currently Disabled)") {

            it("test classes should not use JUnit annotations") {
                // All test classes should use Kotest instead of JUnit for consistency
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.classes().any { it.name?.endsWith("Test") ?: false }
                    }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            val importPath = import.name
                            importPath.startsWith("org.junit.jupiter.api.") ||
                                importPath.startsWith("org.junit.Test") ||
                                importPath.startsWith("org.junit.Before") ||
                                importPath.startsWith("org.junit.After") ||
                                importPath.startsWith("org.junit.Assert")
                        }
                    }
            }

            it("test classes should use Kotest DescribeSpec") {
                // All test classes should extend Kotest DescribeSpec for consistency
                Konsist.scopeFromProject()
                    .classes()
                    .withNameEndingWith("Test")
                    .assertFalse { clazz ->
                        // Check if class doesn't extend DescribeSpec but has test-like structure
                        val extendsDescribeSpec = clazz.hasParent { parent ->
                            parent.name == "DescribeSpec"
                        }

                        val hasJUnitAnnotations = clazz.functions().any { function ->
                            function.annotations.any { annotation ->
                                annotation.name == "Test" ||
                                    annotation.name == "BeforeEach" ||
                                    annotation.name == "AfterEach" ||
                                    annotation.name == "BeforeAll" ||
                                    annotation.name == "AfterAll" ||
                                    annotation.name == "Nested" ||
                                    annotation.name == "DisplayName"
                            }
                        }

                        // Flag classes that use JUnit annotations instead of Kotest
                        hasJUnitAnnotations && !extendsDescribeSpec
                    }
            }

            it("test files should not import JUnit assertions") {
                // Test files should use Kotest matchers instead of JUnit assertions
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            (
                                file.classes().any { it.name?.endsWith("Test") ?: false } ||
                                    file.interfaces().any { it.name?.endsWith("Test") ?: false }
                                )
                    }
                    .assertFalse { file ->
                        file.imports.any { import ->
                            val importPath = import.name
                            importPath.startsWith("org.junit.jupiter.api.Assertions") ||
                                importPath.contains("org.junit.Assert") ||
                                importPath.contains("org.hamcrest")
                        }
                    }
            }

            it("test classes should use Kotest matchers") {
                // Test classes should import and use Kotest matchers for assertions
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.classes().any { clazz ->
                                clazz.hasParent { parent -> parent.name == "DescribeSpec" }
                            }
                    }
                    .assertFalse { file ->
                        // Check if file uses JUnit-style assertions instead of Kotest matchers
                        val hasJUnitAssertions = file.text.contains("assertEquals(") ||
                            file.text.contains("assertTrue(") ||
                            file.text.contains("assertFalse(") ||
                            file.text.contains("assertNull(") ||
                            file.text.contains("assertNotNull(")

                        val hasKotestImports = file.imports.any { import ->
                            import.name.startsWith("io.kotest.matchers")
                        }

                        // Flag files that use JUnit assertions without Kotest imports
                        hasJUnitAssertions && !hasKotestImports
                    }
            }
        }
    })
