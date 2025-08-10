package io.github.kamiazya.scopes.application.service.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf

/**
 * Test for ApplicationValidationError hierarchy.
 * 
 * This test validates that application-level validation errors provide
 * comprehensive field-level error information for cross-aggregate validations.
 * 
 * Following Serena MCP guidance on validation error patterns in DDD.
 */
class ApplicationValidationErrorTest : DescribeSpec({

    describe("ApplicationValidationError hierarchy") {

        describe("InputValidationError") {
            it("should create InvalidFieldFormat with field details") {
                val error = ApplicationValidationError.InputValidationError.InvalidFieldFormat(
                    field = "email",
                    value = "invalid-email",
                    expectedFormat = "valid email format",
                    validationRule = "RFC 5322 compliant"
                )

                error.field shouldBe "email"
                error.value shouldBe "invalid-email"
                error.expectedFormat shouldBe "valid email format"
                error.validationRule shouldBe "RFC 5322 compliant"
            }

            it("should create MissingRequiredField") {
                val error = ApplicationValidationError.InputValidationError.MissingRequiredField("title")
                
                error.field shouldBe "title"
            }

            it("should create FieldConstraintViolation with specific constraints") {
                val error = ApplicationValidationError.InputValidationError.FieldConstraintViolation(
                    field = "description",
                    constraint = "maxLength",
                    actualValue = "1000",
                    expectedValue = "500"
                )

                error.field shouldBe "description"
                error.constraint shouldBe "maxLength"
                error.actualValue shouldBe "1000"
                error.expectedValue shouldBe "500"
            }
        }

        describe("CrossAggregateValidationError") {
            it("should create AggregateConsistencyViolation") {
                val aggregateIds = setOf("scope-1", "scope-2")
                val error = ApplicationValidationError.CrossAggregateValidationError.AggregateConsistencyViolation(
                    operation = "createChildScope",
                    affectedAggregates = aggregateIds,
                    consistencyRule = "parent-child-hierarchy",
                    violationDetails = "Parent scope is archived"
                )

                error.operation shouldBe "createChildScope"
                error.affectedAggregates shouldBe aggregateIds
                error.consistencyRule shouldBe "parent-child-hierarchy"
                error.violationDetails shouldBe "Parent scope is archived"
            }

            it("should create CrossReferenceViolation") {
                val error = ApplicationValidationError.CrossAggregateValidationError.CrossReferenceViolation(
                    sourceAggregate = "scope-1",
                    targetAggregate = "scope-2", 
                    referenceType = "parentId",
                    violation = "target does not exist"
                )

                error.sourceAggregate shouldBe "scope-1"
                error.targetAggregate shouldBe "scope-2"
                error.referenceType shouldBe "parentId"
                error.violation shouldBe "target does not exist"
            }

            it("should create InvariantViolation across aggregates") {
                val error = ApplicationValidationError.CrossAggregateValidationError.InvariantViolation(
                    invariantName = "uniqueTitleWithinParent",
                    aggregateIds = listOf("parent-1", "child-1", "child-2"),
                    violationDescription = "Duplicate titles found within parent scope"
                )

                error.invariantName shouldBe "uniqueTitleWithinParent"
                error.aggregateIds shouldBe listOf("parent-1", "child-1", "child-2")
                error.violationDescription shouldBe "Duplicate titles found within parent scope"
            }
        }

        describe("BusinessRuleValidationError") {
            it("should create PreconditionViolation") {
                val error = ApplicationValidationError.BusinessRuleValidationError.PreconditionViolation(
                    operation = "archiveScope",
                    precondition = "noActiveChildren",
                    actualCondition = "scope has 3 active children",
                    affectedEntityId = "scope-123"
                )

                error.operation shouldBe "archiveScope"
                error.precondition shouldBe "noActiveChildren"
                error.actualCondition shouldBe "scope has 3 active children"
                error.affectedEntityId shouldBe "scope-123"
            }

            it("should create PostconditionViolation") {
                val error = ApplicationValidationError.BusinessRuleValidationError.PostconditionViolation(
                    operation = "updateScope",
                    postcondition = "parentChildRelationshipMaintained",
                    actualResult = "orphaned child scopes detected",
                    affectedEntityIds = listOf("child-1", "child-2")
                )

                error.operation shouldBe "updateScope"
                error.postcondition shouldBe "parentChildRelationshipMaintained"
                error.actualResult shouldBe "orphaned child scopes detected"
                error.affectedEntityIds shouldBe listOf("child-1", "child-2")
            }
        }

        describe("AsyncValidationError") {
            it("should create ValidationTimeout") {
                val error = ApplicationValidationError.AsyncValidationError.ValidationTimeout(
                    validationType = "uniquenessCheck",
                    timeoutMs = 5000,
                    elapsedMs = 5001
                )

                error.validationType shouldBe "uniquenessCheck"
                error.timeoutMs shouldBe 5000
                error.elapsedMs shouldBe 5001
            }

            it("should create ConcurrentModificationDetected") {
                val error = ApplicationValidationError.AsyncValidationError.ConcurrentModificationDetected(
                    entityId = "scope-123",
                    expectedVersion = 1,
                    actualVersion = 2,
                    operation = "updateTitle"
                )

                error.entityId shouldBe "scope-123"
                error.expectedVersion shouldBe 1
                error.actualVersion shouldBe 2
                error.operation shouldBe "updateTitle"
            }

            it("should create ExternalServiceValidationFailure") {
                val cause = RuntimeException("Connection timeout")
                val error = ApplicationValidationError.AsyncValidationError.ExternalServiceValidationFailure(
                    serviceName = "titleValidationService",
                    validationType = "profanityCheck",
                    cause = cause,
                    retryAttempts = 3
                )

                error.serviceName shouldBe "titleValidationService"
                error.validationType shouldBe "profanityCheck"
                error.cause shouldBe cause
                error.retryAttempts shouldBe 3
            }
        }

        describe("error hierarchy") {
            it("all errors should extend ApplicationValidationError") {
                val inputError = ApplicationValidationError.InputValidationError.MissingRequiredField("field")
                val crossAggregateError = ApplicationValidationError.CrossAggregateValidationError.InvariantViolation(
                    "invariant", listOf("id1"), "violation"
                )
                val businessRuleError = ApplicationValidationError.BusinessRuleValidationError.PreconditionViolation(
                    "operation", "precondition", "actual", "id"
                )
                val asyncError = ApplicationValidationError.AsyncValidationError.ValidationTimeout(
                    "type", 1000, 1001
                )

                inputError should beInstanceOf<ApplicationValidationError>()
                crossAggregateError should beInstanceOf<ApplicationValidationError>()
                businessRuleError should beInstanceOf<ApplicationValidationError>()
                asyncError should beInstanceOf<ApplicationValidationError>()
            }
        }
    }
})