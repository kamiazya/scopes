package io.github.kamiazya.scopes.application.service.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf

/**
 * Test for NotificationServiceError hierarchy.
 * 
 * This test validates notification service errors for message delivery,
 * event distribution, and communication failures.
 * 
 * Based on Serena MCP research on notification patterns:
 * - Message delivery failure handling
 * - Event sourcing integration errors
 * - Asynchronous communication failures
 * - Retry and failure recovery patterns
 */
class NotificationServiceErrorTest : DescribeSpec({

    describe("NotificationServiceError hierarchy") {

        describe("MessageDeliveryError") {
            it("should create DeliveryFailure") {
                val error = MessageDeliveryError.DeliveryFailure(
                    messageId = "msg-123",
                    channel = "EMAIL",
                    recipient = "user@example.com",
                    attemptCount = 3,
                    lastError = "SMTP connection timeout"
                )

                error.messageId shouldBe "msg-123"
                error.channel shouldBe "EMAIL"
                error.recipient shouldBe "user@example.com"
                error.attemptCount shouldBe 3
                error.lastError shouldBe "SMTP connection timeout"
            }

            it("should create ChannelUnavailable") {
                val error = MessageDeliveryError.ChannelUnavailable(
                    channel = "SLACK",
                    reason = "API temporarily down",
                    expectedRecoveryTime = 3600000L,
                    alternativeChannels = listOf("EMAIL")
                )

                error.channel shouldBe "SLACK"
                error.reason shouldBe "API temporarily down"
                error.expectedRecoveryTime shouldBe 3600000L
                error.alternativeChannels shouldBe listOf("EMAIL")
            }

            it("should create RateLimitExceeded") {
                val error = MessageDeliveryError.RateLimitExceeded(
                    channel = "SMS",
                    limit = 100,
                    windowSeconds = 3600,
                    resetTime = 1640999200000L
                )

                error.channel shouldBe "SMS"
                error.limit shouldBe 100
                error.windowSeconds shouldBe 3600
                error.resetTime shouldBe 1640999200000L
            }
        }

        describe("error hierarchy") {
            it("all errors should extend NotificationServiceError") {
                val deliveryError = MessageDeliveryError.DeliveryFailure(
                    messageId = "test",
                    channel = "test",
                    recipient = "test",
                    attemptCount = 1,
                    lastError = "test"
                )

                deliveryError should beInstanceOf<NotificationServiceError>()
            }
        }
    }
})