package io.github.kamiazya.scopes.contracts.scopemanagement.results

import io.github.kamiazya.scopes.contracts.scopemanagement.types.AspectDefinition
import io.github.kamiazya.scopes.contracts.scopemanagement.types.ValidationFailure

/**
 * Response for creating an aspect definition.
 */
public sealed interface CreateAspectDefinitionResult {
    public data class Success(val aspectDefinition: AspectDefinition) : CreateAspectDefinitionResult
    public data class AlreadyExists(val key: String) : CreateAspectDefinitionResult
    public data class InvalidType(val type: String, val supportedTypes: List<String>) : CreateAspectDefinitionResult
    public data class ValidationError(val field: String, val validationFailure: ValidationFailure) : CreateAspectDefinitionResult
}

/**
 * Response for updating an aspect definition.
 */
public sealed interface UpdateAspectDefinitionResult {
    public data class Success(val aspectDefinition: AspectDefinition) : UpdateAspectDefinitionResult
    public data class NotFound(val key: String) : UpdateAspectDefinitionResult
    public data class ValidationError(val field: String, val validationFailure: ValidationFailure) : UpdateAspectDefinitionResult
}

/**
 * Response for deleting an aspect definition.
 */
public sealed interface DeleteAspectDefinitionResult {
    public object Success : DeleteAspectDefinitionResult
    public data class NotFound(val key: String) : DeleteAspectDefinitionResult
    public data class InUse(val key: String, val usageCount: Int) : DeleteAspectDefinitionResult
}

/**
 * Response for getting an aspect definition.
 */
public sealed interface GetAspectDefinitionResult {
    public data class Success(val aspectDefinition: AspectDefinition?) : GetAspectDefinitionResult
    public data class NotFound(val key: String) : GetAspectDefinitionResult
}

/**
 * Response for listing aspect definitions.
 */
public sealed interface ListAspectDefinitionsResult {
    public data class Success(val aspectDefinitions: List<AspectDefinition>) : ListAspectDefinitionsResult
}

/**
 * Response for validating aspect values.
 */
public sealed interface ValidateAspectValueResult {
    public data class Success(val validatedValues: List<String>) : ValidateAspectValueResult
    public data class ValidationFailed(val key: String, val validationFailure: ValidationFailure) : ValidateAspectValueResult
}
