package io.github.kamiazya.scopes.scopemanagement.application.command.handler.aspect

import arrow.core.Either
import arrow.core.raise.either
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.platform.application.handler.CommandHandler
import io.github.kamiazya.scopes.platform.application.port.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.application.command.dto.aspect.DefineAspectCommand
import io.github.kamiazya.scopes.scopemanagement.application.mapper.ApplicationErrorMapper
import io.github.kamiazya.scopes.scopemanagement.domain.entity.AspectDefinition
import io.github.kamiazya.scopes.scopemanagement.domain.repository.AspectDefinitionRepository
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectKey
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType

/**
 * Handler for defining a new aspect.
 * Creates and persists an AspectDefinition with the specified type.
 */
class DefineAspectHandler(
    private val aspectDefinitionRepository: AspectDefinitionRepository,
    private val transactionManager: TransactionManager,
    private val applicationErrorMapper: ApplicationErrorMapper,
) : CommandHandler<DefineAspectCommand, ScopeContractError, AspectDefinition> {

    override suspend operator fun invoke(command: DefineAspectCommand): Either<ScopeContractError, AspectDefinition> = transactionManager.inTransaction {
        either {
            // Validate and create aspect key
            val aspectKey = AspectKey.create(command.key)
                .mapLeft { applicationErrorMapper.mapDomainError(it) }
                .bind()

            // Check if aspect already exists
            aspectDefinitionRepository.findByKey(aspectKey).fold(
                { error ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "aspect-definition-repository",
                        ),
                    )
                },
                { existing ->
                    if (existing != null) {
                        raise(
                            ScopeContractError.BusinessError.DuplicateTitle(
                                title = command.key,
                                parentId = null,
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
                ).mapLeft { applicationErrorMapper.mapDomainError(it) }.bind()
                is AspectType.Duration -> AspectDefinition.createDuration(
                    key = aspectKey,
                    description = command.description,
                )
            }

            // Save to repository
            aspectDefinitionRepository.save(aspectDefinition).fold(
                { error ->
                    raise(
                        ScopeContractError.SystemError.ServiceUnavailable(
                            service = "aspect-definition-repository",
                        ),
                    )
                },
                { saved -> saved },
            )
        }
    }
}
