package io.github.kamiazya.scopes.infrastructure.error

import io.kotest.core.spec.style.DescribeSpec
import kotlinx.datetime.Clock

/**
 * Debugging test to see actual outputs
 */
class DebugTest : DescribeSpec({

    describe("Debug sanitization") {
        it("should show actual outputs v2") {
            val error = ConfigurationAdapterError.EnvironmentError(
                environment = "production",
                missingVariables = listOf("REQUIRED_VAR"),
                invalidVariableReasons = mapOf(
                    "SECRET_KEY" to "Expected format 'sk_xxx' but got 'secret123456'",
                    "API_TOKEN" to "Invalid token format, got 'live_api_abc123def456'",
                    "DATABASE_URL" to "Expected postgres:// but got 'postgres://user:password123@host:5432/db'"
                ),
                timestamp = Clock.System.now()
            )

            val sanitized = error.sanitizedReasons
            println("SECRET_KEY: ${sanitized["SECRET_KEY"]}")
            println("API_TOKEN: ${sanitized["API_TOKEN"]}")
            println("DATABASE_URL: ${sanitized["DATABASE_URL"]}")

            val toStringResult = error.toString()
            println("toString: $toStringResult")
        }
    }
})
