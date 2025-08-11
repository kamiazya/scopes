package io.github.kamiazya.scopes.application.service.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

/**
 * Test class for TemplateError hierarchy.
 *
 * Tests verify that template-specific error types provide appropriate
 * sanitization for potentially sensitive template variable values.
 */
class TemplateErrorTest : DescribeSpec({

    describe("TemplateError hierarchy") {

        describe("TemplateRenderingFailure") {
            it("should have invalidVariableReasons property") {
                val error = TemplateError.TemplateRenderingFailure(
                    templateId = "email-notification",
                    missingVariables = listOf("RECIPIENT"),
                    invalidVariableReasons = mapOf(
                        "API_KEY" to "Invalid format",
                        "PASSWORD" to "Too short"
                    ),
                    cause = null
                )

                error.templateId shouldBe "email-notification"
                error.invalidVariableReasons.size shouldBe 2
                error.invalidVariableReasons["API_KEY"] shouldBe "Invalid format"
                error.invalidVariableReasons["PASSWORD"] shouldBe "Too short"
            }

            it("should provide sanitized reasons") {
                val error = TemplateError.TemplateRenderingFailure(
                    templateId = "welcome-email",
                    missingVariables = listOf("USER_NAME"),
                    invalidVariableReasons = mapOf(
                        "FORMAT" to "Must be a valid email format",
                        "LENGTH" to "Must be between 1 and 100 characters"
                    ),
                    cause = RuntimeException("Template engine error")
                )

                val sanitized = error.sanitizedReasons
                sanitized["FORMAT"] shouldBe "Must be a valid email format"
                sanitized["LENGTH"] shouldBe "Must be between 1 and 100 characters"
            }

            it("should use sanitized reasons in toString") {
                val error = TemplateError.TemplateRenderingFailure(
                    templateId = "password-reset",
                    missingVariables = listOf(),
                    invalidVariableReasons = mapOf(
                        "RESET_TOKEN" to "Invalid token format"
                    ),
                    cause = null
                )

                val toStringResult = error.toString()
                toStringResult.contains("password-reset") shouldBe true
                toStringResult.contains("invalidVariableReasons") shouldBe true
            }
        }

        describe("TemplateNotFound") {
            it("should provide context for missing templates") {
                val error = TemplateError.TemplateNotFound(
                    templateId = "user-welcome",
                    locale = "en_US",
                    searchPaths = listOf("/templates", "/fallback")
                )

                error.templateId shouldBe "user-welcome"
                error.locale shouldBe "en_US"
                error.searchPaths shouldBe listOf("/templates", "/fallback")
            }
        }

        describe("InvalidTemplateSyntax") {
            it("should provide context for syntax errors") {
                val error = TemplateError.InvalidTemplateSyntax(
                    templateId = "malformed-template",
                    syntaxError = "Unclosed mustache tag",
                    lineNumber = 15,
                    columnNumber = 23
                )

                error.templateId shouldBe "malformed-template"
                error.syntaxError shouldBe "Unclosed mustache tag"
                error.lineNumber shouldBe 15
                error.columnNumber shouldBe 23
            }
        }
    }
})
