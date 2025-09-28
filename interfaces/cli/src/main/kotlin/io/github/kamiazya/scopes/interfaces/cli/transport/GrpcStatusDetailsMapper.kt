package io.github.kamiazya.scopes.interfaces.cli.transport

import com.google.rpc.BadRequest
import com.google.rpc.ErrorInfo
import com.google.rpc.PreconditionFailure
import com.google.rpc.ResourceInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import kotlin.time.Duration
import com.google.rpc.Status as RpcStatus

/**
 * Maps gRPC Status with google.rpc.Status details to ScopeContractError.
 *
 * This mapper extracts structured error details from StatusException
 * and converts them to appropriate contract errors with rich context.
 */
object GrpcStatusDetailsMapper {

    /**
     * Maps a StatusException to ScopeContractError, extracting any structured
     * error details if available.
     */
    fun mapStatusExceptionToContractError(e: StatusException): ScopeContractError {
        // Try to extract google.rpc.Status with details
        val rpcStatus = StatusProto.fromThrowable(e)

        return if (rpcStatus != null && rpcStatus.detailsList.isNotEmpty()) {
            // We have structured error details
            mapRpcStatusToContractError(rpcStatus, e.status)
        } else {
            // Fall back to basic status mapping
            mapBasicStatusToContractError(e.status, e.message)
        }
    }

    /**
     * Maps a google.rpc.Status with details to ScopeContractError.
     */
    private fun mapRpcStatusToContractError(rpcStatus: RpcStatus, grpcStatus: Status): ScopeContractError {
        // Extract different types of error details
        val badRequest = extractBadRequest(rpcStatus)
        val errorInfo = extractErrorInfo(rpcStatus)
        val preconditionFailure = extractPreconditionFailure(rpcStatus)
        val resourceInfo = extractResourceInfo(rpcStatus)

        // Map based on error type and available details
        return when {
            badRequest != null -> mapBadRequestToContractError(badRequest, errorInfo)
            preconditionFailure != null -> mapPreconditionFailureToContractError(preconditionFailure, resourceInfo, errorInfo)
            resourceInfo != null && grpcStatus.code == Status.Code.NOT_FOUND ->
                mapNotFoundError(resourceInfo)
            else -> mapBasicStatusToContractError(grpcStatus, rpcStatus.message, errorInfo)
        }
    }

    /**
     * Extracts BadRequest detail from RPC status.
     */
    private fun extractBadRequest(rpcStatus: RpcStatus): BadRequest? = rpcStatus.detailsList
        .firstOrNull { it.`is`(BadRequest::class.java) }
        ?.unpack(BadRequest::class.java)

    /**
     * Extracts ErrorInfo detail from RPC status.
     */
    private fun extractErrorInfo(rpcStatus: RpcStatus): ErrorInfo? = rpcStatus.detailsList
        .firstOrNull { it.`is`(ErrorInfo::class.java) }
        ?.unpack(ErrorInfo::class.java)

    /**
     * Extracts PreconditionFailure detail from RPC status.
     */
    private fun extractPreconditionFailure(rpcStatus: RpcStatus): PreconditionFailure? = rpcStatus.detailsList
        .firstOrNull { it.`is`(PreconditionFailure::class.java) }
        ?.unpack(PreconditionFailure::class.java)

    /**
     * Extracts ResourceInfo detail from RPC status.
     */
    private fun extractResourceInfo(rpcStatus: RpcStatus): ResourceInfo? = rpcStatus.detailsList
        .firstOrNull { it.`is`(ResourceInfo::class.java) }
        ?.unpack(ResourceInfo::class.java)

