package io.github.kamiazya.scopes.interfaces.cli.transport

import com.google.protobuf.Any
import com.google.rpc.BadRequest
import com.google.rpc.ErrorInfo
import com.google.rpc.PreconditionFailure
import com.google.rpc.ResourceInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class GrpcStatusDetailsMapperTest :
    DescribeSpec({

        describe("GrpcStatusDetailsMapper") {

            describe("mapStatusExceptionToContractError") {

                it("should map BadRequest with empty title to InvalidTitle.Empty") {
                    // Create google.rpc.Status with BadRequest detail
                    val badRequest = BadRequest.newBuilder()
                        .addFieldViolations(
                            BadRequest.FieldViolation.newBuilder()
                                .setField("title")
                                .setDescription("Title cannot be empty"),
                        )
                        .build()

                    val errorInfo = ErrorInfo.newBuilder()
                        .setDomain("scopes.kamiazya.github.io")
                        .setReason("INVALID_TITLE")
                        .build()

                    val rpcStatus = com.google.rpc.Status.newBuilder()
                        .setCode(Status.Code.INVALID_ARGUMENT.value())
                        .setMessage("Title cannot be empty")
                        .addDetails(Any.pack(badRequest))
                        .addDetails(Any.pack(errorInfo))
                        .build()

                    val statusException = StatusProto.toStatusException(rpcStatus)

                    // Map to contract error
                    val contractError = GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)

                    // Verify
                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError.title shouldBe ""
                    contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.Empty>()
                }

                it("should map BadRequest with short title to InvalidTitle.TooShort") {
                    val badRequest = BadRequest.newBuilder()
                        .addFieldViolations(
                            BadRequest.FieldViolation.newBuilder()
                                .setField("title")
                                .setDescription("Title is too short (minimum 3 characters, got 2)"),
                        )
                        .build()

                    val errorInfo = ErrorInfo.newBuilder()
                        .setReason("INVALID_TITLE")
                        .putMetadata("title", "ab")
                        .putMetadata("minimumLength", "3")
                        .putMetadata("actualLength", "2")
                        .build()

                    val rpcStatus = com.google.rpc.Status.newBuilder()
                        .setCode(Status.Code.INVALID_ARGUMENT.value())
                        .setMessage("Title is too short")
                        .addDetails(Any.pack(badRequest))
                        .addDetails(Any.pack(errorInfo))
                        .build()

                    val statusException = StatusProto.toStatusException(rpcStatus)
                    val contractError = GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.InvalidTitle>()
                    contractError.title shouldBe "ab"
                    val failure = contractError.validationFailure.shouldBeInstanceOf<ScopeContractError.TitleValidationFailure.TooShort>()
                    failure.minimumLength shouldBe 3
                    failure.actualLength shouldBe 2
                }

                it("should map PreconditionFailure with DUPLICATE_TITLE to DuplicateTitle error") {
                    val preconditionFailure = PreconditionFailure.newBuilder()
                        .addViolations(
                            PreconditionFailure.Violation.newBuilder()
                                .setType("DUPLICATE_TITLE")
                                .setDescription("Title already exists")
                                .setSubject("title"),
                        )
                        .build()

                    val resourceInfo = ResourceInfo.newBuilder()
                        .setResourceType("scope")
                        .setResourceName("existing-456")
                        .build()

                    val errorInfo = ErrorInfo.newBuilder()
                        .setReason("DUPLICATE_TITLE")
                        .putMetadata("title", "Existing Title")
                        .putMetadata("parentId", "parent-123")
                        .putMetadata("existingScopeId", "existing-456")
                        .build()

                    val rpcStatus = com.google.rpc.Status.newBuilder()
                        .setCode(Status.Code.ALREADY_EXISTS.value())
                        .setMessage("Duplicate title")
                        .addDetails(Any.pack(preconditionFailure))
                        .addDetails(Any.pack(resourceInfo))
                        .addDetails(Any.pack(errorInfo))
                        .build()

                    val statusException = StatusProto.toStatusException(rpcStatus)
                    val contractError = GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.DuplicateTitle>()
                    contractError.title shouldBe "Existing Title"
                    contractError.parentId shouldBe "parent-123"
                    contractError.existingScopeId shouldBe "existing-456"
                }

                it("should map hierarchy depth violation to HierarchyViolation.MaxDepthExceeded") {
                    val preconditionFailure = PreconditionFailure.newBuilder()
                        .addViolations(
                            PreconditionFailure.Violation.newBuilder()
                                .setType("HIERARCHY_VIOLATION")
                                .setDescription("Maximum hierarchy depth exceeded (limit: 10, attempted: 11)")
                                .setSubject("scope-123"),
                        )
                        .build()

                    val errorInfo = ErrorInfo.newBuilder()
                        .setReason("HIERARCHY_VIOLATION")
                        .putMetadata("maxDepth", "10")
                        .putMetadata("attemptedDepth", "11")
                        .build()

                    val rpcStatus = com.google.rpc.Status.newBuilder()
                        .setCode(Status.Code.FAILED_PRECONDITION.value())
                        .setMessage("Hierarchy violation")
                        .addDetails(Any.pack(preconditionFailure))
                        .addDetails(Any.pack(errorInfo))
                        .build()

                    val statusException = StatusProto.toStatusException(rpcStatus)
                    val contractError = GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.HierarchyViolation>()
                    val violation = contractError.violation.shouldBeInstanceOf<ScopeContractError.HierarchyViolationType.MaxDepthExceeded>()
                    violation.scopeId shouldBe "scope-123"
                    violation.maximumDepth shouldBe 10
                    violation.attemptedDepth shouldBe 11
                }

                it("should map NOT_FOUND with ResourceInfo to NotFound error") {
                    val resourceInfo = ResourceInfo.newBuilder()
                        .setResourceType("scope")
                        .setResourceName("scope-not-found-123")
                        .build()

                    val rpcStatus = com.google.rpc.Status.newBuilder()
                        .setCode(Status.Code.NOT_FOUND.value())
                        .setMessage("Scope not found")
                        .addDetails(Any.pack(resourceInfo))
                        .build()

                    val statusException = StatusProto.toStatusException(rpcStatus)
                    val contractError = GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    contractError.scopeId shouldBe "scope-not-found-123"
                }

                it("should fall back to basic mapping when no details are present") {
                    val statusException = StatusException(
                        Status.NOT_FOUND.withDescription("Scope scope-123 not found"),
                    )

                    val contractError = GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)

                    contractError.shouldBeInstanceOf<ScopeContractError.BusinessError.NotFound>()
                    contractError.scopeId shouldBe "scope-123"
                }

                it("should map generic BadRequest without ErrorInfo to ValidationFailure") {
                    val badRequest = BadRequest.newBuilder()
                        .addFieldViolations(
                            BadRequest.FieldViolation.newBuilder()
                                .setField("parentId")
                                .setDescription("Invalid parent ID format"),
                        )
                        .build()

                    val rpcStatus = com.google.rpc.Status.newBuilder()
                        .setCode(Status.Code.INVALID_ARGUMENT.value())
                        .setMessage("Invalid request")
                        .addDetails(Any.pack(badRequest))
                        .build()

                    val statusException = StatusProto.toStatusException(rpcStatus)
                    val contractError = GrpcStatusDetailsMapper.mapStatusExceptionToContractError(statusException)

                    contractError.shouldBeInstanceOf<ScopeContractError.InputError.ValidationFailure>()
                    contractError.field shouldBe "parentId"
                    val constraint = contractError.constraint.shouldBeInstanceOf<ScopeContractError.ValidationConstraint.InvalidValue>()
                    constraint.actualValue shouldBe "Invalid parent ID format"
                }
            }
        }
    })
