package io.github.kamiazya.scopes.infrastructure.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.string.contain
import kotlinx.datetime.Clock

/**
 * Test configuration error sanitization
 */
class ConfigurationErrorSanitizationTest : DescribeSpec({

    describe("Environment error sanitization") {
        it("should sanitize sensitive values in error reasons") {
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
            
            // Assert sanitized values follow the masking pattern: first 2 + stars + last 2
            sanitized["SECRET_KEY"] shouldBe "Expected format 'sk_xxx' but got 'se********56'"
            sanitized["API_TOKEN"] shouldBe "Invalid token format, got 'li*****************56'"
            sanitized["DATABASE_URL"] shouldBe "Expected postgres:// but got 'po************************************db'"

            // Assert raw secret values are not present in sanitized reasons
            sanitized["SECRET_KEY"] shouldNot contain("secret123456")
            sanitized["API_TOKEN"] shouldNot contain("live_api_abc123def456")
            sanitized["DATABASE_URL"] shouldNot contain("postgres://user:password123@host:5432/db")

            // Assert toString() does not contain raw secret values
            val toStringResult = error.toString()
            toStringResult shouldNot contain("secret123456")
            toStringResult shouldNot contain("live_api_abc123def456") 
            toStringResult shouldNot contain("postgres://user:password123@host:5432/db")
        }
    }
})
