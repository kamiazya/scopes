package io.github.kamiazya.scopes.application.service.error

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * Test for ApplicationValidationError hierarchy.
 *
 * This test validates that application-level validation errors provide
 * comprehensive field-level error information for cross-aggregate validations.
 *
 * Following Serena MCP guidance on validation error patterns in DDD.
 */
class ApplicationValidationErrorTest :
    DescribeSpec({

        describe("ApplicationValidationError hierarchy") {

            describe("InputValidationError") {
                it("should create InvalidFieldFormat with field details") {
                    val error = InputValidationError.InvalidFieldFormat(
                        fieldName = "email",
                        expectedFormat = "valid email format",
                        actualValue = "invalid-email",
                        validationRule = "RFC 5322 compliant",
                    )

                    error.fieldName shouldBe "email"
                    error.actualValue shouldBe "invalid-email"
                    error.expectedFormat shouldBe "valid email format"
                    error.validationRule shouldBe "RFC 5322 compliant"
                }

                it("should create MissingRequiredField with context") {
                    val error = InputValidationError.MissingRequiredField(
                        fieldName = "title",
                        entityType = "Scope",
                    )

                    error.fieldName shouldBe "title"
                    error.entityType shouldBe "Scope"
                }

                it("should create ValueOutOfRange with bounds") {
                    val error = InputValidationError.ValueOutOfRange(
                        fieldName = "priority",
                        minValue = 1,
                        maxValue = 10,
                        actualValue = 15,
                    )

                    error.fieldName shouldBe "priority"
                    error.minValue shouldBe 1
                    error.maxValue shouldBe 10
                    error.actualValue shouldBe 15
                }
            }

            describe("CrossAggregateValidationError") {
                it("should create AggregateConsistencyViolation") {
                    val error = CrossAggregateValidationError.AggregateConsistencyViolation(
                        operation = "create",
                        affectedAggregates = setOf("Scope", "Project"),
                        consistencyRule = "unique_title_per_parent",
                        violationDetails = "Title already exists in parent scope",
                    )

                    error.operation shouldBe "create"
                    error.consistencyRule shouldBe "unique_title_per_parent"
                    error.violationDetails shouldBe "Title already exists in parent scope"
                }

                it("should create CrossReferenceViolation") {
                    val error = CrossAggregateValidationError.CrossReferenceViolation(
                        sourceAggregate = "Scope",
                        targetAggregate = "Project",
                        referenceType = "parent",
                        violation = "Parent does not exist",
                    )

                    error.sourceAggregate shouldBe "Scope"
                    error.targetAggregate shouldBe "Project"
                    error.referenceType shouldBe "parent"
                    error.violation shouldBe "Parent does not exist"
                }

                it("should create InvariantViolation") {
                    val error = CrossAggregateValidationError.InvariantViolation(
                        invariantName = "scope_hierarchy_limit",
                        aggregateIds = listOf("scope1", "scope2"),
                        violationDescription = "Maximum hierarchy depth exceeded",
                    )

                    error.invariantName shouldBe "scope_hierarchy_limit"
                    error.aggregateIds shouldBe listOf("scope1", "scope2")
                    error.violationDescription shouldBe "Maximum hierarchy depth exceeded"
                }
            }

            describe("BusinessRuleValidationError") {
                it("should create PreconditionViolation") {
                    val error = BusinessRuleValidationError.PreconditionViolation(
                        operation = "delete_scope",
                        precondition = "no_children",
                        currentState = "has_children",
                        requiredState = "no_children",
                    )

                    error.operation shouldBe "delete_scope"
                    error.precondition shouldBe "no_children"
                    error.currentState shouldBe "has_children"
                    error.requiredState shouldBe "no_children"
                }

                it("should create PostconditionViolation") {
                    val error = BusinessRuleValidationError.PostconditionViolation(
                        operation = "create_scope",
                        postcondition = "parent_updated",
                        expectedOutcome = "parent_child_count_incremented",
                        actualOutcome = "parent_child_count_unchanged",
                    )

                    error.operation shouldBe "create_scope"
                    error.postcondition shouldBe "parent_updated"
                    error.expectedOutcome shouldBe "parent_child_count_incremented"
                    error.actualOutcome shouldBe "parent_child_count_unchanged"
                }
            }

            describe("AsyncValidationError") {
                it("should create ValidationTimeout") {
                    val error = AsyncValidationError.ValidationTimeout<Any>(
                        operation = "cross_aggregate_check",
                        timeout = Duration.parse("5s"),
                        validationPhase = "consistency_check",
                    )

                    error.operation shouldBe "cross_aggregate_check"
                    error.timeout shouldBe Duration.parse("5s")
                    error.validationPhase shouldBe "consistency_check"
                }

                it("should create ConcurrentValidationConflict") {
                    val error = AsyncValidationError.ConcurrentValidationConflict(
                        resource = "scope_123",
                        conflictingOperations = listOf("update", "delete"),
                        timestamp = Instant.fromEpochSeconds(1234567890),
                    )

                    error.resource shouldBe "scope_123"
                    error.conflictingOperations shouldBe listOf("update", "delete")
                    error.timestamp shouldBe Instant.fromEpochSeconds(1234567890)
                }
            }

            describe("error type hierarchy verification") {
                it("should have correct inheritance") {
                    val inputError: ApplicationValidationError = InputValidationError.MissingRequiredField("field", "Type")
                    val crossError: ApplicationValidationError = CrossAggregateValidationError.InvariantViolation("inv", listOf("id"), "desc")
                    val businessError: ApplicationValidationError = BusinessRuleValidationError.PreconditionViolation("op", "pre", "curr", "req")
                    val asyncError: ApplicationValidationError = AsyncValidationError.ValidationTimeout<Any>("op", Duration.parse("1s"), "phase")

                    inputError should beInstanceOf<InputValidationError>()
                    crossError should beInstanceOf<CrossAggregateValidationError>()
                    businessError should beInstanceOf<BusinessRuleValidationError>()
                    asyncError should beInstanceOf<AsyncValidationError>()
                }
            }
        }
    })
