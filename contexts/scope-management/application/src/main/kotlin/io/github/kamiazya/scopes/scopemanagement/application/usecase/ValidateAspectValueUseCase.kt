package io.github.kamiazya.scopes.scopemanagement.application.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.application.error.ScopeManagementApplicationError
import io.github.kamiazya.scopes.scopemanagement.application.error.toGenericApplicationError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.validation.AspectValueValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Use case for validating aspect values against their definitions.
 * Orchestrates validation by delegating business logic to domain services.
 */
class ValidateAspectValueUseCase(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val validationService: AspectValueValidationService,
) : UseCase<ValidateAspectValueUseCase.Query, ScopeManagementApplicationError, AspectValue> {

    // Query classes for different validation scenarios
    data class Query(val key: String, val value: String)

    data class MultipleQuery(val values: Map<String, List<String>>)

    override suspend operator fun invoke(input: Query): Either<ScopeManagementApplicationError, AspectValue> {
        // Parse the aspect key
        val aspectKey = AspectKey.create(input.key).fold(
            { return it.toGenericApplicationError().left() },
            { it },
        )

        // Find the aspect definition
        val definition = aspectDefinitionRepository.findByKey(aspectKey).fold(
            {
                return ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                    operation = "find-aspect-definition",
                    errorCause = it.toString(),
                ).left()
            },
            {
                it ?: return ScopeManagementApplicationError.PersistenceError.NotFound(
                    entityType = "AspectDefinition",
                    entityId = input.key,
                ).left()
            },
        )

        // Create the aspect value
        val aspectValue = AspectValue.create(input.value).fold(
            { return it.toGenericApplicationError().left() },
            { it },
        )

        // Delegate validation to domain service
        return validationService.validateValue(definition, aspectValue)
            .mapLeft { it.toGenericApplicationError() }
    }

    /**
     * Validate a single aspect value against its definition.
     * @param key The aspect key
     * @param value The aspect value to validate
     * @return Either an error or the validated AspectValue
     */
    suspend fun execute(key: String, value: String): Either<ScopeManagementApplicationError, AspectValue> = invoke(Query(key, value))

    /**
     * Validate multiple aspect values.
     * @param query MultipleQuery containing the values to validate
     * @return Either an error (first validation failure) or the validated values
     */
    suspend operator fun invoke(query: MultipleQuery): Either<ScopeManagementApplicationError, Map<AspectKey, List<AspectValue>>> =
        executeMultiple(query.values)

    /**
     * Validate multiple aspect values.
     * @param values Map of aspect key to values
     * @return Either an error (first validation failure) or the validated values
     */
    suspend fun executeMultiple(values: Map<String, List<String>>): Either<ScopeManagementApplicationError, Map<AspectKey, List<AspectValue>>> {
        val validatedValues = mutableMapOf<AspectKey, List<AspectValue>>()

        for ((key, valueList) in values) {
            // Parse the aspect key
            val aspectKey = AspectKey.create(key).fold(
                { return it.toGenericApplicationError().left() },
                { it },
            )

            // Find the aspect definition
            val definition = aspectDefinitionRepository.findByKey(aspectKey).fold(
                {
                    return ScopeManagementApplicationError.PersistenceError.StorageUnavailable(
                        operation = "find-aspect-definition",
                        errorCause = it.toString(),
                    ).left()
                },
                {
                    it ?: return ScopeManagementApplicationError.PersistenceError.NotFound(
                        entityType = "AspectDefinition",
                        entityId = key,
                    ).left()
                },
            )

            // Check if multiple values are allowed using domain service
            validationService.validateMultipleValuesAllowed(definition, valueList.size).fold(
                { return it.toGenericApplicationError().left() },
                { },
            )

            // Validate each value
            val validatedList = mutableListOf<AspectValue>()
            for (value in valueList) {
                val aspectValue = AspectValue.create(value).fold(
                    { return it.toGenericApplicationError().left() },
                    { it },
                )

                validationService.validateValue(definition, aspectValue).fold(
                    { return it.toGenericApplicationError().left() },
                    { validatedList.add(it) },
                )
            }

            validatedValues[aspectKey] = validatedList
        }

        return validatedValues.right()
    }

    /**
     * Validate required aspects are present.
     * @param providedKeys Set of provided aspect keys
     * @param requiredKeys Set of required aspect keys
     * @return Either an error or Unit if all required aspects are present
     */
    fun validateRequired(providedKeys: Set<String>, requiredKeys: Set<String>): Either<ScopeManagementApplicationError, Unit> =
        validationService.validateRequiredAspects(providedKeys, requiredKeys)
            .mapLeft { it.toGenericApplicationError() }
}
