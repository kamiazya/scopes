package io.github.kamiazya.scopes.application.service.error

import kotlinx.datetime.Instant

/**
 * Audit service errors for audit trail management and compliance failures.
 *
 * This hierarchy provides comprehensive error types for audit concerns
 * including audit trail integrity, event logging, compliance violations, and system failures.
 *
 * Based on Serena MCP research on audit patterns:
 * - Immutable audit trail management with integrity verification
 * - Event sourcing error handling with ordering guarantees
 * - Compliance and regulatory requirement violations
 * - Audit log security and tamper detection
 *
 * Following functional error handling principles for audit integrity and compliance.
 */
sealed class AuditServiceError

/**
 * Audit trail integrity errors for audit log management failures.
 * These represent violations of audit trail immutability and integrity.
 */
sealed class AuditTrailError : AuditServiceError() {

    /**
     * Audit log corruption detected through integrity checks.
     */
    data class AuditLogCorruption(
        val auditEntryId: String,
        val detectedAt: Instant,
        val corruptionType: String,
        val affectedEntries: List<String>,
        val integrityHash: String
    ) : AuditTrailError()

    /**
     * Required audit entry cannot be found.
     */
    data class AuditEntryNotFound(
        val auditEntryId: String,
        val requestedBy: String,
        val searchCriteria: Map<String, String>
    ) : AuditTrailError()

    /**
     * Attempt to modify immutable audit entry detected.
     */
    data class ImmutabilityViolation(
        val auditEntryId: String,
        val attemptedOperation: String,
        val attemptedBy: String,
        val detectedAt: Instant,
        val originalCreatedAt: Instant
    ) : AuditTrailError()
}

/**
 * Event logging errors for domain event audit recording failures.
 * These handle failures in capturing and storing audit events.
 */
sealed class EventLoggingError : AuditServiceError() {

    /**
     * Failed to serialize event for audit logging.
     */
    data class EventSerializationFailure(
        val eventId: String,
        val eventType: String,
        val serializationError: String,
        val timestamp: Instant
    ) : EventLoggingError()

    /**
     * Failed to store audit event to persistent storage.
     */
    data class EventStorageFailure(
        val eventId: String,
        val storageLocation: String,
        val cause: Throwable,
        val retryAttempts: Int
    ) : EventLoggingError()

    /**
     * Event ordering violation in audit sequence.
     */
    data class EventOrderingViolation(
        val eventId: String,
        val expectedSequence: Int,
        val actualSequence: Int,
        val aggregateId: String
    ) : EventLoggingError()
}

/**
 * Compliance errors for regulatory and policy violations.
 * These handle failures to meet compliance and regulatory requirements.
 */
sealed class ComplianceError : AuditServiceError() {

    /**
     * Data retention policy violation detected.
     */
    data class RetentionPolicyViolation(
        val policyId: String,
        val violationType: String,
        val affectedRecords: Int,
        val policyDetails: String,
        val detectedAt: Instant
    ) : ComplianceError()

    /**
     * Data classification error for sensitive information.
     */
    data class DataClassificationError(
        val dataId: String,
        val expectedClassification: String,
        val actualClassification: String,
        val complianceRegulation: String
    ) : ComplianceError()

    /**
     * Audit requirement not met for compliance framework.
     */
    data class AuditRequirementNotMet(
        val requirementId: String,
        val description: String,
        val missingAudits: List<String>,
        val complianceFramework: String
    ) : ComplianceError()
}

/**
 * System errors for audit service infrastructure failures.
 * These handle technical failures in the audit system itself.
 */
sealed class AuditSystemError : AuditServiceError() {

    /**
     * Audit system or subsystem is unavailable.
     */
    data class AuditSystemUnavailable(
        val subsystem: String,
        val cause: Throwable,
        val estimatedRecoveryAt: Instant,
        val impactLevel: String
    ) : AuditSystemError()

    /**
     * Configuration error in audit system setup.
     */
    data class ConfigurationError(
        val configurationKey: String,
        val errorDetails: String,
        val service: String
    ) : AuditSystemError()
}
