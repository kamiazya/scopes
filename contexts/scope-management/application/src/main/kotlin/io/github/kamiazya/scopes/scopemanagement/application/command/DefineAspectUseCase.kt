package io.github.kamiazya.scopes.scopemanagement.application.command

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.scopemanagement.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.usecase.UseCase
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType

/**
 * Use case for defining a new aspect.
 * Creates and persists an AspectDefinition with the specified type.
 */
class DefineAspectUseCase(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) :
    UseCase<DefineAspectUseCase.Command, ScopesError, AspectDefinition> {

    data class Command(val key: String, val description: String, val type: AspectType)

    override suspend operator fun invoke(input: Command): Either<ScopesError, AspectDefinition> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(input.key).bind()

            // Check if aspect already exists
            aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error -> raise(ScopesError.SystemError("Failed to check for existing aspect: $error")) },
                { existing ->
                    if (existing != null) {
                        raise(ScopesError.AlreadyExists("Aspect definition with key '${input.key}' already exists"))
                    }
                },
            )

            // Create aspect definition based on type
            val aspectDefinition = when (input.type) {
                is AspectType.Text -> AspectDefinition.createText(
                    key = aspectKey,
                    description = input.description,
                )
                is AspectType.Numeric -> AspectDefinition.createNumeric(
                    key = aspectKey,
                    description = input.description,
                )
                is AspectType.BooleanType -> AspectDefinition.createBoolean(
                    key = aspectKey,
                    description = input.description,
                )
                is AspectType.Ordered -> AspectDefinition.createOrdered(
                    key = aspectKey,
                    description = input.description,
                    allowedValues = input.type.allowedValues,
                ).bind()
                is AspectType.Duration -> AspectDefinition.createDuration(
                    key = aspectKey,
                    description = input.description,
                )
            }

            // Save to repository
            aspectDefinitionRepository.save(aspectDefinition).fold(
                { error -> raise(ScopesError.SystemError("Failed to save aspect definition: $error")) },
                { saved -> saved },
            )
        }
    }
}
