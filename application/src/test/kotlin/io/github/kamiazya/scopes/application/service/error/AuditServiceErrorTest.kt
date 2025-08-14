package io.github.kamiazya.scopes.application.service.error

import kotlinx.datetime.Instant

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf

/**
 * Test for AuditServiceError hierarchy.
 * 
 * This test validates audit service errors for audit trail management,
 * event logging, and compliance tracking failures.
 * 
 * Based on Serena MCP research on audit patterns:
 * - Immutable audit trail management
 * - Event sourcing error handling
 * - Compliance and regulatory requirements
 * - Audit log integrity and security
 */
class AuditServiceErrorTest : DescribeSpec({

    describe("AuditServiceError hierarchy") {

        describe("AuditTrailError") {
            it("should create AuditLogCorruption with integrity details") {
                val error = AuditTrailError.AuditLogCorruption(
                    auditEntryId = "audit-123",
                    detectedAt = Instant.fromEpochMilliseconds(1640995200000L),
                    corruptionType = "HASH_MISMATCH",
                    affectedEntries = listOf("audit-121", "audit-122", "audit-123"),
                    integrityHash = "abc123def456"
                )

                error.auditEntryId shouldBe "audit-123"
                error.detectedAt shouldBe Instant.fromEpochMilliseconds(1640995200000L)
                error.corruptionType shouldBe "HASH_MISMATCH"
                error.affectedEntries shouldBe listOf("audit-121", "audit-122", "audit-123")
                error.integrityHash shouldBe "abc123def456"
            }

            it("should create AuditEntryNotFound") {
                val error = AuditTrailError.AuditEntryNotFound(
                    auditEntryId = "audit-999",
                    requestedBy = "compliance-officer",
                    searchCriteria = mapOf("userId" to "user-123", "action" to "DELETE")
                )

                error.auditEntryId shouldBe "audit-999"
                error.requestedBy shouldBe "compliance-officer"
                error.searchCriteria shouldBe mapOf("userId" to "user-123", "action" to "DELETE")
            }

            it("should create ImmutabilityViolation") {
                val error = AuditTrailError.ImmutabilityViolation(
                    auditEntryId = "audit-456",
                    attemptedOperation = "UPDATE",
                    attemptedBy = "malicious-user",
                    detectedAt = Instant.fromEpochMilliseconds(1640995300000L),
                    originalCreatedAt = Instant.fromEpochMilliseconds(1640994000000L)
                )

                error.auditEntryId shouldBe "audit-456"
                error.attemptedOperation shouldBe "UPDATE"
                error.attemptedBy shouldBe "malicious-user"
                error.detectedAt shouldBe Instant.fromEpochMilliseconds(1640995300000L)
                error.originalCreatedAt shouldBe Instant.fromEpochMilliseconds(1640994000000L)
            }
        }

        describe("EventLoggingError") {
            it("should create EventSerializationFailure") {
                val error = EventLoggingError.EventSerializationFailure(
                    eventId = "event-789",
                    eventType = "ScopeDeleted",
                    serializationError = "Cannot serialize field: metadata",
                    timestamp = Instant.fromEpochMilliseconds(1640995400000L)
                )

                error.eventId shouldBe "event-789"
                error.eventType shouldBe "ScopeDeleted"
                error.serializationError shouldBe "Cannot serialize field: metadata"
                error.timestamp shouldBe Instant.fromEpochMilliseconds(1640995400000L)
            }

            it("should create EventStorageFailure") {
                val error = EventLoggingError.EventStorageFailure(
                    eventId = "event-012",
                    storageLocation = "/var/audit/2024/01/",
                    cause = RuntimeException("Disk full"),
                    retryAttempts = 3
                )

                error.eventId shouldBe "event-012"
                error.storageLocation shouldBe "/var/audit/2024/01/"
                error.cause.message shouldBe "Disk full"
                error.retryAttempts shouldBe 3
            }

            it("should create EventOrderingViolation") {
                val error = EventLoggingError.EventOrderingViolation(
                    eventId = "event-345",
                    expectedSequence = 100,
                    actualSequence = 98,
                    aggregateId = "scope-123"
                )

                error.eventId shouldBe "event-345"
                error.expectedSequence shouldBe 100
                error.actualSequence shouldBe 98
                error.aggregateId shouldBe "scope-123"
            }
        }

        describe("ComplianceError") {
            it("should create RetentionPolicyViolation") {
                val error = ComplianceError.RetentionPolicyViolation(
                    policyId = "gdpr-retention-policy",
                    violationType = "RETENTION_PERIOD_EXCEEDED",
                    affectedRecords = 1500,
                    policyDetails = "Data must be deleted after 3 years",
                    detectedAt = Instant.fromEpochMilliseconds(1640995500000L)
                )

                error.policyId shouldBe "gdpr-retention-policy"
                error.violationType shouldBe "RETENTION_PERIOD_EXCEEDED"
                error.affectedRecords shouldBe 1500
                error.policyDetails shouldBe "Data must be deleted after 3 years"
                error.detectedAt shouldBe Instant.fromEpochMilliseconds(1640995500000L)
            }

            it("should create DataClassificationError") {
                val error = ComplianceError.DataClassificationError(
                    dataId = "scope-sensitive-data",
                    expectedClassification = "CONFIDENTIAL",
                    actualClassification = "PUBLIC",
                    complianceRegulation = "GDPR"
                )

                error.dataId shouldBe "scope-sensitive-data"
                error.expectedClassification shouldBe "CONFIDENTIAL"
                error.actualClassification shouldBe "PUBLIC"
                error.complianceRegulation shouldBe "GDPR"
            }

            it("should create AuditRequirementNotMet") {
                val error = ComplianceError.AuditRequirementNotMet(
                    requirementId = "pci-dss-req-10",
                    description = "All user access must be logged",
                    missingAudits = listOf("user-login-123", "user-access-456"),
                    complianceFramework = "PCI-DSS"
                )

                error.requirementId shouldBe "pci-dss-req-10"
                error.description shouldBe "All user access must be logged"
                error.missingAudits shouldBe listOf("user-login-123", "user-access-456")
                error.complianceFramework shouldBe "PCI-DSS"
            }
        }

        describe("AuditSystemError") {
            it("should create AuditSystemUnavailable") {
                val error = AuditSystemError.AuditSystemUnavailable(
                    subsystem = "event-store",
                    cause = RuntimeException("Database connection failed"),
                    estimatedRecoveryAt = Instant.fromEpochMilliseconds(3600000L), // 1 hour
                    impactLevel = "HIGH"
                )

                error.subsystem shouldBe "event-store"
                error.cause.message shouldBe "Database connection failed"
                error.estimatedRecoveryAt shouldBe Instant.fromEpochMilliseconds(3600000L)
                error.impactLevel shouldBe "HIGH"
            }

            it("should create ConfigurationError") {
                val error = AuditSystemError.ConfigurationError(
                    configurationKey = "audit.storage.encryption.key",
                    errorDetails = "Encryption key not found in keystore",
                    service = "audit-service"
                )

                error.configurationKey shouldBe "audit.storage.encryption.key"
                error.errorDetails shouldBe "Encryption key not found in keystore"
                error.service shouldBe "audit-service"
            }
        }

        describe("error hierarchy") {
            it("all errors should extend AuditServiceError") {
                val trailError = AuditTrailError.AuditLogCorruption(
                    "id", Instant.fromEpochMilliseconds(1L), "type", emptyList(), "hash"
                )
                val loggingError = EventLoggingError.EventSerializationFailure(
                    "id", "type", "error", Instant.fromEpochMilliseconds(1L)
                )
                val complianceError = ComplianceError.RetentionPolicyViolation(
                    "policy", "type", 0, "details", Instant.fromEpochMilliseconds(1L)
                )
                val systemError = AuditSystemError.AuditSystemUnavailable(
                    "subsystem", RuntimeException(), Instant.fromEpochMilliseconds(1L), "level"
                )

                trailError should beInstanceOf<AuditServiceError>()
                loggingError should beInstanceOf<AuditServiceError>()
                complianceError should beInstanceOf<AuditServiceError>()
                systemError should beInstanceOf<AuditServiceError>()
            }
        }
    }
})