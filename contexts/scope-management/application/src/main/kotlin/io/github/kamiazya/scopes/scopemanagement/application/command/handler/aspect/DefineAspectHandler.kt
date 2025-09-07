package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DefineAspectCommand
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import kotlinx.datetime.Clock

/**
 * Handler for defining a new aspect.
 * Creates and persists an AspectDefinition with the specified type.
 */
class DefineAspectHandler(private val aspectDefinitionRepository: AspectDefinitionRepository, private val transactionManager: TransactionManager) :
    CommandHandler<DefineAspectCommand, ScopesError, AspectDefinition> {

    override suspend operator fun invoke(command: DefineAspectCommand): Either<ScopesError, AspectDefinition> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(command.key).bind()

            // Check if aspect already exists
            aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error ->
                    raise(
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "check-existing-aspect", "key" to command.key),
                            occurredAt = Clock.System.now(),
                        ),
                    )
                },
                { existing ->
                    if (existing != null) {
                        raise(
                            ScopesError.AlreadyExists(
                                entityType = "AspectDefinition",
                                identifier = command.key,
                                identifierType = "key",
                                occurredAt = Clock.System.now(),
                            ),
                        )
                    }
                },
            )

            // Create aspect definition based on type
            val aspectDefinition = when (command.type) {
                is AspectType.Text -> AspectDefinition.createText(
                    key = aspectKey,
                    description = command.description,
                )
                is AspectType.Numeric -> AspectDefinition.createNumeric(
                    key = aspectKey,
                    description = command.description,
                )
                is AspectType.BooleanType -> AspectDefinition.createBoolean(
                    key = aspectKey,
                    description = command.description,
                )
                is AspectType.Ordered -> AspectDefinition.createOrdered(
                    key = aspectKey,
                    description = command.description,
                    allowedValues = command.type.allowedValues,
                ).bind()
                is AspectType.Duration -> AspectDefinition.createDuration(
                    key = aspectKey,
                    description = command.description,
                )
            }

            // Save to repository
            aspectDefinitionRepository.save(aspectDefinition).fold(
                { error ->
                    raise(
                        ScopesError.SystemError(
                            errorType = ScopesError.SystemError.SystemErrorType.EXTERNAL_SERVICE_ERROR,
                            service = "aspect-repository",
                            cause = error as? Throwable,
                            context = mapOf("operation" to "save-aspect-definition", "key" to command.key),
                            occurredAt = Clock.System.now(),
                        ),
                    )
                },
                { saved -> saved },
            )
        }
    }
}
