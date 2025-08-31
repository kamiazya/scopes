package io.github.kamiazya.scopes.scopemanagement.application.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.service.AspectValueValidationService
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue

/**
 * Use case for validating aspect values against their definitions.
 * Orchestrates validation by delegating business logic to domain services.
 */
class ValidateAspectValueUseCase(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val validationService: AspectValueValidationService,
) : UseCase<ValidateAspectValueUseCase.Query, ScopesError, AspectValue> {

    // Query classes for different validation scenarios
    data class Query(val key: String, val value: String)

    data class MultipleQuery(val values: Map<String, List<String>>)

    override suspend operator fun invoke(input: Query): Either<ScopesError, AspectValue> {
        // Parse the aspect key
        val aspectKey = AspectKey.create(input.key).fold(
            { return it.left() },
            { it },
        )

        // Find the aspect definition
        val definition = aspectDefinitionRepository.findByKey(aspectKey).fold(
            { return ScopesError.SystemError("Failed to find aspect definition: $it").left() },
            {
                it ?: return ScopesError.NotFound("Aspect definition not found for key: ${input.key}").left()
            },
        )

        // Create the aspect value
        val aspectValue = AspectValue.create(input.value).fold(
            { return it.left() },
            { it },
        )

        // Delegate validation to domain service
        return validationService.validateValue(definition, aspectValue)
    }

    /**
     * Validate a single aspect value against its definition.
     * @param key The aspect key
     * @param value The aspect value to validate
     * @return Either an error or the validated AspectValue
     */
    suspend fun execute(key: String, value: String): Either<ScopesError, AspectValue> = invoke(Query(key, value))

    /**
     * Validate multiple aspect values.
     * @param query MultipleQuery containing the values to validate
     * @return Either an error (first validation failure) or the validated values
     */
    suspend operator fun invoke(query: MultipleQuery): Either<ScopesError, Map<AspectKey, List<AspectValue>>> = executeMultiple(query.values)

    /**
     * Validate multiple aspect values.
     * @param values Map of aspect key to values
     * @return Either an error (first validation failure) or the validated values
     */
    suspend fun executeMultiple(values: Map<String, List<String>>): Either<ScopesError, Map<AspectKey, List<AspectValue>>> {
        val validatedValues = mutableMapOf<AspectKey, List<AspectValue>>()

        for ((key, valueList) in values) {
            // Parse the aspect key
            val aspectKey = AspectKey.create(key).fold(
                { return it.left() },
                { it },
            )

            // Find the aspect definition
            val definition = aspectDefinitionRepository.findByKey(aspectKey).fold(
                { return ScopesError.SystemError("Failed to find aspect definition: $it").left() },
                {
                    it ?: return ScopesError.NotFound("Aspect definition not found for key: $key").left()
                },
            )

            // Check if multiple values are allowed using domain service
            validationService.validateMultipleValuesAllowed(definition, valueList.size).fold(
                { return it.left() },
                { },
            )

            // Validate each value
            val validatedList = mutableListOf<AspectValue>()
            for (value in valueList) {
                val aspectValue = AspectValue.create(value).fold(
                    { return it.left() },
                    { it },
                )

                validationService.validateValue(definition, aspectValue).fold(
                    { return it.left() },
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
    fun validateRequired(providedKeys: Set<String>, requiredKeys: Set<String>): Either<ScopesError, Unit> =
        validationService.validateRequiredAspects(providedKeys, requiredKeys)
}