    /**
     * Maps BadRequest details to appropriate InputError.
     */
    private fun mapBadRequestToContractError(badRequest: BadRequest, errorInfo: ErrorInfo?): ScopeContractError {
        // Check if this is a title validation error based on ErrorInfo reason
        if (errorInfo?.reason == "INVALID_TITLE" && badRequest.fieldViolationsCount > 0) {
            val violation = badRequest.getFieldViolations(0)
            if (violation.field == "title") {
                return when {
                    violation.description.contains("empty", ignoreCase = true) ->
                        ScopeContractError.InputError.InvalidTitle(
                            title = "",
                            validationFailure = ScopeContractError.TitleValidationFailure.Empty,
                        )
                    violation.description.contains("too short", ignoreCase = true) -> {
                        // Use structured metadata from ErrorInfo - required for title validation
                        val minLength = errorInfo?.metadataMap?.get("minimumLength")?.toIntOrNull()
                            ?: error("TitleTooShort error must include minimumLength in ErrorInfo metadata")
                        val actualLength = errorInfo?.metadataMap?.get("actualLength")?.toIntOrNull()
                            ?: error("TitleTooShort error must include actualLength in ErrorInfo metadata")
                        val title = errorInfo?.metadataMap?.get("title")
                            ?: error("TitleTooShort error must include title in ErrorInfo metadata")

                        ScopeContractError.InputError.InvalidTitle(
                            title = title,
                            validationFailure = ScopeContractError.TitleValidationFailure.TooShort(
                                minimumLength = minLength,
                                actualLength = actualLength,
                            ),
                        )
                    }
                    violation.description.contains("too long", ignoreCase = true) -> {
                        // Use structured metadata from ErrorInfo - required for title validation
                        val maxLength = errorInfo?.metadataMap?.get("maximumLength")?.toIntOrNull()
                            ?: error("TitleTooLong error must include maximumLength in ErrorInfo metadata")
                        val actualLength = errorInfo?.metadataMap?.get("actualLength")?.toIntOrNull()
                            ?: error("TitleTooLong error must include actualLength in ErrorInfo metadata")
                        val title = errorInfo?.metadataMap?.get("title")
                            ?: error("TitleTooLong error must include title in ErrorInfo metadata")

                        ScopeContractError.InputError.InvalidTitle(
                            title = title,
                            validationFailure = ScopeContractError.TitleValidationFailure.TooLong(
                                maximumLength = maxLength,
                                actualLength = actualLength,
                            ),
                        )
                    }
                    else ->
                        // Generic validation failure
                        ScopeContractError.InputError.ValidationFailure(
                            field = violation.field,
                            value = "",
                            constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                                expectedValues = null,
                                actualValue = violation.description,
                            ),
                        )
                }
            }
        }

