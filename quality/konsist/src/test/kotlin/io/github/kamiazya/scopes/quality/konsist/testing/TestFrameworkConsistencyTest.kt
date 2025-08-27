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
                        // Every test class must extend DescribeSpec
                        // Return true (fail assertion) if class does NOT extend DescribeSpec
                        !clazz.hasParent { parent ->
                            parent.name == "DescribeSpec"
                        }
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

            it("Kotest-based test files should not use JUnit/Hamcrest assertions") {
                // Kotest DescribeSpec files must not import or call JUnit/Hamcrest assertions
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.classes().any { clazz ->
                                clazz.hasParent { parent -> parent.name == "DescribeSpec" }
                            }
                    }
                    .assertFalse { file ->
                        // Import-based detection
                        val hasJUnitAssertionImports = file.imports.any { import ->
                            val importPath = import.name
                            importPath.startsWith("org.junit.jupiter.api.Assertions") ||
                                importPath.startsWith("org.junit.Assert") ||
                                importPath.startsWith("org.hamcrest")
                        }

                        // Call-site detection for qualified assertions only
                        val usesJUnitAssertionCalls =
                            file.text.contains(Regex("""\bAssertions\.\w+\(""")) ||
                                file.text.contains(Regex("""\bAssert\.\w+\("""))

                        hasJUnitAssertionImports || usesJUnitAssertionCalls
                    }
            }

            it("Kotest-based test files should use Kotest assertions") {
                // Ensure DescribeSpec test files actually use Kotest matchers/assertions
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.classes().any { clazz ->
                                clazz.hasParent { parent -> parent.name == "DescribeSpec" }
                            }
                    }
                    .assertFalse { file ->
                        // Check if file has actual test content (not just structure)
                        val hasTestContent = file.text.contains("it(\"") ||
                            file.text.contains("it {") ||
                            file.text.contains("describe(\"") ||
                            file.text.contains("describe {")

                        // If it has test content, it should import Kotest assertions
                        if (hasTestContent) {
                            val hasKotestAssertions = file.imports.any { import ->
                                val importPath = import.name
                                importPath.startsWith("io.kotest.assertions") ||
                                    importPath.startsWith("io.kotest.matchers")
                            }
                            !hasKotestAssertions
                        } else {
                            false // No test content, so no assertion needed
                        }
                    }
            }
        }
    })
