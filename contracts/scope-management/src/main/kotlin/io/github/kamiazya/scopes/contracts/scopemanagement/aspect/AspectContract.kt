package io.github.kamiazya.scopes.contracts.scopemanagement.aspect

import kotlinx.datetime.Instant

/**
 * Aspect definition data structure.
 */
public data class AspectDefinition(val key: String, val description: String, val type: String, val createdAt: Instant, val updatedAt: Instant)

/**
 * Request to create a new aspect definition.
 */
public data class CreateAspectDefinitionRequest(val key: String, val description: String, val type: String)

/**
 * Request to update an aspect definition.
 */
public data class UpdateAspectDefinitionRequest(val key: String, val description: String? = null)

/**
 * Request to delete an aspect definition.
 */
public data class DeleteAspectDefinitionRequest(val key: String)

/**
 * Request to get an aspect definition.
 */
public data class GetAspectDefinitionRequest(val key: String)

/**
 * Request to list aspect definitions.
 */
public object ListAspectDefinitionsRequest

/**
 * Request to validate aspect value.
 */
public data class ValidateAspectValueRequest(val key: String, val values: List<String>)

/**
 * Contract for aspect operations.
 */
public object AspectContract {
    /**
     * Response for creating an aspect definition.
     */
    public sealed interface CreateAspectDefinitionResponse {
        public data class Success(val aspectDefinition: AspectDefinition) : CreateAspectDefinitionResponse
        public data class AlreadyExists(val key: String) : CreateAspectDefinitionResponse
        public data class InvalidType(val type: String, val supportedTypes: List<String>) : CreateAspectDefinitionResponse
        public data class ValidationError(val field: String, val validationFailure: ValidationFailure) : CreateAspectDefinitionResponse
    }

    /**
     * Response for updating an aspect definition.
     */
    public sealed interface UpdateAspectDefinitionResponse {
        public data class Success(val aspectDefinition: AspectDefinition) : UpdateAspectDefinitionResponse
        public data class NotFound(val key: String) : UpdateAspectDefinitionResponse
        public data class ValidationError(val field: String, val validationFailure: ValidationFailure) : UpdateAspectDefinitionResponse
    }

    /**
     * Response for deleting an aspect definition.
     */
    public sealed interface DeleteAspectDefinitionResponse {
        public object Success : DeleteAspectDefinitionResponse
        public data class NotFound(val key: String) : DeleteAspectDefinitionResponse
        public data class InUse(val key: String, val usageCount: Int) : DeleteAspectDefinitionResponse
    }

    /**
     * Response for getting an aspect definition.
     */
    public sealed interface GetAspectDefinitionResponse {
        public data class Success(val aspectDefinition: AspectDefinition?) : GetAspectDefinitionResponse
        public data class NotFound(val key: String) : GetAspectDefinitionResponse
    }

    /**
     * Response for listing aspect definitions.
     */
    public sealed interface ListAspectDefinitionsResponse {
        public data class Success(val aspectDefinitions: List<AspectDefinition>) : ListAspectDefinitionsResponse
    }

    /**
     * Response for validating aspect values.
     */
    public sealed interface ValidateAspectValueResponse {
        public data class Success(val validatedValues: List<String>) : ValidateAspectValueResponse
        public data class ValidationFailed(val key: String, val validationFailure: ValidationFailure) : ValidateAspectValueResponse
    }

    /**
     * Types of validation failures.
     */
    public sealed interface ValidationFailure {
        public data object Empty : ValidationFailure
        public data class TooShort(val minimumLength: Int) : ValidationFailure
        public data class TooLong(val maximumLength: Int) : ValidationFailure
        public data class InvalidFormat(val expectedFormat: String) : ValidationFailure
        public data class InvalidType(val expectedType: String) : ValidationFailure
        public data class NotInAllowedValues(val allowedValues: List<String>) : ValidationFailure
        public data object MultipleValuesNotAllowed : ValidationFailure
    }
}
