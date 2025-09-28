package io.github.kamiazya.scopes.interfaces.daemon.grpc.services

import com.google.protobuf.Any
import com.google.rpc.BadRequest
import com.google.rpc.ErrorInfo
import com.google.rpc.Help
import com.google.rpc.LocalizedMessage
import com.google.rpc.PreconditionFailure
import com.google.rpc.ResourceInfo
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.interfaces.daemon.grpc.services.TaskGatewayServiceImpl
import io.grpc.Status
import io.grpc.protobuf.StatusProto

/**
 * Builder for creating enhanced error responses with google.rpc.Status error details.
 *
 * This provides structured error information for validation errors and other
 * contract errors, making it easier for clients to understand and handle errors
 * programmatically.
 */
internal object ErrorDetailsBuilder {

    /**
     * Creates an enhanced StatusException with structured error details.
     */
    fun createStatusException(error: ScopeContractError): io.grpc.StatusException {
        val (status, message) = mapContractErrorToGrpcBasic(error)
        val errorDetails = createErrorDetails(error)

        return if (errorDetails.isNotEmpty()) {
            // Create google.rpc.Status with details
            val rpcStatus = com.google.rpc.Status.newBuilder()
                .setCode(status.code.value())
                .setMessage(message)
                .addAllDetails(errorDetails)
                .build()

            StatusProto.toStatusException(rpcStatus)
        } else {
            // Fall back to simple StatusException
            io.grpc.StatusException(status.withDescription(message))
        }
    }

    /**
     * Maps contract error to basic gRPC status and message.
     */
    private fun mapContractErrorToGrpcBasic(error: ScopeContractError): Pair<Status, String> = TaskGatewayServiceImpl.mapContractErrorToGrpc(error)

    /**
     * Creates error details based on the type of contract error.
     */
    private fun createErrorDetails(error: ScopeContractError): List<Any> {
        val details = mutableListOf<Any>()

        when (error) {
            is ScopeContractError.InputError -> {
                details.addAll(createInputErrorDetails(error))
            }
            is ScopeContractError.BusinessError -> {
                details.addAll(createBusinessErrorDetails(error))
            }
            is ScopeContractError.SystemError -> {
                details.addAll(createSystemErrorDetails(error))
            }
            is ScopeContractError.DataInconsistency -> {
                details.addAll(createDataInconsistencyDetails(error))
            }
        }

        return details
    }

    /**
     * Creates error details for input validation errors.
     */
    private fun createInputErrorDetails(error: ScopeContractError.InputError): List<Any> {
        val details = mutableListOf<Any>()

        // Add BadRequest details for validation errors
        val badRequestBuilder = BadRequest.newBuilder()

        when (error) {
            is ScopeContractError.InputError.InvalidTitle -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("title")
                    .setDescription(getValidationDescription(error.validationFailure))
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.InvalidDescription -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("description")
                    .setDescription(getDescriptionValidationDescription(error.validationFailure))
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.InvalidAlias -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("alias")
                    .setDescription(getAliasValidationDescription(error.validationFailure))
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.InvalidContextKey -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("contextKey")
                    .setDescription(getContextKeyValidationDescription(error.validationFailure))
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.InvalidContextFilter -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("filter")
                    .setDescription(getContextFilterValidationDescription(error.validationFailure))
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.ValidationFailure -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField(error.field)
                    .setDescription(getGenericValidationDescription(error.constraint))
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.InvalidId -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("id")
                    .setDescription("Invalid ID format: ${error.id}")
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.InvalidParentId -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("parentId")
                    .setDescription("Invalid parent ID format: ${error.parentId}")
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }

            is ScopeContractError.InputError.InvalidContextName -> {
                val fieldViolation = BadRequest.FieldViolation.newBuilder()
                    .setField("name")
                    .setDescription(getContextNameValidationDescription(error.validationFailure))
                    .build()
                badRequestBuilder.addFieldViolations(fieldViolation)
            }
        }

        if (badRequestBuilder.fieldViolationsCount > 0) {
            details.add(Any.pack(badRequestBuilder.build()))
        }

        // Add ErrorInfo for categorization
        val errorInfo = ErrorInfo.newBuilder()
            .setDomain("scopes.kamiazya.github.io")
            .setReason(getErrorReason(error))
            .putAllMetadata(getErrorMetadata(error))
            .build()
        details.add(Any.pack(errorInfo))

