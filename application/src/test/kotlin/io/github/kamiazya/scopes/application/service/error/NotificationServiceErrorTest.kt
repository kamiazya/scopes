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
            it("should create DeliveryFailure with specific details") {
                val error = NotificationServiceError.MessageDeliveryError.DeliveryFailure(
                    messageId = "msg-123",
                    recipient = "user@example.com",
                    channel = "EMAIL",
                    attemptCount = 3,
                    lastFailureReason = "SMTP connection timeout"
                )

                error.messageId shouldBe "msg-123"
                error.recipient shouldBe "user@example.com"
                error.channel shouldBe "EMAIL"
                error.attemptCount shouldBe 3
                error.lastFailureReason shouldBe "SMTP connection timeout"
            }

            it("should create InvalidRecipient") {
                val error = NotificationServiceError.MessageDeliveryError.InvalidRecipient(
                    recipient = "invalid-email",
                    channel = "EMAIL",
                    validationError = "Invalid email format",
                    messageId = "msg-456"
                )

                error.recipient shouldBe "invalid-email"
                error.channel shouldBe "EMAIL"
                error.validationError shouldBe "Invalid email format"
                error.messageId shouldBe "msg-456"
            }

            it("should create ChannelUnavailable") {
                val error = NotificationServiceError.MessageDeliveryError.ChannelUnavailable(
                    channel = "SMS",
                    reason = "SMS service is down",
                    estimatedRecoveryTime = 1800000L, // 30 minutes
                    messageId = "msg-789"
                )

                error.channel shouldBe "SMS"
                error.reason shouldBe "SMS service is down"
                error.estimatedRecoveryTime shouldBe 1800000L
                error.messageId shouldBe "msg-789"
            }
        }

        describe("EventDistributionError") {
            it("should create EventPublishingFailure") {
                val error = NotificationServiceError.EventDistributionError.EventPublishingFailure(
                    eventId = "event-123",
                    eventType = "ScopeCreated",
                    targetSubscribers = listOf("audit-service", "analytics-service"),
                    failedSubscribers = listOf("analytics-service"),
                    cause = RuntimeException("Connection refused")
                )

                error.eventId shouldBe "event-123"
                error.eventType shouldBe "ScopeCreated"
                error.targetSubscribers shouldBe listOf("audit-service", "analytics-service")
                error.failedSubscribers shouldBe listOf("analytics-service")
                error.cause.message shouldBe "Connection refused"
            }

            it("should create SubscriberTimeout") {
                val error = NotificationServiceError.EventDistributionError.SubscriberTimeout(
                    subscriberId = "slow-service",
                    eventId = "event-456",
                    timeoutMs = 5000,
                    elapsedMs = 5100
                )

                error.subscriberId shouldBe "slow-service"
                error.eventId shouldBe "event-456"
                error.timeoutMs shouldBe 5000
                error.elapsedMs shouldBe 5100
            }

            it("should create InvalidEventFormat") {
                val error = NotificationServiceError.EventDistributionError.InvalidEventFormat(
                    eventId = "event-789",
                    eventType = "ScopeUpdated",
                    formatError = "Missing required field: aggregateId",
                    subscriberId = "strict-subscriber"
                )

                error.eventId shouldBe "event-789"
                error.eventType shouldBe "ScopeUpdated"
                error.formatError shouldBe "Missing required field: aggregateId"
                error.subscriberId shouldBe "strict-subscriber"
            }
        }

        describe("TemplateError") {
            it("should create TemplateNotFound") {
                val error = NotificationServiceError.TemplateError.TemplateNotFound(
                    templateId = "welcome-email",
                    channel = "EMAIL",
                    availableTemplates = listOf("basic-email", "rich-email"),
                    messageId = "msg-101"
                )

                error.templateId shouldBe "welcome-email"
                error.channel shouldBe "EMAIL"
                error.availableTemplates shouldBe listOf("basic-email", "rich-email")
                error.messageId shouldBe "msg-101"
            }

            it("should create TemplateRenderingFailure") {
                val error = NotificationServiceError.TemplateError.TemplateRenderingFailure(
                    templateId = "scope-notification",
                    messageId = "msg-102",
                    renderingError = "Undefined variable: scopeTitle",
                    templateData = mapOf("scopeId" to "123")
                )

                error.templateId shouldBe "scope-notification"
                error.messageId shouldBe "msg-102"
                error.renderingError shouldBe "Undefined variable: scopeTitle"
                error.templateData shouldBe mapOf("scopeId" to "123")
            }

            it("should create TemplateVersionConflict") {
                val error = NotificationServiceError.TemplateError.TemplateVersionConflict(
                    templateId = "update-notification",
                    requestedVersion = "v2.0",
                    availableVersion = "v1.5",
                    messageId = "msg-103"
                )

                error.templateId shouldBe "update-notification"
                error.requestedVersion shouldBe "v2.0"
                error.availableVersion shouldBe "v1.5"
                error.messageId shouldBe "msg-103"
            }
        }

        describe("ConfigurationError") {
            it("should create InvalidChannelConfiguration") {
                val error = NotificationServiceError.ConfigurationError.InvalidChannelConfiguration(
                    channel = "SLACK",
                    configurationKey = "webhook.url",
                    configurationError = "URL is not reachable",
                    affectedMessages = listOf("msg-201", "msg-202")
                )

                error.channel shouldBe "SLACK"
                error.configurationKey shouldBe "webhook.url"
                error.configurationError shouldBe "URL is not reachable"
                error.affectedMessages shouldBe listOf("msg-201", "msg-202")
            }

            it("should create MissingCredentials") {
                val error = NotificationServiceError.ConfigurationError.MissingCredentials(
                    channel = "EMAIL",
                    credentialType = "SMTP_PASSWORD",
                    service = "notification-service"
                )

                error.channel shouldBe "EMAIL"
                error.credentialType shouldBe "SMTP_PASSWORD"
                error.service shouldBe "notification-service"
            }
        }

        describe("error hierarchy") {
            it("all errors should extend NotificationServiceError") {
                val deliveryError = NotificationServiceError.MessageDeliveryError.DeliveryFailure(
                    "id", "recipient", "channel", 1, "reason"
                )
                val eventError = NotificationServiceError.EventDistributionError.EventPublishingFailure(
                    "id", "type", emptyList(), emptyList(), RuntimeException()
                )
                val templateError = NotificationServiceError.TemplateError.TemplateNotFound(
                    "id", "channel", emptyList(), "msgId"
                )
                val configError = NotificationServiceError.ConfigurationError.MissingCredentials(
                    "channel", "type", "service"
                )

                deliveryError should beInstanceOf<NotificationServiceError>()
                eventError should beInstanceOf<NotificationServiceError>()
                templateError should beInstanceOf<NotificationServiceError>()
                configError should beInstanceOf<NotificationServiceError>()
            }
        }
    }
})