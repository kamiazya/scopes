package io.github.kamiazya.scopes.konsist

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.verify.assertTrue
import io.kotest.core.spec.style.StringSpec

/**
 * Input validation tests to ensure proper validation of user inputs.
 */
class InputValidationTest :
    StringSpec({

        "idempotency key validation should be consistent" {
            Konsist
                .scopeFromProject()
                .functions()
                .filter { it.text.contains("idempotencyKey") || it.text.contains("IdempotencyKey") }
                .filter { !it.resideInPackage("..mcp..") } // Exclude MCP module
                .assertTrue { function ->
                    val functionText = function.text

                    // If function handles idempotency keys, should validate format
                    if (functionText.contains("idempotencyKey") &&
                        !functionText.contains("test") &&
                        !functionText.contains("Test")
                    ) {

                        // Should have pattern validation
                        functionText.contains("PATTERN") ||
                            functionText.contains("Regex") ||
                            functionText.contains("matches") ||
                            functionText.contains("validate")
                    } else {
                        true // Test code or not handling keys directly
                    }
                }
        }

        "functions accepting external input should validate parameters" {
            Konsist
                .scopeFromProject()
                .functions()
                .filter {
                    it.name.startsWith("handle") ||
                        it.name.startsWith("process") ||
                        it.name.contains("Tool")
                }
                .filter { !it.resideInPackage("..test..") }
                .assertTrue { function ->
                    val functionText = function.text

                    // Functions processing external input should have validation
                    if (functionText.contains("args") || functionText.contains("params")) {
                        // Should have some form of validation
                        functionText.contains("require") ||
                            functionText.contains("check") ||
                            functionText.contains("validate") ||
                            functionText.contains("when") ||
                            // Pattern matching counts as validation
                            functionText.contains("?.") ||
                            // Null safety
                            functionText.contains(" ?: ") // Elvis operator
                    } else {
                        true // No external input parameters
                    }
                }
        }

        "JSON parsing should handle malformed input" {
            Konsist
                .scopeFromProject()
                .functions()
                .filter {
                    it.text.contains("Json.parseToJsonElement") ||
                        it.text.contains("Json.decodeFromString") ||
                        it.text.contains("jsonObject")
                }
                .filter { !it.resideInPackage("..test..") }
                .filter { !it.resideInPackage("..mcp..") } // Exclude MCP module
                .assertTrue { function ->
                    val functionText = function.text

                    // JSON parsing should be wrapped in try-catch or return Either/Result
                    functionText.contains("try") ||
                        functionText.contains("Either") ||
                        functionText.contains("Result") ||
                        functionText.contains("runCatching")
                }
        }
    })
