package io.github.kamiazya.scopes.interfaces.daemon.grpc.services

import com.google.rpc.BadRequest
import com.google.rpc.ErrorInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ErrorDetailsBuilderTest :
    DescribeSpec({

        describe("ErrorDetailsBuilder") {

            describe("createStatusException") {

                it("should create enhanced error for InvalidTitle with empty validation") {
                    val error = ScopeContractError.InputError.InvalidTitle(
                        title = "",
                        validationFailure = ScopeContractError.TitleValidationFailure.Empty,
                    )

                    val exception = ErrorDetailsBuilder.createStatusException(error)

                    exception.shouldBeInstanceOf<StatusException>()
                    exception.status.code shouldBe io.grpc.Status.Code.INVALID_ARGUMENT
                    exception.status.description shouldBe "Title cannot be empty"

                    // Extract the google.rpc.Status
                    val rpcStatus = StatusProto.fromThrowable(exception)
                    rpcStatus shouldNotBe null
                    rpcStatus!!.code shouldBe io.grpc.Status.Code.INVALID_ARGUMENT.value()

                    // Verify error details
                    val details = rpcStatus.detailsList
                    details.size shouldBe 2 // BadRequest and ErrorInfo

                    // Check BadRequest details
                    val badRequestAny = details.find { it.`is`(BadRequest::class.java) }
                    badRequestAny shouldNotBe null
                    val badRequest = badRequestAny!!.unpack(BadRequest::class.java)
                    badRequest.fieldViolationsCount shouldBe 1
                    badRequest.getFieldViolations(0).field shouldBe "title"
                    badRequest.getFieldViolations(0).description shouldBe "Title cannot be empty"

                    // Check ErrorInfo details
                    val errorInfoAny = details.find { it.`is`(ErrorInfo::class.java) }
                    errorInfoAny shouldNotBe null
                    val errorInfo = errorInfoAny!!.unpack(ErrorInfo::class.java)
                    errorInfo.domain shouldBe "scopes.kamiazya.github.io"
                    errorInfo.reason shouldBe "INVALID_TITLE"
                }

                it("should create enhanced error for TitleTooShort validation") {
                    val error = ScopeContractError.InputError.InvalidTitle(
                        title = "ab",
                        validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                            minimumLength = 3,
                            actualLength = 2,
                        ),
                    )

                    val exception = ErrorDetailsBuilder.createStatusException(error)
                    val rpcStatus = StatusProto.fromThrowable(exception)

                    rpcStatus shouldNotBe null

                    // Check BadRequest details
                    val badRequestAny = rpcStatus!!.detailsList.find { it.`is`(BadRequest::class.java) }
                    val badRequest = badRequestAny!!.unpack(BadRequest::class.java)
                    badRequest.getFieldViolations(0).description shouldBe "Title is too short (minimum 3 characters, got 2)"
                }

                it("should create enhanced error for DuplicateTitle business error") {
                    val error = ScopeContractError.BusinessError.DuplicateTitle(
                        title = "Existing Title",
                        parentId = "parent-123",
                        existingScopeId = "existing-456",
                    )

                    val exception = ErrorDetailsBuilder.createStatusException(error)

                    exception.status.code shouldBe io.grpc.Status.Code.ALREADY_EXISTS

                    val rpcStatus = StatusProto.fromThrowable(exception)
                    rpcStatus shouldNotBe null

                    // Should have PreconditionFailure and ResourceInfo
                    val details = rpcStatus!!.detailsList
                    details.size shouldBe 2

                    // Check PreconditionFailure
                    val preconditionFailureAny = details.find { it.`is`(com.google.rpc.PreconditionFailure::class.java) }
                    preconditionFailureAny shouldNotBe null
                    val preconditionFailure = preconditionFailureAny!!.unpack(com.google.rpc.PreconditionFailure::class.java)
                    preconditionFailure.violationsCount shouldBe 1
                    preconditionFailure.getViolations(0).type shouldBe "DUPLICATE_TITLE"

                    // Check ResourceInfo for existing scope
                    val resourceInfoAny = details.find { it.`is`(com.google.rpc.ResourceInfo::class.java) }
                    resourceInfoAny shouldNotBe null
                    val resourceInfo = resourceInfoAny!!.unpack(com.google.rpc.ResourceInfo::class.java)
                    resourceInfo.resourceType shouldBe "scope"
                    resourceInfo.resourceName shouldBe "existing-456"
                }

                it("should create enhanced error for hierarchy violation") {
                    val error = ScopeContractError.BusinessError.HierarchyViolation(
                        violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                            scopeId = "scope-123",
                            attemptedDepth = 11,
                            maximumDepth = 10,
                        ),
                    )

                    val exception = ErrorDetailsBuilder.createStatusException(error)
                    exception.status.code shouldBe io.grpc.Status.Code.FAILED_PRECONDITION

                    val rpcStatus = StatusProto.fromThrowable(exception)
                    val details = rpcStatus!!.detailsList

                    // Check PreconditionFailure
                    val preconditionFailureAny = details.find { it.`is`(com.google.rpc.PreconditionFailure::class.java) }
                    val preconditionFailure = preconditionFailureAny!!.unpack(com.google.rpc.PreconditionFailure::class.java)
                    preconditionFailure.getViolations(0).type shouldBe "HIERARCHY_VIOLATION"
                    preconditionFailure.getViolations(0).description shouldBe
                        "Maximum hierarchy depth exceeded (limit: 10, attempted: 11)"
                }

                it("should create enhanced error for system errors with help links") {
                    val error = ScopeContractError.SystemError.ServiceUnavailable(
                        service = "database",
                    )

                    val exception = ErrorDetailsBuilder.createStatusException(error)
                    exception.status.code shouldBe io.grpc.Status.Code.UNAVAILABLE

                    val rpcStatus = StatusProto.fromThrowable(exception)
                    val details = rpcStatus!!.detailsList

                    // Should have ErrorInfo and Help
                    details.size shouldBe 2

                    // Check Help link
                    val helpAny = details.find { it.`is`(com.google.rpc.Help::class.java) }
                    helpAny shouldNotBe null
                    val help = helpAny!!.unpack(com.google.rpc.Help::class.java)
                    help.linksCount shouldBe 1
                    help.getLinks(0).description shouldBe "Check service status"
                    help.getLinks(0).url shouldBe "https://github.com/kamiazya/scopes/wiki/troubleshooting#service-unavailable"
                }
            }
        }
    })
