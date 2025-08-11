package io.github.kamiazya.scopes.infrastructure.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes
import io.github.kamiazya.scopes.infrastructure.error.InfrastructureAdapterError
import io.github.kamiazya.scopes.infrastructure.error.DatabaseAdapterError
import io.github.kamiazya.scopes.infrastructure.error.ExternalApiAdapterError
import io.github.kamiazya.scopes.infrastructure.error.FileSystemAdapterError
import io.github.kamiazya.scopes.infrastructure.error.NetworkErrorType

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