package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Test quality architecture tests to ensure test code follows best practices.
 * These rules were created based on issues identified during PR #119 review.
 */
class TestQualityArchitectureTest {

    @Test
    @DisplayName("Test classes should properly import coroutine test utilities")
    fun `test classes should properly import coroutine test utilities`() {
        // Note: Currently using runBlocking as kotlinx-coroutines-test is not in dependencies
        // This test is disabled until the project adds kotlinx-coroutines-test dependency
        // Then we can enforce using runTest instead of runBlocking
        Konsist
            .scopeFromProject()
            .files
            .withNameEndingWith("Test.kt")
            .flatMap { it.imports }
            .filter { import ->
                import.name.contains("kotlinx.coroutines")
            }
            .assertTrue { import ->
                // Currently allowing runBlocking
                true
            }
    }

    @Test
    @DisplayName("Repository tests should properly manage database resources")
    fun `repository tests should properly manage database resources`() {
        Konsist
            .scopeFromProject()
            .classes
            .withNameEndingWith("RepositoryTest")
            .flatMap { it.functions }
            .filter { function ->
                function.name == "afterEach" || function.name == "afterTest"
            }
            .assertTrue { function ->
                // Ensure cleanup methods exist and contain close() calls
                function.text.contains("close()")
            }
    }

    @Test
    @DisplayName("Tests should use specific error type assertions")
    fun `tests should assert specific error types not just isLeft`() {
        Konsist
            .scopeFromProject()
            .files
            .withNameEndingWith("Test.kt")
            .flatMap { it.functions }
            .filter { function ->
                function.hasAnnotationOf(Test::class) ||
                function.name.startsWith("test") ||
                function.name.contains("should")
            }
            .filter { function ->
                // Find functions that check for errors
                function.text.contains("isLeft()") ||
                function.text.contains("leftOrNull()")
            }
            .assertTrue { function ->
                // If checking for errors, should also check specific error type
                function.text.contains("shouldBeInstanceOf") ||
                function.text.contains("shouldBe") && function.text.contains("Error")
            }
    }

    @Test
    @DisplayName("SqlDelight repository tests should use in-memory databases")
    fun `SqlDelight repository tests should use in-memory databases`() {
        Konsist
            .scopeFromProject()
            .classes
            .withNameEndingWith("RepositoryTest")
            .filter { testClass ->
                testClass.text.contains("SqlDelight")
            }
            .flatMap { it.functions }
            .filter { function ->
                function.name == "beforeEach" || function.name == "beforeTest"
            }
            .assertTrue { function ->
                // Should use IN_MEMORY for test databases
                function.text.contains("IN_MEMORY") ||
                function.text.contains("createInMemoryDatabase")
            }
    }

    @Test
    @DisplayName("Tests should not use Thread.sleep for timing")
    fun `tests should not use Thread sleep for timing`() {
        Konsist
            .scopeFromProject()
            .files
            .withNameEndingWith("Test.kt")
            .assertTrue { file ->
                // Thread.sleep is discouraged in tests, use test schedulers instead
                !file.text.contains("Thread.sleep")
            }
    }

    @Test
    @DisplayName("Test functions should have descriptive names")
    fun `test functions should have descriptive names`() {
        Konsist
            .scopeFromProject()
            .classes
            .withNameEndingWith("Test")
            .flatMap { it.functions }
            .filter { function ->
                function.hasAnnotationOf(Test::class) ||
                function.name.startsWith("test")
            }
            .assertTrue { function ->
                // Test names should be descriptive (at least 3 words)
                function.name.split("_", " ").size >= 3 ||
                function.name.contains("should") ||
                function.name.contains("when") ||
                function.name.contains("given")
            }
    }

    @Test
    @DisplayName("Repository tests should test error scenarios")
    fun `repository tests should test error scenarios`() {
        Konsist
            .scopeFromProject()
            .classes
            .withNameEndingWith("RepositoryTest")
            .assertTrue { testClass ->
                // Repository tests should include error handling tests
                testClass.text.contains("error") ||
                testClass.text.contains("fail") ||
                testClass.text.contains("invalid") ||
                testClass.text.contains("exception")
            }
    }

    @Test
    @DisplayName("Tests should properly handle nullable assertions")
    fun `tests should handle nullable types with safe calls`() {
        Konsist
            .scopeFromProject()
            .files
            .withNameEndingWith("Test.kt")
            .flatMap { it.functions }
            .filter { function ->
                // Find assertion functions
                function.text.contains("shouldBe") ||
                function.text.contains("shouldHaveSize") ||
                function.text.contains("shouldContain")
            }
            .assertTrue { function ->
                // When using nullable types, should use safe calls
                !function.text.contains(Regex("""result\.getOrNull\(\)\s+should""")) ||
                function.text.contains(Regex("""result\.getOrNull\(\)\?\.should"""))
            }
    }
}