        return details
    }

    /**
     * Creates error details for business rule violations.
     */
    private fun createBusinessErrorDetails(error: ScopeContractError.BusinessError): List<Any> {
        val details = mutableListOf<Any>()

        when (error) {
            is ScopeContractError.BusinessError.NotFound -> {
                val resourceInfo = ResourceInfo.newBuilder()
                    .setResourceType("scope")
                    .setResourceName(error.scopeId)
                    .setDescription("The requested scope does not exist")
                    .build()
                details.add(Any.pack(resourceInfo))
            }

            is ScopeContractError.BusinessError.DuplicateTitle -> {
                val preconditionFailure = PreconditionFailure.newBuilder()
                    .addViolations(
                        PreconditionFailure.Violation.newBuilder()
                            .setType("DUPLICATE_TITLE")
                            .setSubject("title")
                            .setDescription("A scope with this title already exists in the parent scope")
                            .build(),
                    )
                    .build()
                details.add(Any.pack(preconditionFailure))

                // Add resource info for the existing scope
                if (error.existingScopeId != null) {
                    val resourceInfo = ResourceInfo.newBuilder()
                        .setResourceType("scope")
                        .setResourceName(error.existingScopeId)
                        .setDescription("Existing scope with duplicate title")
                        .build()
                    details.add(Any.pack(resourceInfo))
                }
            }

            is ScopeContractError.BusinessError.HierarchyViolation -> {
                val preconditionFailure = PreconditionFailure.newBuilder()
                    .addViolations(
                        PreconditionFailure.Violation.newBuilder()
                            .setType("HIERARCHY_VIOLATION")
                            .setSubject("hierarchy")
                            .setDescription(getHierarchyViolationDescription(error.violation))
                            .build(),
                    )
                    .build()
                details.add(Any.pack(preconditionFailure))
            }

            is ScopeContractError.BusinessError.HasChildren -> {
                val preconditionFailure = PreconditionFailure.newBuilder()
                    .addViolations(
                        PreconditionFailure.Violation.newBuilder()
                            .setType("HAS_CHILDREN")
                            .setSubject("scope")
                            .setDescription("Cannot delete scope with ${error.childrenCount ?: "existing"} children")
                            .build(),
                    )
                    .build()
                details.add(Any.pack(preconditionFailure))
            }

            is ScopeContractError.BusinessError.AlreadyDeleted -> {
                val preconditionFailure = PreconditionFailure.newBuilder()
                    .addViolations(
                        PreconditionFailure.Violation.newBuilder()
                            .setType("ALREADY_DELETED")
                            .setSubject("scope")
                            .setDescription("The scope has already been deleted")
                            .build(),
                    )
                    .build()
                details.add(Any.pack(preconditionFailure))
            }

            is ScopeContractError.BusinessError.ArchivedScope -> {
                val preconditionFailure = PreconditionFailure.newBuilder()
                    .addViolations(
                        PreconditionFailure.Violation.newBuilder()
                            .setType("ARCHIVED_SCOPE")
                            .setSubject("scope")
                            .setDescription("Cannot modify archived scope")
                            .build(),
                    )
                    .build()
                details.add(Any.pack(preconditionFailure))
            }

            else -> {
                // Generic business error handling
                val errorInfo = ErrorInfo.newBuilder()
                    .setDomain("scopes.kamiazya.github.io")
                    .setReason(getErrorReason(error))
                    .putAllMetadata(getErrorMetadata(error))
                    .build()
                details.add(Any.pack(errorInfo))
            }
        }

        return details
    }

    /**
     * Creates error details for system errors.
     */
    private fun createSystemErrorDetails(error: ScopeContractError.SystemError): List<Any> {
        val details = mutableListOf<Any>()

        val errorInfo = ErrorInfo.newBuilder()
            .setDomain("scopes.kamiazya.github.io")
            .setReason(getErrorReason(error))
            .putAllMetadata(getErrorMetadata(error))
            .build()
        details.add(Any.pack(errorInfo))

        // Add help links for common system errors
        when (error) {
            is ScopeContractError.SystemError.ServiceUnavailable -> {
                val help = Help.newBuilder()
                    .addLinks(
                        Help.Link.newBuilder()
                            .setDescription("Check service status")
                            .setUrl("https://github.com/kamiazya/scopes/wiki/troubleshooting#service-unavailable")
                            .build(),
                    )
                    .build()
                details.add(Any.pack(help))
            }

            is ScopeContractError.SystemError.Timeout -> {
                val help = Help.newBuilder()
                    .addLinks(
                        Help.Link.newBuilder()
                            .setDescription("Timeout troubleshooting guide")
                            .setUrl("https://github.com/kamiazya/scopes/wiki/troubleshooting#timeouts")
                            .build(),
                    )
                    .build()
                details.add(Any.pack(help))
            }

            else -> {}
        }

        return details
    }

    /**
     * Creates error details for data inconsistency errors.
     */
    private fun createDataInconsistencyDetails(error: ScopeContractError.DataInconsistency): List<Any> {
        val details = mutableListOf<Any>()

        val errorInfo = ErrorInfo.newBuilder()
            .setDomain("scopes.kamiazya.github.io")
            .setReason("DATA_INCONSISTENCY")
            .putMetadata("type", error::class.simpleName ?: "Unknown")
            .build()
        details.add(Any.pack(errorInfo))

        // Add localized message
        val localizedMessage = LocalizedMessage.newBuilder()
            .setLocale("en-US")
            .setMessage("A data inconsistency was detected. Please contact support.")
            .build()
        details.add(Any.pack(localizedMessage))

        return details
    }

    // Helper functions for generating descriptions

    private fun getValidationDescription(failure: ScopeContractError.TitleValidationFailure): String = when (failure) {
        is ScopeContractError.TitleValidationFailure.Empty -> "Title cannot be empty"
        is ScopeContractError.TitleValidationFailure.TooShort ->
            "Title is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.TitleValidationFailure.TooLong ->
            "Title is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
            "Title contains invalid characters: ${failure.prohibitedCharacters.joinToString()}"
    }

    private fun getDescriptionValidationDescription(failure: ScopeContractError.DescriptionValidationFailure): String = when (failure) {
        is ScopeContractError.DescriptionValidationFailure.TooLong ->
            "Description is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
    }

    private fun getAliasValidationDescription(failure: ScopeContractError.AliasValidationFailure): String = when (failure) {
        is ScopeContractError.AliasValidationFailure.Empty -> "Alias cannot be empty"
        is ScopeContractError.AliasValidationFailure.TooShort ->
            "Alias is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.AliasValidationFailure.TooLong ->
            "Alias is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.AliasValidationFailure.InvalidFormat ->
            "Alias has invalid format. Expected: ${failure.expectedPattern}"
    }

    private fun getContextKeyValidationDescription(failure: ScopeContractError.ContextKeyValidationFailure): String = when (failure) {
        is ScopeContractError.ContextKeyValidationFailure.Empty -> "Context key cannot be empty"
        is ScopeContractError.ContextKeyValidationFailure.TooShort ->
            "Context key is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextKeyValidationFailure.TooLong ->
            "Context key is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextKeyValidationFailure.InvalidFormat ->
            "Context key has invalid format: ${failure.invalidType}"
    }

    private fun getContextNameValidationDescription(failure: ScopeContractError.ContextNameValidationFailure): String = when (failure) {
        is ScopeContractError.ContextNameValidationFailure.Empty -> "Context name cannot be empty"
        is ScopeContractError.ContextNameValidationFailure.TooLong ->
            "Context name is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
    }

    private fun getContextFilterValidationDescription(failure: ScopeContractError.ContextFilterValidationFailure): String = when (failure) {
        is ScopeContractError.ContextFilterValidationFailure.Empty -> "Context filter cannot be empty"
        is ScopeContractError.ContextFilterValidationFailure.TooShort ->
            "Context filter is too short (minimum ${failure.minimumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextFilterValidationFailure.TooLong ->
            "Context filter is too long (maximum ${failure.maximumLength} characters, got ${failure.actualLength})"
        is ScopeContractError.ContextFilterValidationFailure.InvalidSyntax ->
            "Invalid filter syntax: ${failure.errorType} in expression '${failure.expression}'"
    }

    private fun getGenericValidationDescription(constraint: ScopeContractError.ValidationConstraint): String = when (constraint) {
        is ScopeContractError.ValidationConstraint.Empty -> "Field cannot be empty"
        is ScopeContractError.ValidationConstraint.TooShort ->
            "Field is too short (minimum ${constraint.minimumLength} characters, got ${constraint.actualLength})"
        is ScopeContractError.ValidationConstraint.TooLong ->
            "Field is too long (maximum ${constraint.maximumLength} characters, got ${constraint.actualLength})"
        is ScopeContractError.ValidationConstraint.InvalidFormat ->
            "Field has invalid format. Expected: ${constraint.expectedFormat}"
        is ScopeContractError.ValidationConstraint.InvalidType ->
            "Field has invalid type. Expected: ${constraint.expectedType}, got: ${constraint.actualType}"
        is ScopeContractError.ValidationConstraint.InvalidValue ->
            "Field has invalid value: '${constraint.actualValue}'"
        is ScopeContractError.ValidationConstraint.EmptyValues ->
            "Field cannot have empty values"
        is ScopeContractError.ValidationConstraint.MultipleValuesNotAllowed ->
            "Field cannot have multiple values"
        is ScopeContractError.ValidationConstraint.RequiredField ->
            "Field is required"
    }

    private fun getHierarchyViolationDescription(violation: ScopeContractError.HierarchyViolationType): String = when (violation) {
        is ScopeContractError.HierarchyViolationType.CircularReference ->
            "Circular reference detected: scope ${violation.scopeId} cannot have parent ${violation.parentId}"
        is ScopeContractError.HierarchyViolationType.MaxDepthExceeded ->
            "Maximum hierarchy depth exceeded (limit: ${violation.maximumDepth}, attempted: ${violation.attemptedDepth})"
        is ScopeContractError.HierarchyViolationType.MaxChildrenExceeded ->
            "Maximum children limit exceeded (limit: ${violation.maximumChildren}, current: ${violation.currentChildrenCount})"
        is ScopeContractError.HierarchyViolationType.SelfParenting ->
            "A scope cannot be its own parent"
        is ScopeContractError.HierarchyViolationType.ParentNotFound ->
            "Parent scope not found"
    }

    private fun getErrorReason(error: ScopeContractError): String = when (error) {
        is ScopeContractError.InputError -> when (error) {
            is ScopeContractError.InputError.InvalidTitle -> "INVALID_TITLE"
            is ScopeContractError.InputError.InvalidDescription -> "INVALID_DESCRIPTION"
            is ScopeContractError.InputError.InvalidAlias -> "INVALID_ALIAS"
            is ScopeContractError.InputError.InvalidContextKey -> "INVALID_CONTEXT_KEY"
            is ScopeContractError.InputError.InvalidContextFilter -> "INVALID_CONTEXT_FILTER"
            is ScopeContractError.InputError.ValidationFailure -> "VALIDATION_FAILURE"
            else -> "INPUT_ERROR"
        }
        is ScopeContractError.BusinessError -> when (error) {
            is ScopeContractError.BusinessError.NotFound -> "NOT_FOUND"
            is ScopeContractError.BusinessError.DuplicateTitle -> "DUPLICATE_TITLE"
            is ScopeContractError.BusinessError.HierarchyViolation -> "HIERARCHY_VIOLATION"
            is ScopeContractError.BusinessError.HasChildren -> "HAS_CHILDREN"
            is ScopeContractError.BusinessError.AlreadyDeleted -> "ALREADY_DELETED"
            is ScopeContractError.BusinessError.ArchivedScope -> "ARCHIVED_SCOPE"
            else -> "BUSINESS_ERROR"
        }
        is ScopeContractError.SystemError -> when (error) {
            is ScopeContractError.SystemError.ServiceUnavailable -> "SERVICE_UNAVAILABLE"
            is ScopeContractError.SystemError.Timeout -> "TIMEOUT"
            else -> "SYSTEM_ERROR"
        }
        is ScopeContractError.DataInconsistency -> "DATA_INCONSISTENCY"
    }

    private fun getErrorMetadata(error: ScopeContractError): Map<String, String> {
        val metadata = mutableMapOf<String, String>()

        when (error) {
            is ScopeContractError.InputError.InvalidTitle -> {
                metadata["title"] = error.title
                when (val failure = error.validationFailure) {
                    is ScopeContractError.TitleValidationFailure.TooShort -> {
                        metadata["minimumLength"] = failure.minimumLength.toString()
                        metadata["actualLength"] = failure.actualLength.toString()
                    }
                    is ScopeContractError.TitleValidationFailure.TooLong -> {
                        metadata["maximumLength"] = failure.maximumLength.toString()
                        metadata["actualLength"] = failure.actualLength.toString()
                    }
                    else -> {}
                }
            }
            is ScopeContractError.BusinessError.HierarchyViolation -> {
                when (val violation = error.violation) {
                    is ScopeContractError.HierarchyViolationType.MaxDepthExceeded -> {
                        metadata["maxDepth"] = violation.maximumDepth.toString()
                        metadata["attemptedDepth"] = violation.attemptedDepth.toString()
                    }
                    is ScopeContractError.HierarchyViolationType.MaxChildrenExceeded -> {
                        metadata["maxChildren"] = violation.maximumChildren.toString()
                        metadata["currentChildren"] = violation.currentChildrenCount.toString()
                    }
                    else -> {}
                }
            }
            is ScopeContractError.BusinessError.DuplicateTitle -> {
                metadata["title"] = error.title
                error.parentId?.let { metadata["parentId"] = it }
                error.existingScopeId?.let { metadata["existingScopeId"] = it }
            }
            is ScopeContractError.BusinessError.NotFound -> {
                metadata["scopeId"] = error.scopeId
            }
            is ScopeContractError.SystemError.Timeout -> {
                metadata["operation"] = error.operation
                metadata["timeout"] = error.timeout.toString()
            }
            else -> {}
        }

        return metadata
    }
}