        // For other field violations, map to generic ValidationFailure
        return if (badRequest.fieldViolationsCount > 0) {
            val violation = badRequest.getFieldViolations(0)
            ScopeContractError.InputError.ValidationFailure(
                field = violation.field,
                value = "",
                constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                    expectedValues = null,
                    actualValue = violation.description,
                ),
            )
        } else {
            // No specific field violations
            ScopeContractError.InputError.ValidationFailure(
                field = "unknown",
                value = "unknown",
                constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                    expectedValues = null,
                    actualValue = "Invalid request",
                ),
            )
        }
    }

    /**
     * Maps PreconditionFailure details to appropriate BusinessError.
     */
    private fun mapPreconditionFailureToContractError(
        preconditionFailure: PreconditionFailure,
        resourceInfo: ResourceInfo?,
        errorInfo: ErrorInfo? = null,
    ): ScopeContractError {
        if (preconditionFailure.violationsCount > 0) {
            val violation = preconditionFailure.getViolations(0)

            return when (violation.type) {
                "DUPLICATE_TITLE" -> {
                    // Get title from ErrorInfo metadata or violation subject
                    val title = errorInfo?.metadataMap?.get("title")
                        ?: violation.subject.takeIf { it.isNotBlank() }
                        ?: error("DuplicateTitle error must include title in ErrorInfo metadata or violation subject")

                    ScopeContractError.BusinessError.DuplicateTitle(
                        title = title,
                        parentId = errorInfo?.metadataMap?.get("parentId"),
                        existingScopeId = resourceInfo?.resourceName,
                    )
                }
                "HIERARCHY_VIOLATION" -> {
                    // Use structured metadata from ErrorInfo - required for all hierarchy violations
                    when {
                        violation.description.contains("depth exceeded", ignoreCase = true) -> {
                            val limit = errorInfo?.metadataMap?.get("maxDepth")?.toIntOrNull()
                                ?: error("MaxDepthExceeded error must include maxDepth in ErrorInfo metadata")
                            val attempted = errorInfo?.metadataMap?.get("attemptedDepth")?.toIntOrNull()
                                ?: error("MaxDepthExceeded error must include attemptedDepth in ErrorInfo metadata")

                            ScopeContractError.BusinessError.HierarchyViolation(
                                violation = ScopeContractError.HierarchyViolationType.MaxDepthExceeded(
                                    scopeId = violation.subject.ifEmpty { "unknown" },
                                    attemptedDepth = attempted,
                                    maximumDepth = limit,
                                ),
                            )
                        }
                        violation.description.contains("children exceeded", ignoreCase = true) -> {
                            val limit = errorInfo?.metadataMap?.get("maxChildren")?.toIntOrNull()
                                ?: error("MaxChildrenExceeded error must include maxChildren in ErrorInfo metadata")
                            val current = errorInfo?.metadataMap?.get("currentChildren")?.toIntOrNull()
                                ?: error("MaxChildrenExceeded error must include currentChildren in ErrorInfo metadata")

                            ScopeContractError.BusinessError.HierarchyViolation(
                                violation = ScopeContractError.HierarchyViolationType.MaxChildrenExceeded(
                                    parentId = violation.subject.ifEmpty { "unknown" },
                                    currentChildrenCount = current,
                                    maximumChildren = limit,
                                ),
                            )
                        }
                        else -> {
                            error("Unsupported hierarchy violation type: ${violation.description}")
                        }
                    }
                }
                else -> {
                    // Generic precondition failure - map to generic validation failure
                    ScopeContractError.InputError.ValidationFailure(
                        field = violation.type,
                        value = violation.subject,
                        constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                            expectedValues = null,
                            actualValue = violation.description,
                        ),
                    )
                }
            }
        }

        // No specific violations - map to generic validation failure
        return ScopeContractError.InputError.ValidationFailure(
            field = "precondition",
            value = "unknown",
            constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                expectedValues = null,
                actualValue = "Precondition failed",
            ),
        )
    }

    /**
     * Maps NOT_FOUND error with ResourceInfo.
     */
    private fun mapNotFoundError(resourceInfo: ResourceInfo): ScopeContractError = when (resourceInfo.resourceType) {
        "scope" -> ScopeContractError.BusinessError.NotFound(
            scopeId = resourceInfo.resourceName,
        )
        "context_view" -> ScopeContractError.BusinessError.ContextNotFound(
            contextKey = resourceInfo.resourceName,
        )
        else -> ScopeContractError.BusinessError.NotFound(
            scopeId = resourceInfo.resourceName,
        )
    }

    /**
     * Basic status mapping without structured details (fallback).
     */
    private fun mapBasicStatusToContractError(status: Status, message: String?, errorInfo: ErrorInfo? = null): ScopeContractError = when (status.code) {
        Status.Code.INVALID_ARGUMENT -> {
            ScopeContractError.InputError.ValidationFailure(
                field = "unknown",
                value = "unknown",
                constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                    expectedValues = null,
                    actualValue = message ?: "Invalid argument",
                ),
            )
        }
        Status.Code.NOT_FOUND -> {
            // Try to extract scope ID from message if no ErrorInfo metadata is available
            val scopeId = errorInfo?.metadataMap?.get("scopeId")
                ?: message?.let { msg ->
                    // Extract scope ID from message like "Scope scope-123 not found"
                    Regex("Scope ([\\w-]+) not found").find(msg)?.groupValues?.get(1)
                } ?: "unknown"
            ScopeContractError.BusinessError.NotFound(scopeId = scopeId)
        }
        Status.Code.ALREADY_EXISTS -> {
            // Use structured metadata from ErrorInfo
            val title = errorInfo?.metadataMap?.get("title") ?: "unknown"
            val parentId = errorInfo?.metadataMap?.get("parentId")
            val existingScopeId = errorInfo?.metadataMap?.get("existingScopeId")
            ScopeContractError.BusinessError.DuplicateTitle(
                title = title,
                parentId = parentId,
                existingScopeId = existingScopeId,
            )
        }
        Status.Code.FAILED_PRECONDITION -> {
            ScopeContractError.InputError.ValidationFailure(
                field = "precondition",
                value = "unknown",
                constraint = ScopeContractError.ValidationConstraint.InvalidValue(
                    expectedValues = null,
                    actualValue = message ?: "Operation failed precondition",
                ),
            )
        }
        Status.Code.UNAVAILABLE -> {
            ScopeContractError.SystemError.ServiceUnavailable(
                service = message?.substringAfter("Service '")?.substringBefore("' is") ?: "daemon",
            )
        }
        Status.Code.DEADLINE_EXCEEDED -> {
            // Use structured metadata from ErrorInfo - required for all timeout errors
            val operation = errorInfo?.metadataMap?.get("operation")
                ?: error("Timeout error must include operation in ErrorInfo metadata")
            val timeout = errorInfo.metadataMap["timeout"]?.let { timeoutStr ->
                try {
                    Duration.parse(timeoutStr)
                } catch (e: Exception) {
                    error("Invalid timeout format in ErrorInfo metadata: $timeoutStr")
                }
            } ?: error("Timeout error must include timeout in ErrorInfo metadata")

            ScopeContractError.SystemError.Timeout(
                operation = operation,
                timeout = timeout,
            )
        }
        else -> {
            ScopeContractError.SystemError.ServiceUnavailable(
                service = "gRPC transport",
            )
        }
    }
}
