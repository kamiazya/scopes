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

        describe("Test Framework Consistency Rules") {

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

            it("test classes should use Kotest specs") {
                // All test classes should extend Kotest specs (DescribeSpec or StringSpec) for consistency
                Konsist.scopeFromProject()
                    .classes()
                    .withNameEndingWith("Test")
                    .assertFalse { clazz ->
                        // Every test class must extend a Kotest spec
                        // Return true (fail assertion) if class does NOT extend DescribeSpec or StringSpec
                        !clazz.hasParent { parent ->
                            parent.name == "DescribeSpec" || parent.name == "StringSpec"
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
                // Kotest spec files must not import or call JUnit/Hamcrest assertions
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.classes().any { clazz ->
                                clazz.hasParent { parent ->
                                    parent.name == "DescribeSpec" || parent.name == "StringSpec"
                                }
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
                // Ensure Kotest spec test files actually use Kotest matchers/assertions
                // Exception: Konsist architecture tests can use Konsist's own assertions
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.classes().any { clazz ->
                                clazz.hasParent { parent ->
                                    parent.name == "DescribeSpec" || parent.name == "StringSpec"
                                }
                            }
                    }
                    .assertFalse { file ->
                        // Skip Konsist architecture test files as they use Konsist's assertions
                        if (file.path.contains("/konsist/") &&
                            file.imports.any { it.name.startsWith("com.lemonappdev.konsist") }
                        ) {
                            return@assertFalse false
                        }

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

            it("tests should use standardized Either assertion patterns") {
                // Tests using Arrow Either should use direct shouldBeRight/shouldBeLeft
                // instead of manual casting or .fold() patterns
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.imports.any { import ->
                                import.name.startsWith("io.kotest.assertions.arrow.core")
                            }
                    }
                    .assertFalse { file ->
                        // Check for deprecated patterns that should be avoided
                        val hasManualEitherCasting = file.text.contains(Regex("""\(.*\bas\s+Either\.Left\)""")) ||
                            file.text.contains(Regex("""\(.*\bas\s+Either\.Right\)"""))

                        val hasManualIsLeftRight = file.text.contains(Regex("""\.isLeft\(\)\s+shouldBe\s+true""")) ||
                            file.text.contains(Regex("""\.isRight\(\)\s+shouldBe\s+true"""))

                        val hasFoldPattern = file.text.contains(Regex("""\.fold\s*\(\s*\{\s*throw\s+AssertionError"""))

                        hasManualEitherCasting || hasManualIsLeftRight || hasFoldPattern
                    }
            }

            it("tests should avoid non-null assertions on shouldBeInstanceOf results") {
                // Tests should leverage shouldBeInstanceOf's reified return type
                // instead of manual casting with 'as'
                Konsist.scopeFromProject()
                    .files
                    .filter { file ->
                        file.path.contains("/test/") &&
                            file.text.contains("shouldBeInstanceOf") &&
                            // Skip this test file itself to avoid self-referential detection
                            !file.path.contains("TestFrameworkConsistencyTest.kt")
                    }
                    .assertFalse { file ->
                        // Look for pattern: shouldBeInstanceOf followed by manual casting on same or next line
                        val lines = file.text.lines()

                        // Check for same-line pattern first
                        val hasSameLinePattern = lines.any { line ->
                            val trimmedLine = line.trim()
                            trimmedLine.contains("shouldBeInstanceOf<") &&
                                trimmedLine.contains(" as ") &&
                                !trimmedLine.startsWith("//") &&
                                // Ignore comments
                                !trimmedLine.startsWith("*") // Ignore doc comments
                        }

                        if (hasSameLinePattern) return@assertFalse true

                        // Check for multi-line pattern
                        for (i in 0 until lines.size - 1) {
                            val currentLine = lines[i].trim()
                            val nextLine = lines.getOrNull(i + 1)?.trim() ?: ""

                            // Skip comments and empty lines
                            if (currentLine.startsWith("//") ||
                                currentLine.startsWith("*") ||
                                nextLine.startsWith("//") ||
                                nextLine.startsWith("*")
                            ) {
                                continue
                            }

                            if (currentLine.contains("shouldBeInstanceOf<") &&
                                nextLine.contains(" as ") &&
                                !nextLine.contains("assertFalse") // Don't flag our test logic
                            ) {
                                return@assertFalse true
                            }
                        }
                        false
                    }
            }
        }
    })
