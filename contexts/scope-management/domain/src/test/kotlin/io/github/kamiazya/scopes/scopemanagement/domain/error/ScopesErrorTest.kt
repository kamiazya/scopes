package io.github.kamiazya.scopes.scopemanagement.domain.error

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for ScopesError base error types and their behavior.
 */
class ScopesErrorTest :
    StringSpec({

        // InvalidOperation error tests
        "should create InvalidOperation error with all parameters" {
            val error = ScopesError.InvalidOperation(
                operation = "deleteScope",
                entityType = "Scope",
                entityId = "scope-123",
                reason = ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE,
            )

            error.operation shouldBe "deleteScope"
            error.entityType shouldBe "Scope"
            error.entityId shouldBe "scope-123"
            error.reason shouldBe ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE
        }

        "should create InvalidOperation error with minimal parameters" {
            val error = ScopesError.InvalidOperation(
                operation = "updateScope",
            )

            error.operation shouldBe "updateScope"
            error.entityType shouldBe null
            error.entityId shouldBe null
            error.reason shouldBe null
        }

        "should support all InvalidOperationReason types" {
            val reasons = listOf(
                ScopesError.InvalidOperation.InvalidOperationReason.INVALID_STATE,
                ScopesError.InvalidOperation.InvalidOperationReason.OPERATION_NOT_ALLOWED,
                ScopesError.InvalidOperation.InvalidOperationReason.MISSING_PREREQUISITE,
                ScopesError.InvalidOperation.InvalidOperationReason.INVALID_INPUT,
            )

            reasons.forEach { reason ->
                val error = ScopesError.InvalidOperation(
                    operation = "test",
                    reason = reason,
                )
                error.reason shouldBe reason
            }
        }

        // AlreadyExists error tests
        "should create AlreadyExists error with default identifier type" {
            val error = ScopesError.AlreadyExists(
                entityType = "Scope",
                identifier = "my-project",
            )

            error.entityType shouldBe "Scope"
            error.identifier shouldBe "my-project"
            error.identifierType shouldBe "key"
        }

        "should create AlreadyExists error with custom identifier type" {
            val error = ScopesError.AlreadyExists(
                entityType = "Alias",
                identifier = "project-alpha",
                identifierType = "alias",
            )

            error.entityType shouldBe "Alias"
            error.identifier shouldBe "project-alpha"
            error.identifierType shouldBe "alias"
        }

        // NotFound error tests
        "should create NotFound error with default identifier type" {
            val error = ScopesError.NotFound(
                entityType = "Scope",
                identifier = "non-existent",
            )

            error.entityType shouldBe "Scope"
            error.identifier shouldBe "non-existent"
            error.identifierType shouldBe "key"
        }

        "should create NotFound error with custom identifier type" {
            val error = ScopesError.NotFound(
                entityType = "AspectDefinition",
                identifier = "priority",
                identifierType = "aspectKey",
            )

            error.entityType shouldBe "AspectDefinition"
            error.identifier shouldBe "priority"
            error.identifierType shouldBe "aspectKey"
        }

        // SystemError tests
        "should create SystemError with all parameters" {
            val context = mapOf("retryCount" to 3, "lastError" to "Connection timeout")
            val error = ScopesError.SystemError(
                errorType = ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE,
                service = "database",
                context = context,
            )

            error.errorType shouldBe ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE
            error.service shouldBe "database"
            error.context shouldBe context
        }

        "should create SystemError with minimal parameters" {
            val error = ScopesError.SystemError(
                errorType = ScopesError.SystemError.SystemErrorType.SERIALIZATION_FAILED,
            )

            error.errorType shouldBe ScopesError.SystemError.SystemErrorType.SERIALIZATION_FAILED
            error.service shouldBe null
            error.context shouldBe emptyMap()
        }

        "should support all SystemErrorType values" {
            val errorTypes = listOf(
                ScopesError.SystemError.SystemErrorType.SERVICE_UNAVAILABLE,
                ScopesError.SystemError.SystemErrorType.SERIALIZATION_FAILED,
                ScopesError.SystemError.SystemErrorType.DESERIALIZATION_FAILED,
                ScopesError.SystemError.SystemErrorType.QUERY_TIMEOUT,
                ScopesError.SystemError.SystemErrorType.CAPACITY_EXCEEDED,
                ScopesError.SystemError.SystemErrorType.ACCESS_DENIED,
                ScopesError.SystemError.SystemErrorType.CONFIGURATION_ERROR,
                ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
            )

            errorTypes.forEach { errorType ->
                val error = ScopesError.SystemError(errorType = errorType)
                error.errorType shouldBe errorType
            }
        }

        // ValidationFailed error tests
        "should create ValidationFailed error with InvalidType constraint" {
            val constraint = ScopesError.ValidationConstraintType.InvalidType(
                expectedType = "String",
                actualType = "Number",
            )
            val error = ScopesError.ValidationFailed(
                field = "priority",
                value = "123",
                constraint = constraint,
            )

            error.field shouldBe "priority"
            error.value shouldBe "123"
            val invalidType = error.constraint.shouldBeInstanceOf<ScopesError.ValidationConstraintType.InvalidType>()
            invalidType.expectedType shouldBe "String"
            invalidType.actualType shouldBe "Number"
        }

        "should create ValidationFailed error with NotInAllowedValues constraint" {
            val constraint = ScopesError.ValidationConstraintType.NotInAllowedValues(
                allowedValues = listOf("low", "medium", "high"),
            )
            val error = ScopesError.ValidationFailed(
                field = "priority",
                value = "urgent",
                constraint = constraint,
            )

            error.field shouldBe "priority"
            error.value shouldBe "urgent"
            val notAllowed = error.constraint.shouldBeInstanceOf<ScopesError.ValidationConstraintType.NotInAllowedValues>()
            notAllowed.allowedValues shouldBe listOf("low", "medium", "high")
        }

        "should create ValidationFailed error with details" {
            val details = mapOf("maxLength" to 100, "actualLength" to 150)
            val constraint = ScopesError.ValidationConstraintType.InvalidValue(
                reason = "Title too long",
            )
            val error = ScopesError.ValidationFailed(
                field = "title",
                value = "A very long title...",
                constraint = constraint,
                details = details,
            )

            error.details shouldBe details
        }

        // Conflict error tests
        "should create Conflict error with all parameters" {
            val details = mapOf("dependentCount" to 5)
            val error = ScopesError.Conflict(
                resourceType = "Scope",
                resourceId = "scope-123",
                conflictType = ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES,
                details = details,
            )

            error.resourceType shouldBe "Scope"
            error.resourceId shouldBe "scope-123"
            error.conflictType shouldBe ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES
            error.details shouldBe details
        }

        "should support all ConflictType values" {
            val conflictTypes = listOf(
                ScopesError.Conflict.ConflictType.ALREADY_IN_USE,
                ScopesError.Conflict.ConflictType.HAS_DEPENDENCIES,
                ScopesError.Conflict.ConflictType.DUPLICATE_KEY,
                ScopesError.Conflict.ConflictType.OPTIMISTIC_LOCK_FAILURE,
            )

            conflictTypes.forEach { conflictType ->
                val error = ScopesError.Conflict(
                    resourceType = "Resource",
                    resourceId = "id",
                    conflictType = conflictType,
                )
                error.conflictType shouldBe conflictType
            }
        }

        // ConcurrencyError tests
        "should create ConcurrencyError with all parameters" {
            val error = ScopesError.ConcurrencyError(
                aggregateId = "scope-123",
                aggregateType = "ScopeAggregate",
                expectedVersion = 5,
                actualVersion = 7,
                operation = "updateTitle",
            )

            error.aggregateId shouldBe "scope-123"
            error.aggregateType shouldBe "ScopeAggregate"
            error.expectedVersion shouldBe 5
            error.actualVersion shouldBe 7
            error.operation shouldBe "updateTitle"
        }

        "should create ConcurrencyError with minimal parameters" {
            val error = ScopesError.ConcurrencyError(
                aggregateId = "scope-456",
                aggregateType = "ScopeAggregate",
            )

            error.aggregateId shouldBe "scope-456"
            error.aggregateType shouldBe "ScopeAggregate"
            error.expectedVersion shouldBe null
            error.actualVersion shouldBe null
            error.operation shouldBe null
        }

        // RepositoryError tests
        "should create RepositoryError with all parameters" {
            val details = mapOf("query" to "SELECT * FROM scopes", "timeout" to 30)
            val error = ScopesError.RepositoryError(
                repositoryName = "ScopeRepository",
                operation = ScopesError.RepositoryError.RepositoryOperation.QUERY,
                entityType = "Scope",
                entityId = "scope-123",
                failure = ScopesError.RepositoryError.RepositoryFailure.TIMEOUT,
                details = details,
            )

            error.repositoryName shouldBe "ScopeRepository"
            error.operation shouldBe ScopesError.RepositoryError.RepositoryOperation.QUERY
            error.entityType shouldBe "Scope"
            error.entityId shouldBe "scope-123"
            error.failure shouldBe ScopesError.RepositoryError.RepositoryFailure.TIMEOUT
            error.details shouldBe details
        }

        "should support all RepositoryOperation values" {
            val operations = listOf(
                ScopesError.RepositoryError.RepositoryOperation.SAVE,
                ScopesError.RepositoryError.RepositoryOperation.FIND,
                ScopesError.RepositoryError.RepositoryOperation.DELETE,
                ScopesError.RepositoryError.RepositoryOperation.UPDATE,
                ScopesError.RepositoryError.RepositoryOperation.QUERY,
                ScopesError.RepositoryError.RepositoryOperation.COUNT,
            )

            operations.forEach { operation ->
                val error = ScopesError.RepositoryError(
                    repositoryName = "TestRepo",
                    operation = operation,
                )
                error.operation shouldBe operation
            }
        }

        "should support all RepositoryFailure values" {
            val failures = listOf(
                ScopesError.RepositoryError.RepositoryFailure.STORAGE_UNAVAILABLE,
                ScopesError.RepositoryError.RepositoryFailure.CONSTRAINT_VIOLATION,
                ScopesError.RepositoryError.RepositoryFailure.TIMEOUT,
                ScopesError.RepositoryError.RepositoryFailure.ACCESS_DENIED,
                ScopesError.RepositoryError.RepositoryFailure.OPERATION_FAILED,
                ScopesError.RepositoryError.RepositoryFailure.CORRUPTED_DATA,
            )

            failures.forEach { failure ->
                val error = ScopesError.RepositoryError(
                    repositoryName = "TestRepo",
                    operation = ScopesError.RepositoryError.RepositoryOperation.FIND,
                    failure = failure,
                )
                error.failure shouldBe failure
            }
        }

        // ScopeStatusTransitionError tests
        "should create ScopeStatusTransitionError" {
            val error = ScopesError.ScopeStatusTransitionError(
                from = "TODO",
                to = "DONE",
                reason = "Cannot transition directly from TODO to DONE without going through IN_PROGRESS",
            )

            error.from shouldBe "TODO"
            error.to shouldBe "DONE"
            error.reason shouldBe "Cannot transition directly from TODO to DONE without going through IN_PROGRESS"
        }

        // Error hierarchy tests
        "should maintain proper inheritance hierarchy" {
            val errors: List<ScopesError> = listOf(
                ScopesError.InvalidOperation("test"),
                ScopesError.AlreadyExists("Type", "id"),
                ScopesError.NotFound("Type", "id"),
                ScopesError.SystemError(ScopesError.SystemError.SystemErrorType.SERIALIZATION_FAILED),
                ScopesError.ValidationFailed("field", "value", ScopesError.ValidationConstraintType.InvalidValue("reason")),
                ScopesError.Conflict("Resource", "id", ScopesError.Conflict.ConflictType.DUPLICATE_KEY),
                ScopesError.ConcurrencyError("id", "type"),
                ScopesError.RepositoryError("repo", ScopesError.RepositoryError.RepositoryOperation.SAVE),
                ScopesError.ScopeStatusTransitionError("from", "to", "reason"),
            )

            errors.forEach { error ->
                error.shouldBeInstanceOf<ScopesError>()
            }
        }

        // Error equality and data class behavior
        "should properly implement equals and hashCode for data classes" {
            val error1 = ScopesError.NotFound("Scope", "scope-123", "id")
            val error2 = ScopesError.NotFound("Scope", "scope-123", "id")
            val error3 = ScopesError.NotFound("Scope", "scope-456", "id")

            error1 shouldBe error2
            error1.hashCode() shouldBe error2.hashCode()
            error1 shouldNotBe error3
            error1.hashCode() shouldNotBe error3.hashCode()
        }
    })
