package io.github.kamiazya.scopes.infrastructure.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock

/**
 * Test class for InfrastructureAdapterError hierarchy.
 *
 * Tests verify that adapter-specific error types provide appropriate
 * context and maintain proper inheritance relationships.
 */
class InfrastructureAdapterErrorTest : DescribeSpec({

    describe("InfrastructureAdapterError hierarchy") {

        describe("DatabaseAdapterError") {
            it("should provide context for connection errors") {
                val error = DatabaseAdapterError.ConnectionError(
                    connectionString = "jdbc:postgresql://localhost:5432/test",
                    poolSize = 10,
                    activeConnections = 8,
                    cause = RuntimeException("Connection refused"),
                    timestamp = Clock.System.now()
                )

                error.shouldBeInstanceOf<DatabaseAdapterError>()
                error.shouldBeInstanceOf<InfrastructureAdapterError>()
                error.connectionString shouldBe "jdbc:postgresql://localhost:5432/test"
                error.poolSize shouldBe 10
                error.retryable shouldBe true
            }

            it("should provide context for query errors") {
                val error = DatabaseAdapterError.QueryError(
                    query = "SELECT * FROM scopes",
                    parameters = mapOf("id" to "123"),
                    executionTimeMs = 5000L,
                    cause = RuntimeException("Syntax error"),
                    timestamp = Clock.System.now()
                )

                error.query shouldBe "SELECT * FROM scopes"
                error.parameters shouldBe mapOf("id" to "123")
                error.executionTimeMs shouldBe 5000L
            }
        }

        describe("ExternalApiAdapterError") {
            it("should provide context for network errors") {
                val error = ExternalApiAdapterError.NetworkError(
                    endpoint = "https://api.example.com",
                    errorType = NetworkErrorType.CONNECTION_TIMEOUT,
                    cause = RuntimeException("Network timeout"),
                    timestamp = Clock.System.now()
                )

                error.shouldBeInstanceOf<ExternalApiAdapterError>()
                error.endpoint shouldBe "https://api.example.com"
                error.errorType shouldBe NetworkErrorType.CONNECTION_TIMEOUT
                error.retryable shouldBe true
            }

            it("should provide context for HTTP errors") {
                val error = ExternalApiAdapterError.HttpError(
                    endpoint = "https://api.example.com",
                    statusCode = 503,
                    timestamp = Clock.System.now()
                )

                error.statusCode shouldBe 503
                error.retryable shouldBe true
            }
        }

        describe("ConfigurationAdapterError") {
            it("should redact sensitive values in ValidationError") {
                val error = ConfigurationAdapterError.ValidationError(
                    configKey = "database.password",
                    expectedType = "String",
                    actualValue = "supersecret123",
                    validationRules = listOf("min_length:8", "contains_special_chars"),
                    timestamp = Clock.System.now()
                )

                error.shouldBeInstanceOf<ConfigurationAdapterError>()
                error.shouldBeInstanceOf<InfrastructureAdapterError>()
                error.configKey shouldBe "database.password"
                error.expectedType shouldBe "String"
                error.actualValueRedacted shouldBe "su**********23"
                error.retryable shouldBe false
            }

            it("should handle null actual values safely") {
                val error = ConfigurationAdapterError.ValidationError(
                    configKey = "optional.setting",
                    expectedType = "String",
                    actualValue = null,
                    validationRules = listOf("required"),
                    timestamp = Clock.System.now()
                )

                error.actualValueRedacted shouldBe "<null>"
            }

            it("should handle blank actual values safely") {
                val error = ConfigurationAdapterError.ValidationError(
                    configKey = "required.setting",
                    expectedType = "String",
                    actualValue = "   ",
                    validationRules = listOf("not_blank"),
                    timestamp = Clock.System.now()
                )

                error.actualValueRedacted shouldBe "<blank>"
            }

            it("should handle short values by masking completely") {
                val error = ConfigurationAdapterError.ValidationError(
                    configKey = "short.key",
                    expectedType = "String",
                    actualValue = "abc",
                    validationRules = listOf("min_length:5"),
                    timestamp = Clock.System.now()
                )

                error.actualValueRedacted shouldBe "***"
            }

            it("should never expose sensitive data in toString") {
                val error = ConfigurationAdapterError.ValidationError(
                    configKey = "api.secret",
                    expectedType = "String",
                    actualValue = "sk_live_secretkey123456",
                    validationRules = listOf("starts_with:sk_"),
                    timestamp = Clock.System.now(),
                    correlationId = "req-123"
                )

                val toStringResult = error.toString()
                toStringResult.contains("sk_live_secretkey123456") shouldBe false
                toStringResult.contains("sk*******************56") shouldBe true
                toStringResult.contains("api.secret") shouldBe true
            }

            it("should exclude sensitive data from equals comparison") {
                val timestamp = Clock.System.now()
                val error1 = ConfigurationAdapterError.ValidationError(
                    configKey = "api.key",
                    expectedType = "String",
                    actualValue = "secret1",
                    validationRules = listOf("required"),
                    timestamp = timestamp
                )

                val error2 = ConfigurationAdapterError.ValidationError(
                    configKey = "api.key",
                    expectedType = "String",
                    actualValue = "differentsecret",
                    validationRules = listOf("required"),
                    timestamp = timestamp
                )

                // Should be equal despite different actualValue
                error1 shouldBe error2
            }

            it("should exclude sensitive data from hashCode") {
                val timestamp = Clock.System.now()
                val error1 = ConfigurationAdapterError.ValidationError(
                    configKey = "api.key",
                    expectedType = "String",
                    actualValue = "secret1",
                    validationRules = listOf("required"),
                    timestamp = timestamp
                )

                val error2 = ConfigurationAdapterError.ValidationError(
                    configKey = "api.key",
                    expectedType = "String",
                    actualValue = "differentsecret",
                    validationRules = listOf("required"),
                    timestamp = timestamp
                )

                // Should have same hashCode despite different actualValue
                error1.hashCode() shouldBe error2.hashCode()
            }

            it("should have invalidVariableReasons property in EnvironmentError") {
                val error = ConfigurationAdapterError.EnvironmentError(
                    environment = "production",
                    missingVariables = listOf("REQUIRED_VAR"),
                    invalidVariableReasons = mapOf(
                        "SECRET_KEY" to "Invalid format",
                        "API_TOKEN" to "Missing prefix"
                    ),
                    timestamp = Clock.System.now()
                )

                error.environment shouldBe "production"
                error.invalidVariableReasons.size shouldBe 2
                error.invalidVariableReasons["SECRET_KEY"] shouldBe "Invalid format"
                error.invalidVariableReasons["API_TOKEN"] shouldBe "Missing prefix"
            }
        }

        describe("InfrastructureAdapterError base class") {
            it("should be a sealed class hierarchy") {
                val error = DatabaseAdapterError.ConnectionError(
                    connectionString = "jdbc:test",
                    poolSize = null,
                    activeConnections = null,
                    cause = RuntimeException("test"),
                    timestamp = Clock.System.now()
                )

                error.shouldBeInstanceOf<InfrastructureAdapterError>()
            }
        }
    }
})
