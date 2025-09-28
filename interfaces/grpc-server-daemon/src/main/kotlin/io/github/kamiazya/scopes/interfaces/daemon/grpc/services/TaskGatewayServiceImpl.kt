package io.github.kamiazya.scopes.interfaces.daemon.grpc.services

import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ContextViewQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementCommandPort
import io.github.kamiazya.scopes.contracts.scopemanagement.ScopeManagementQueryPort
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.AddAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.CreateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.DeleteScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.RemoveAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetActiveContextCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.SetCanonicalAliasCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateContextViewCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.commands.UpdateScopeCommand
import io.github.kamiazya.scopes.contracts.scopemanagement.errors.ScopeContractError
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetActiveContextQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetAspectDefinitionQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetContextViewQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListAliasesQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ListContextViewsQuery
import io.github.kamiazya.scopes.contracts.scopemanagement.queries.ValidateAspectValueQuery
import io.github.kamiazya.scopes.platform.observability.logging.Logger
import io.github.kamiazya.scopes.rpc.v1beta.Envelope
import io.github.kamiazya.scopes.rpc.v1beta.TaskGatewayServiceGrpcKt
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Implementation of the TaskGatewayService for generic command and query execution.
 *
 * This service acts as a gateway that receives generic Envelope messages and routes
 * them to appropriate handlers based on the 'kind' field.
 */
class TaskGatewayServiceImpl(
    private val scopeManagementCommandPort: ScopeManagementCommandPort?,
    private val scopeManagementQueryPort: ScopeManagementQueryPort?,
    private val contextViewCommandPort: ContextViewCommandPort?,
    private val contextViewQueryPort: ContextViewQueryPort?,
    private val json: Json,
    private val logger: Logger,
) : TaskGatewayServiceGrpcKt.TaskGatewayServiceCoroutineImplBase() {

    companion object {
        // Supported command kinds
        const val CREATE_SCOPE_COMMAND = "scopes.commands.CreateScope"
        const val UPDATE_SCOPE_COMMAND = "scopes.commands.UpdateScope"
        const val DELETE_SCOPE_COMMAND = "scopes.commands.DeleteScope"
        const val ADD_ALIAS_COMMAND = "scopes.commands.AddAlias"
        const val REMOVE_ALIAS_COMMAND = "scopes.commands.RemoveAlias"
        const val SET_CANONICAL_ALIAS_COMMAND = "scopes.commands.SetCanonicalAlias"

        // Context command kinds
        const val CREATE_CONTEXT_VIEW_COMMAND = "scopes.commands.CreateContextView"
        const val UPDATE_CONTEXT_VIEW_COMMAND = "scopes.commands.UpdateContextView"
        const val DELETE_CONTEXT_VIEW_COMMAND = "scopes.commands.DeleteContextView"
        const val SET_ACTIVE_CONTEXT_COMMAND = "scopes.commands.SetActiveContext"
        const val CLEAR_ACTIVE_CONTEXT_COMMAND = "scopes.commands.ClearActiveContext"

        // Supported query kinds
        const val GET_SCOPE_QUERY = "scopes.queries.GetScope"
        const val GET_ROOT_SCOPES_QUERY = "scopes.queries.GetRootScopes"
        const val GET_CHILDREN_QUERY = "scopes.queries.GetChildren"
        const val LIST_ALIASES_QUERY = "scopes.queries.ListAliases"

        // Context query kinds
        const val LIST_CONTEXT_VIEWS_QUERY = "scopes.queries.ListContextViews"
        const val GET_CONTEXT_VIEW_QUERY = "scopes.queries.GetContextView"
        const val GET_ACTIVE_CONTEXT_QUERY = "scopes.queries.GetActiveContext"

        // Aspect command kinds
        const val CREATE_ASPECT_DEFINITION_COMMAND = "scopes.commands.CreateAspectDefinition"
        const val UPDATE_ASPECT_DEFINITION_COMMAND = "scopes.commands.UpdateAspectDefinition"
        const val DELETE_ASPECT_DEFINITION_COMMAND = "scopes.commands.DeleteAspectDefinition"

        // Aspect query kinds
        const val GET_ASPECT_DEFINITION_QUERY = "scopes.queries.GetAspectDefinition"
        const val LIST_ASPECT_DEFINITIONS_QUERY = "scopes.queries.ListAspectDefinitions"
        const val VALIDATE_ASPECT_VALUE_QUERY = "scopes.queries.ValidateAspectValue"

        // Supported versions
        const val VERSION_V1 = "v1"

        // Headers
        const val HEADER_USER_ID = "user-id"
        const val HEADER_CLIENT_VERSION = "client-version"

        internal fun mapContractErrorToGrpc(error: ScopeContractError): Pair<Status, String> = when (error) {
            is ScopeContractError.InputError -> mapInputErrorToGrpc(error)
            is ScopeContractError.BusinessError -> mapBusinessErrorToGrpc(error)
            is ScopeContractError.SystemError -> mapSystemErrorToGrpc(error)
            is ScopeContractError.DataInconsistency -> mapDataInconsistencyToGrpc(error)
        }

        private fun mapInputErrorToGrpc(error: ScopeContractError.InputError): Pair<Status, String> = when (error) {
            is ScopeContractError.InputError.InvalidId ->
                Status.INVALID_ARGUMENT to "Invalid ID format: ${error.id}${error.expectedFormat?.let { ". Expected: $it" } ?: ""}"

            is ScopeContractError.InputError.InvalidTitle -> {
                val details = when (val validation = error.validationFailure) {
                    is ScopeContractError.TitleValidationFailure.Empty ->
                        "Title cannot be empty"
                    is ScopeContractError.TitleValidationFailure.TooShort ->
                        "Title is too short (minimum ${validation.minimumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.TitleValidationFailure.TooLong ->
                        "Title is too long (maximum ${validation.maximumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.TitleValidationFailure.InvalidCharacters ->
                        "Title contains invalid characters: ${validation.prohibitedCharacters.joinToString()}"
                }
                Status.INVALID_ARGUMENT to details
            }

            is ScopeContractError.InputError.InvalidDescription -> {
                val details = when (val validation = error.validationFailure) {
                    is ScopeContractError.DescriptionValidationFailure.TooLong ->
                        "Description is too long (maximum ${validation.maximumLength} characters, got ${validation.actualLength})"
                }
                Status.INVALID_ARGUMENT to details
            }

            is ScopeContractError.InputError.InvalidParentId ->
                Status.INVALID_ARGUMENT to "Invalid parent ID format: ${error.parentId}${error.expectedFormat?.let { ". Expected: $it" } ?: ""}"

            is ScopeContractError.InputError.InvalidAlias -> {
                val details = when (val validation = error.validationFailure) {
                    is ScopeContractError.AliasValidationFailure.Empty ->
                        "Alias cannot be empty"
                    is ScopeContractError.AliasValidationFailure.TooShort ->
                        "Alias is too short (minimum ${validation.minimumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.AliasValidationFailure.TooLong ->
                        "Alias is too long (maximum ${validation.maximumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.AliasValidationFailure.InvalidFormat ->
                        "Alias has invalid format. Expected: ${validation.expectedPattern}"
                }
                Status.INVALID_ARGUMENT to details
            }

            is ScopeContractError.InputError.InvalidContextKey -> {
                val details = when (val validation = error.validationFailure) {
                    is ScopeContractError.ContextKeyValidationFailure.Empty ->
                        "Context key cannot be empty"
                    is ScopeContractError.ContextKeyValidationFailure.TooShort ->
                        "Context key is too short (minimum ${validation.minimumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.ContextKeyValidationFailure.TooLong ->
                        "Context key is too long (maximum ${validation.maximumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.ContextKeyValidationFailure.InvalidFormat ->
                        "Context key has invalid format: ${validation.invalidType}"
                }
                Status.INVALID_ARGUMENT to details
            }

            is ScopeContractError.InputError.InvalidContextName -> {
                val details = when (val validation = error.validationFailure) {
                    is ScopeContractError.ContextNameValidationFailure.Empty ->
                        "Context name cannot be empty"
                    is ScopeContractError.ContextNameValidationFailure.TooLong ->
                        "Context name is too long (maximum ${validation.maximumLength} characters, got ${validation.actualLength})"
                }
                Status.INVALID_ARGUMENT to details
            }

            is ScopeContractError.InputError.InvalidContextFilter -> {
                val details = when (val validation = error.validationFailure) {
                    is ScopeContractError.ContextFilterValidationFailure.Empty ->
                        "Context filter cannot be empty"
                    is ScopeContractError.ContextFilterValidationFailure.TooShort ->
                        "Context filter is too short (minimum ${validation.minimumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.ContextFilterValidationFailure.TooLong ->
                        "Context filter is too long (maximum ${validation.maximumLength} characters, got ${validation.actualLength})"
                    is ScopeContractError.ContextFilterValidationFailure.InvalidSyntax ->
                        "Invalid filter syntax: ${validation.errorType} in expression '${validation.expression}'${validation.position?.let {
                            " at position $it"
                        } ?: ""}"
                }
                Status.INVALID_ARGUMENT to details
            }

            is ScopeContractError.InputError.ValidationFailure -> {
                val details = when (val constraint = error.constraint) {
                    is ScopeContractError.ValidationConstraint.Empty ->
                        "${error.field} cannot be empty"
                    is ScopeContractError.ValidationConstraint.TooShort ->
                        "${error.field} is too short (minimum ${constraint.minimumLength} characters, got ${constraint.actualLength})"
                    is ScopeContractError.ValidationConstraint.TooLong ->
                        "${error.field} is too long (maximum ${constraint.maximumLength} characters, got ${constraint.actualLength})"
                    is ScopeContractError.ValidationConstraint.InvalidFormat ->
                        "${error.field} has invalid format. Expected: ${constraint.expectedFormat}"
                    is ScopeContractError.ValidationConstraint.InvalidType ->
                        "${error.field} has invalid type. Expected: ${constraint.expectedType}, got: ${constraint.actualType}"
                    is ScopeContractError.ValidationConstraint.InvalidValue ->
                        "${error.field} has invalid value: '${constraint.actualValue}'${constraint.expectedValues?.let {
                            ". Expected one of: ${it.joinToString()}"
                        } ?: ""}"
                    is ScopeContractError.ValidationConstraint.EmptyValues ->
                        "${constraint.field} cannot have empty values"
                    is ScopeContractError.ValidationConstraint.MultipleValuesNotAllowed ->
                        "${constraint.field} cannot have multiple values"
                    is ScopeContractError.ValidationConstraint.RequiredField ->
                        "${constraint.field} is required"
                }
                Status.INVALID_ARGUMENT to details
            }
        }

        private fun mapBusinessErrorToGrpc(error: ScopeContractError.BusinessError): Pair<Status, String> = when (error) {
            is ScopeContractError.BusinessError.NotFound ->
                Status.NOT_FOUND to "Scope not found: ${error.scopeId}"

            is ScopeContractError.BusinessError.DuplicateTitle ->
                Status.ALREADY_EXISTS to
                    "Title '${error.title}' already exists within parent scope ${error.parentId ?: "root"}${error.existingScopeId?.let {
                        " (existing scope: $it)"
                    } ?: ""}"

            is ScopeContractError.BusinessError.HierarchyViolation -> {
                val details = when (val violation = error.violation) {
                    is ScopeContractError.HierarchyViolationType.CircularReference ->
                        "Circular reference detected: scope ${violation.scopeId} cannot have parent ${violation.parentId}${violation.cyclePath?.let {
                            ". Cycle path: ${it.joinToString(" -> ")}"
                        } ?: ""}"
                    is ScopeContractError.HierarchyViolationType.MaxDepthExceeded ->
                        "Maximum hierarchy depth exceeded for scope ${violation.scopeId}: attempted depth ${violation.attemptedDepth}, maximum allowed ${violation.maximumDepth}"
                    is ScopeContractError.HierarchyViolationType.MaxChildrenExceeded ->
                        "Maximum children limit exceeded for parent ${violation.parentId}: current count ${violation.currentChildrenCount}, maximum allowed ${violation.maximumChildren}"
                    is ScopeContractError.HierarchyViolationType.SelfParenting ->
                        "Scope ${violation.scopeId} cannot be its own parent"
                    is ScopeContractError.HierarchyViolationType.ParentNotFound ->
                        "Parent scope not found: ${violation.parentId} for scope ${violation.scopeId}"
                }
                Status.FAILED_PRECONDITION to details
            }

            is ScopeContractError.BusinessError.AlreadyDeleted ->
                Status.FAILED_PRECONDITION to "Scope ${error.scopeId} is already deleted"

            is ScopeContractError.BusinessError.ArchivedScope ->
                Status.FAILED_PRECONDITION to "Cannot modify archived scope ${error.scopeId}. Unarchive the scope first"

            is ScopeContractError.BusinessError.NotArchived ->
                Status.FAILED_PRECONDITION to "Scope ${error.scopeId} is not archived"

            is ScopeContractError.BusinessError.HasChildren ->
                Status.FAILED_PRECONDITION to
                    "Cannot delete scope ${error.scopeId} that has ${error.childrenCount ?: "one or more"} children. Delete children first"

            is ScopeContractError.BusinessError.AliasNotFound ->
                Status.NOT_FOUND to "Alias not found: ${error.alias}"

            is ScopeContractError.BusinessError.DuplicateAlias ->
                Status.ALREADY_EXISTS to
                    "Alias '${error.alias}' already exists${error.existingScopeId?.let {
                        " for scope $it"
                    } ?: ""}${error.attemptedScopeId?.let { " (attempted by scope $it)" } ?: ""}"

            is ScopeContractError.BusinessError.CannotRemoveCanonicalAlias ->
                Status.FAILED_PRECONDITION to "Cannot remove canonical alias '${error.aliasName}' from scope ${error.scopeId}"

            is ScopeContractError.BusinessError.AliasOfDifferentScope ->
                Status.FAILED_PRECONDITION to "Alias '${error.alias}' belongs to scope ${error.actualScopeId}, not ${error.expectedScopeId}"

            is ScopeContractError.BusinessError.AliasGenerationFailed ->
                Status.INTERNAL to "Failed to generate alias for scope ${error.scopeId} after ${error.retryCount} retries"

            is ScopeContractError.BusinessError.AliasGenerationValidationFailed ->
                Status.INTERNAL to "Generated alias '${error.alias}' for scope ${error.scopeId} failed validation: ${error.reason}"

            is ScopeContractError.BusinessError.ContextNotFound ->
                Status.NOT_FOUND to "Context view not found: ${error.contextKey}"

            is ScopeContractError.BusinessError.DuplicateContextKey ->
                Status.ALREADY_EXISTS to "Context key '${error.contextKey}' already exists${error.existingContextId?.let { " (ID: $it)" } ?: ""}"
        }

        private fun mapSystemErrorToGrpc(error: ScopeContractError.SystemError): Pair<Status, String> = when (error) {
            is ScopeContractError.SystemError.ServiceUnavailable ->
                Status.UNAVAILABLE to "Service '${error.service}' is temporarily unavailable. Please try again later"

            is ScopeContractError.SystemError.Timeout ->
                Status.DEADLINE_EXCEEDED to "Operation '${error.operation}' timed out after ${error.timeout}"

            is ScopeContractError.SystemError.ConcurrentModification ->
                Status.ABORTED to
                    "Concurrent modification detected for scope ${error.scopeId}: expected version ${error.expectedVersion}, actual version ${error.actualVersion}"
        }

        private fun mapDataInconsistencyToGrpc(error: ScopeContractError.DataInconsistency): Pair<Status, String> = when (error) {
            is ScopeContractError.DataInconsistency.MissingCanonicalAlias ->
                Status.INTERNAL to "Data inconsistency: Missing canonical alias for scope ${error.scopeId}"
        }
    }

    override suspend fun executeCommand(request: Envelope): Envelope {
        logger.info(
            "Executing command",
            mapOf(
                "envelope.id" to request.id,
                "envelope.kind" to request.kind,
                "envelope.version" to request.version,
                "envelope.correlationId" to request.correlationId,
            ) as Map<String, Any>,
        )

        return try {
            when (request.kind) {
                CREATE_SCOPE_COMMAND -> handleCreateScopeCommand(request)
                UPDATE_SCOPE_COMMAND -> handleUpdateScopeCommand(request)
                DELETE_SCOPE_COMMAND -> handleDeleteScopeCommand(request)
                ADD_ALIAS_COMMAND -> handleAddAliasCommand(request)
                REMOVE_ALIAS_COMMAND -> handleRemoveAliasCommand(request)
                SET_CANONICAL_ALIAS_COMMAND -> handleSetCanonicalAliasCommand(request)
                CREATE_CONTEXT_VIEW_COMMAND -> handleCreateContextViewCommand(request)
                UPDATE_CONTEXT_VIEW_COMMAND -> handleUpdateContextViewCommand(request)
                DELETE_CONTEXT_VIEW_COMMAND -> handleDeleteContextViewCommand(request)
                SET_ACTIVE_CONTEXT_COMMAND -> handleSetActiveContextCommand(request)
                CLEAR_ACTIVE_CONTEXT_COMMAND -> handleClearActiveContextCommand(request)
                CREATE_ASPECT_DEFINITION_COMMAND -> handleCreateAspectDefinitionCommand(request)
                UPDATE_ASPECT_DEFINITION_COMMAND -> handleUpdateAspectDefinitionCommand(request)
                DELETE_ASPECT_DEFINITION_COMMAND -> handleDeleteAspectDefinitionCommand(request)
                else -> throw StatusException(Status.UNIMPLEMENTED.withDescription("Unknown command kind: ${request.kind}"))
            }
        } catch (e: StatusException) {
            // Re-throw status exceptions as-is to propagate proper gRPC status
            throw e
        } catch (e: Exception) {
            logger.error(
                "Error executing command",
                mapOf<String, Any>(
                    "envelope.id" to request.id,
                    "envelope.kind" to request.kind,
                ),
                e,
            )
            throw StatusException(Status.INTERNAL.withDescription("Internal error executing command: ${e.message}"))
        }
    }

    override suspend fun query(request: Envelope): Envelope {
        logger.info(
            "Executing query",
            mapOf<String, Any>(
                "envelope.id" to request.id,
                "envelope.kind" to request.kind,
                "envelope.version" to request.version,
                "envelope.correlationId" to request.correlationId,
            ),
        )

        return try {
            when (request.kind) {
                GET_SCOPE_QUERY -> handleGetScopeQuery(request)
                GET_ROOT_SCOPES_QUERY -> handleGetRootScopesQuery(request)
                GET_CHILDREN_QUERY -> handleGetChildrenQuery(request)
                LIST_ALIASES_QUERY -> handleListAliasesQuery(request)
                LIST_CONTEXT_VIEWS_QUERY -> handleListContextViewsQuery(request)
                GET_CONTEXT_VIEW_QUERY -> handleGetContextViewQuery(request)
                GET_ACTIVE_CONTEXT_QUERY -> handleGetActiveContextQuery(request)
                GET_ASPECT_DEFINITION_QUERY -> handleGetAspectDefinitionQuery(request)
                LIST_ASPECT_DEFINITIONS_QUERY -> handleListAspectDefinitionsQuery(request)
                VALIDATE_ASPECT_VALUE_QUERY -> handleValidateAspectValueQuery(request)
                else -> throw StatusException(Status.UNIMPLEMENTED.withDescription("Unknown query kind: ${request.kind}"))
            }
        } catch (e: StatusException) {
            // Re-throw status exceptions as-is to propagate proper gRPC status
            throw e
        } catch (e: Exception) {
            logger.error(
                "Error executing query",
                mapOf<String, Any>(
                    "envelope.id" to request.id,
                    "envelope.kind" to request.kind,
                ),
                e,
            )
            throw StatusException(Status.INTERNAL.withDescription("Internal error executing query: ${e.message}"))
        }
    }

    override fun streamEvents(requests: Flow<Envelope>): Flow<Envelope> {
        logger.info("StreamEvents requested - implementing basic progress updates")

        return flow {
            // Collect incoming requests (could be subscription filters, etc.)
            requests.collect { request ->
                logger.debug(
                    "Received stream request",
                    mapOf(
                        "envelope.id" to request.id,
                        "envelope.kind" to request.kind,
                        "envelope.correlationId" to request.correlationId,
                    ) as Map<String, Any>,
                )

                when (request.kind) {
                    "scopes.stream.Subscribe" -> {
                        // Handle subscription request
                        logger.info("Client subscribed to event stream")

                        // Send initial "connected" event
                        val connectedEvent = createStreamEvent(
                            correlationId = request.correlationId,
                            kind = "scopes.events.Connected",
                            payload = json.encodeToString(
                                ConnectedEventData(
                                    message = "Connected to event stream",
                                    timestamp = Clock.System.now().toString(),
                                ),
                            ),
                        )
                        emit(connectedEvent)

                        // For M3 minimal implementation, we'll emit periodic heartbeat events
                        // In real implementation, this would emit actual operation progress
                        repeat(5) { i ->
                            delay(1000) // 1 second intervals

                            val progressEvent = createStreamEvent(
                                correlationId = request.correlationId,
                                kind = "scopes.events.ProgressUpdate",
                                payload = json.encodeToString(
                                    ProgressUpdateEventData(
                                        operationId = "demo-operation",
                                        percentage = (i + 1) * 20,
                                        message = "Processing step ${i + 1} of 5",
                                        estimatedSecondsRemaining = (4 - i),
                                        metadata = mapOf("step" to "demo-step-${i + 1}"),
                                    ),
                                ),
                            )
                            emit(progressEvent)
                        }

                        // Send completion event
                        val completedEvent = createStreamEvent(
                            correlationId = request.correlationId,
                            kind = "scopes.events.OperationCompleted",
                            payload = json.encodeToString(
                                OperationCompletedEventData(
                                    operationId = "demo-operation",
                                    message = "Operation completed successfully",
                                    timestamp = Clock.System.now().toString(),
                                ),
                            ),
                        )
                        emit(completedEvent)

                        logger.info("Event stream completed for subscription")
                    }

                    "scopes.stream.Unsubscribe" -> {
                        logger.info("Client unsubscribed from event stream")
                        // In real implementation, we'd clean up subscriptions
                        return@collect
                    }

                    else -> {
                        logger.warn("Unknown stream request kind: ${request.kind}")
                        val errorEvent = createStreamEvent(
                            correlationId = request.correlationId,
                            kind = "scopes.events.Error",
                            payload = json.encodeToString(
                                ErrorEventData(
                                    error = "Unknown stream request kind: ${request.kind}",
                                    timestamp = Clock.System.now().toString(),
                                ),
                            ),
                        )
                        emit(errorEvent)
                    }
                }
            }
        }.catch { e ->
            logger.error("Error in stream events", emptyMap(), e)
            // Emit error event before completing
            emit(
                createStreamEvent(
                    correlationId = "", // No specific correlation ID available
                    kind = "scopes.events.Error",
                    payload = json.encodeToString(
                        ErrorEventData(
                            error = "Stream error: ${e.message}",
                            timestamp = Clock.System.now().toString(),
                        ),
                    ),
                ),
            )
        }
    }

    private suspend fun handleCreateScopeCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<CreateScopeCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode CreateScope command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = when (commandData.customAlias) {
            null -> CreateScopeCommand.WithAutoAlias(
                title = commandData.title,
                description = commandData.description,
                parentId = commandData.parentId,
            )
            else -> CreateScopeCommand.WithCustomAlias(
                title = commandData.title,
                description = commandData.description,
                parentId = commandData.parentId,
                alias = commandData.customAlias,
            )
        }

        // Execute command via port
        return try {
            if (scopeManagementCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management service unavailable"))
            }

            val result = scopeManagementCommandPort.createScope(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { scopeResult ->
                    // Encode result
                    val responseData = CreateScopeResponseData(
                        id = scopeResult.id,
                        title = scopeResult.title,
                        description = scopeResult.description,
                        parentId = scopeResult.parentId,
                        canonicalAlias = scopeResult.canonicalAlias,
                        createdAt = scopeResult.createdAt.toString(),
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.CreateScopeResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error creating scope", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleUpdateScopeCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<UpdateScopeCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode UpdateScope command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = UpdateScopeCommand(
            id = commandData.id,
            title = commandData.title,
            description = commandData.description,
            parentId = commandData.parentId,
        )

        // Execute command via port
        return try {
            if (scopeManagementCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management service unavailable"))
            }

            val result = scopeManagementCommandPort.updateScope(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { scopeResult ->
                    // Encode result
                    val responseData = UpdateScopeResponseData(
                        id = scopeResult.id,
                        title = scopeResult.title,
                        description = scopeResult.description,
                        parentId = scopeResult.parentId,
                        canonicalAlias = scopeResult.canonicalAlias,
                        createdAt = scopeResult.createdAt.toString(),
                        updatedAt = scopeResult.updatedAt.toString(),
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.UpdateScopeResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating scope", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleDeleteScopeCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<DeleteScopeCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode DeleteScope command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = DeleteScopeCommand(
            id = commandData.id,
            cascade = commandData.cascade,
        )

        // Execute command via port
        return try {
            if (scopeManagementCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management service unavailable"))
            }

            val result = scopeManagementCommandPort.deleteScope(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Encode result
                    val responseData = DeleteScopeResponseData(
                        deletedScopeId = commandData.id,
                        deletedChildrenCount = 0, // We don't track this in the current implementation
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.DeleteScopeResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error deleting scope", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleAddAliasCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<AddAliasCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode AddAlias command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = AddAliasCommand(
            scopeId = commandData.scopeId,
            aliasName = commandData.aliasName,
        )

        // Execute command via port
        return try {
            if (scopeManagementCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management service unavailable"))
            }

            val result = scopeManagementCommandPort.addAlias(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Encode result
                    val responseData = AddAliasResponseData(
                        scopeId = commandData.scopeId,
                        aliasName = commandData.aliasName,
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.AddAliasResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error adding alias", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleRemoveAliasCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<RemoveAliasCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode RemoveAlias command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = RemoveAliasCommand(
            scopeId = commandData.scopeId,
            aliasName = commandData.aliasName,
        )

        // Execute command via port
        return try {
            if (scopeManagementCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management service unavailable"))
            }

            val result = scopeManagementCommandPort.removeAlias(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Encode result
                    val responseData = RemoveAliasResponseData(
                        scopeId = commandData.scopeId,
                        aliasName = commandData.aliasName,
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.RemoveAliasResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error removing alias", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleSetCanonicalAliasCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<SetCanonicalAliasCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode SetCanonicalAlias command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = SetCanonicalAliasCommand(
            scopeId = commandData.scopeId,
            aliasName = commandData.aliasName,
        )

        // Execute command via port
        return try {
            if (scopeManagementCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management service unavailable"))
            }

            val result = scopeManagementCommandPort.setCanonicalAlias(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Encode result
                    val responseData = SetCanonicalAliasResponseData(
                        scopeId = commandData.scopeId,
                        aliasName = commandData.aliasName,
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.SetCanonicalAliasResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error setting canonical alias", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleCreateContextViewCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<CreateContextViewCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode CreateContextView command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = CreateContextViewCommand(
            key = commandData.key,
            name = commandData.name,
            filter = commandData.filter,
            description = commandData.description,
        )

        // Execute command via port
        return try {
            if (contextViewCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view service unavailable"))
            }

            val result = contextViewCommandPort.createContextView(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Command successful, return minimal response
                    val responseData = CreateContextViewResponseData(
                        key = commandData.key,
                        name = commandData.name,
                        filter = commandData.filter,
                        description = commandData.description,
                        createdAt = Clock.System.now().toString(),
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.CreateContextViewResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error creating context view", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleUpdateContextViewCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<UpdateContextViewCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode UpdateContextView command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = UpdateContextViewCommand(
            key = commandData.key,
            name = commandData.name,
            filter = commandData.filter,
            description = commandData.description,
        )

        // Execute command via port
        return try {
            if (contextViewCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view service unavailable"))
            }

            val result = contextViewCommandPort.updateContextView(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Command successful, return minimal response
                    // Note: In strict CQRS, we'd need to query to get the actual updated values
                    val responseData = UpdateContextViewResponseData(
                        key = commandData.key,
                        name = commandData.name ?: "", // We don't know the actual name if not provided
                        filter = commandData.filter ?: "", // We don't know the actual filter if not provided
                        description = commandData.description,
                        updatedAt = Clock.System.now().toString(),
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.UpdateContextViewResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error updating context view", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleDeleteContextViewCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<DeleteContextViewCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode DeleteContextView command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = DeleteContextViewCommand(
            key = commandData.key,
        )

        // Execute command via port
        return try {
            if (contextViewCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view service unavailable"))
            }

            val result = contextViewCommandPort.deleteContextView(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Encode result
                    val responseData = DeleteContextViewResponseData(
                        deletedKey = commandData.key,
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.DeleteContextViewResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error deleting context view", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleSetActiveContextCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<SetActiveContextCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode SetActiveContext command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract command
        val command = SetActiveContextCommand(
            key = commandData.key,
        )

        // Execute command via port
        return try {
            if (contextViewCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view service unavailable"))
            }

            val result = contextViewCommandPort.setActiveContext(command)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Command successful, return minimal response
                    // Note: In strict CQRS, we'd need to query to get the actual context details
                    val responseData = SetActiveContextResponseData(
                        key = commandData.key,
                        name = "", // We don't have the name from the command
                        filter = "", // We don't have the filter from the command
                        description = null,
                        activatedAt = Clock.System.now().toString(),
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.SetActiveContextResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error setting active context", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleClearActiveContextCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // No payload to decode for clearActiveContext

        // Execute command via port
        return try {
            if (contextViewCommandPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view service unavailable"))
            }

            val result = contextViewCommandPort.clearActiveContext()

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                {
                    // Command successful, return minimal response
                    val responseData = ClearActiveContextResponseData(
                        clearedAt = Clock.System.now().toString(),
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.ClearActiveContextResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error clearing active context", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    // Aspect command handlers

    private suspend fun handleCreateAspectDefinitionCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<CreateAspectDefinitionCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode CreateAspectDefinition command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        logger.info("Creating aspect definition", mapOf("key" to commandData.key, "type" to commandData.type) as Map<String, Any>)

        // Note: Aspect definition commands are not yet implemented in the scope-management context
        // For now, return a success response to maintain API compatibility
        return createSuccessResponse(
            request = request,
            kind = "scopes.responses.CreateAspectDefinitionResult",
            payload = json.encodeToString(commandData),
        )
    }

    private suspend fun handleUpdateAspectDefinitionCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<UpdateAspectDefinitionCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode UpdateAspectDefinition command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        logger.info("Updating aspect definition", mapOf("key" to commandData.key) as Map<String, Any>)

        // Note: Aspect definition commands are not yet implemented in the scope-management context
        // For now, return a success response to maintain API compatibility
        return createSuccessResponse(
            request = request,
            kind = "scopes.responses.UpdateAspectDefinitionResult",
            payload = json.encodeToString(commandData),
        )
    }

    private suspend fun handleDeleteAspectDefinitionCommand(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val commandData = try {
            json.decodeFromString<DeleteAspectDefinitionCommandData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode DeleteAspectDefinition command", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        logger.info("Deleting aspect definition", mapOf("key" to commandData.key) as Map<String, Any>)

        // Note: Aspect definition commands are not yet implemented in the scope-management context
        // For now, return a success response to maintain API compatibility
        return createSuccessResponse(
            request = request,
            kind = "scopes.responses.DeleteAspectDefinitionResult",
            payload = json.encodeToString(commandData),
        )
    }

    private suspend fun handleGetScopeQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val queryData = try {
            json.decodeFromString<GetScopeQueryData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode GetScope query", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract query
        val query = io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetScopeQuery(
            id = queryData.id,
        )

        // Execute query via port
        return try {
            if (scopeManagementQueryPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management query service unavailable"))
            }

            val result = scopeManagementQueryPort.getScope(query)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { scopeResult ->
                    if (scopeResult == null) {
                        throw StatusException(Status.NOT_FOUND.withDescription("Scope not found: ${queryData.id}"))
                    }

                    // Encode result
                    val responseData = GetScopeResponseData(
                        id = scopeResult.id,
                        title = scopeResult.title,
                        description = scopeResult.description,
                        parentId = scopeResult.parentId,
                        canonicalAlias = scopeResult.canonicalAlias,
                        createdAt = scopeResult.createdAt.toString(),
                        updatedAt = scopeResult.updatedAt.toString(),
                        isArchived = scopeResult.isArchived,
                        aspects = scopeResult.aspects,
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.GetScopeResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error getting scope", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleGetRootScopesQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val queryData = try {
            json.decodeFromString<GetRootScopesQueryData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode GetRootScopes query", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract query
        val query = io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetRootScopesQuery(
            offset = queryData.offset,
            limit = queryData.limit,
        )

        // Execute query via port
        return try {
            if (scopeManagementQueryPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management query service unavailable"))
            }

            val result = scopeManagementQueryPort.getRootScopes(query)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { scopeListResult ->
                    // Encode result
                    val responseData = GetRootScopesResponseData(
                        scopes = scopeListResult.scopes.map { scope ->
                            ScopeResponseData(
                                id = scope.id,
                                title = scope.title,
                                description = scope.description,
                                parentId = scope.parentId,
                                canonicalAlias = scope.canonicalAlias,
                                createdAt = scope.createdAt.toString(),
                                updatedAt = scope.updatedAt.toString(),
                                isArchived = scope.isArchived,
                                aspects = scope.aspects,
                            )
                        },
                        totalCount = scopeListResult.totalCount,
                        hasMore = (scopeListResult.offset + scopeListResult.limit) < scopeListResult.totalCount,
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.GetRootScopesResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error getting root scopes", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleGetChildrenQuery(request: Envelope): Envelope {
        // Decode request payload
        val req = try {
            json.decodeFromString<GetChildrenQueryData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode GetChildren query", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid GetChildren payload: ${e.message}"))
        }

        // Map to contract query
        val query = io.github.kamiazya.scopes.contracts.scopemanagement.queries.GetChildrenQuery(
            parentId = req.parentId,
            offset = req.offset,
            limit = req.limit,
        )

        // Execute query via port
        val port = scopeManagementQueryPort
            ?: throw StatusException(Status.UNAVAILABLE.withDescription("Scope management query service unavailable"))

        val result = port.getChildren(query)

        return result.fold(
            { error ->
                // Map contract error to structured google.rpc details
                throw ErrorDetailsBuilder.createStatusException(error)
            },
            { listResult ->
                val scopes = listResult.scopes.map { scope ->
                    ScopeResponseData(
                        id = scope.id,
                        title = scope.title,
                        description = scope.description,
                        parentId = scope.parentId,
                        canonicalAlias = scope.canonicalAlias,
                        createdAt = scope.createdAt.toString(),
                        updatedAt = scope.updatedAt.toString(),
                        isArchived = scope.isArchived,
                        aspects = scope.aspects,
                    )
                }

                val responseData = GetChildrenResponseData(
                    scopes = scopes,
                    totalCount = listResult.totalCount,
                    hasMore = (listResult.offset + scopes.size) < listResult.totalCount,
                )

                val payloadJson = json.encodeToString(GetChildrenResponseData.serializer(), responseData)
                createSuccessResponse(
                    request = request,
                    kind = "scopes.responses.GetChildrenResult",
                    payload = payloadJson,
                )
            },
        )
    }

    private suspend fun handleListAliasesQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val queryData = try {
            json.decodeFromString<ListAliasesQueryData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode ListAliases query", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract query
        val query = ListAliasesQuery(
            scopeId = queryData.scopeId,
        )

        // Execute query via port
        return try {
            if (scopeManagementQueryPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Scope management query service unavailable"))
            }

            val result = scopeManagementQueryPort.listAliases(query)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { aliasListResult ->
                    // Encode result
                    val responseData = ListAliasesResponseData(
                        scopeId = aliasListResult.scopeId,
                        aliases = aliasListResult.aliases.map { aliasResult ->
                            AliasInfo(
                                name = aliasResult.aliasName,
                                isCanonical = aliasResult.isCanonical,
                            )
                        },
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.ListAliasesResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error listing aliases", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleListContextViewsQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload (empty in this case)
        // For ListContextViewsQuery, there's no payload to decode since it's an object

        // Map to contract query
        val query = ListContextViewsQuery

        // Execute query via port
        return try {
            if (contextViewQueryPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view query service unavailable"))
            }

            val result = contextViewQueryPort.listContextViews(query)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { contextViews ->
                    // Encode result
                    val responseData = ListContextViewsResponseData(
                        contextViews = contextViews.map { contextView ->
                            ContextViewResponseData(
                                key = contextView.key,
                                name = contextView.name,
                                filter = contextView.filter,
                                description = contextView.description,
                                createdAt = contextView.createdAt.toString(),
                                updatedAt = contextView.updatedAt.toString(),
                            )
                        },
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.ListContextViewsResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error listing context views", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleGetContextViewQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val queryData = try {
            json.decodeFromString<GetContextViewQueryData>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode GetContextView query", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        // Map to contract query
        val query = GetContextViewQuery(
            key = queryData.key,
        )

        // Execute query via port
        return try {
            if (contextViewQueryPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view query service unavailable"))
            }

            val result = contextViewQueryPort.getContextView(query)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { contextViewResult ->
                    if (contextViewResult == null) {
                        throw StatusException(Status.NOT_FOUND.withDescription("Context view not found: ${queryData.key}"))
                    }

                    // Encode result
                    val responseData = GetContextViewResponseData(
                        key = contextViewResult.key,
                        name = contextViewResult.name,
                        filter = contextViewResult.filter,
                        description = contextViewResult.description,
                        createdAt = contextViewResult.createdAt.toString(),
                        updatedAt = contextViewResult.updatedAt.toString(),
                    )

                    createSuccessResponse(
                        request = request,
                        kind = "scopes.responses.GetContextViewResult",
                        payload = json.encodeToString(responseData),
                    )
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error getting context view", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    private suspend fun handleGetActiveContextQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload (empty in this case)
        // For GetActiveContextQuery, there's no payload to decode since it's an object

        // Map to contract query
        val query = GetActiveContextQuery

        // Execute query via port
        return try {
            if (contextViewQueryPort == null) {
                throw StatusException(Status.UNAVAILABLE.withDescription("Context view query service unavailable"))
            }

            val result = contextViewQueryPort.getActiveContext(query)

            result.fold(
                { error ->
                    // Use enhanced error details for better client experience
                    throw ErrorDetailsBuilder.createStatusException(error)
                },
                { contextViewResult ->
                    if (contextViewResult == null) {
                        // No active context
                        val responseData = GetActiveContextResponseData(
                            activeContext = null,
                        )

                        createSuccessResponse(
                            request = request,
                            kind = "scopes.responses.GetActiveContextResult",
                            payload = json.encodeToString(responseData),
                        )
                    } else {
                        // Encode result
                        val responseData = GetActiveContextResponseData(
                            activeContext = ContextViewResponseData(
                                key = contextViewResult.key,
                                name = contextViewResult.name,
                                filter = contextViewResult.filter,
                                description = contextViewResult.description,
                                createdAt = contextViewResult.createdAt.toString(),
                                updatedAt = contextViewResult.updatedAt.toString(),
                            ),
                        )

                        createSuccessResponse(
                            request = request,
                            kind = "scopes.responses.GetActiveContextResult",
                            payload = json.encodeToString(responseData),
                        )
                    }
                },
            )
        } catch (e: StatusException) {
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error getting active context", emptyMap(), e)
            throw StatusException(Status.INTERNAL.withDescription("Unexpected error: ${e.message}"))
        }
    }

    // Aspect query handlers

    private suspend fun handleGetAspectDefinitionQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val queryData = try {
            json.decodeFromString<GetAspectDefinitionQuery>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode GetAspectDefinition query", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        logger.info("Getting aspect definition", mapOf("key" to queryData.key) as Map<String, Any>)

        // Note: Aspect definition queries are not yet implemented in the scope-management context
        // For now, return null to indicate not found
        return createSuccessResponse(
            request = request,
            kind = "scopes.responses.GetAspectDefinitionResult",
            payload = "null",
        )
    }

    private suspend fun handleListAspectDefinitionsQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // No payload to decode for ListAspectDefinitionsQuery
        logger.info("Listing aspect definitions")

        // Note: Aspect definition queries are not yet implemented in the scope-management context
        // For now, return an empty list
        return createSuccessResponse(
            request = request,
            kind = "scopes.responses.ListAspectDefinitionsResult",
            payload = "[]",
        )
    }

    private suspend fun handleValidateAspectValueQuery(request: Envelope): Envelope {
        // Validate version
        if (request.version != VERSION_V1) {
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Unsupported version: ${request.version}. Expected: $VERSION_V1"))
        }

        // Decode payload
        val queryData = try {
            json.decodeFromString<ValidateAspectValueQuery>(request.payload.toStringUtf8())
        } catch (e: Exception) {
            logger.error("Failed to decode ValidateAspectValue query", emptyMap(), e)
            throw StatusException(Status.INVALID_ARGUMENT.withDescription("Invalid payload format: ${e.message}"))
        }

        logger.info("Validating aspect value", mapOf("key" to queryData.key, "values" to queryData.values) as Map<String, Any>)

        // Note: Aspect definition queries are not yet implemented in the scope-management context
        // For now, return the values as-is (indicating they are valid)
        val jsonResponse = json.encodeToString(queryData.values)
        return createSuccessResponse(
            request = request,
            kind = "scopes.responses.ValidateAspectValueResult",
            payload = jsonResponse,
        )
    }

    private fun createSuccessResponse(request: Envelope, kind: String, payload: String): Envelope {
        val now = Clock.System.now()

        return Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind(kind)
            .setVersion(VERSION_V1)
            .setCorrelationId(request.correlationId)
            .setPayload(ByteString.copyFromUtf8(payload))
            .setTimestamp(
                Timestamp.newBuilder()
                    .setSeconds(now.epochSeconds)
                    .setNanos(now.nanosecondsOfSecond)
                    .build(),
            )
            .putAllHeaders(
                mapOf(
                    "response-to" to request.id,
                    "server-version" to "0.1.0",
                ),
            )
            .build()
    }

    private fun createStreamEvent(correlationId: String, kind: String, payload: String): Envelope {
        val now = Clock.System.now()

        return Envelope.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setKind(kind)
            .setVersion(VERSION_V1)
            .setCorrelationId(correlationId)
            .setPayload(ByteString.copyFromUtf8(payload))
            .setTimestamp(
                Timestamp.newBuilder()
                    .setSeconds(now.epochSeconds)
                    .setNanos(now.nanosecondsOfSecond)
                    .build(),
            )
            .putAllHeaders(
                mapOf(
                    "server-version" to "0.1.0",
                    "event-type" to "stream",
                ),
            )
            .build()
    }
}

/**
 * Data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class CreateScopeCommandData(val title: String, val description: String? = null, val parentId: String? = null, val customAlias: String? = null)

@kotlinx.serialization.Serializable
data class CreateScopeResponseData(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: String,
)

@kotlinx.serialization.Serializable
data class UpdateScopeCommandData(val id: String, val title: String? = null, val description: String? = null, val parentId: String? = null)

@kotlinx.serialization.Serializable
data class UpdateScopeResponseData(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: String,
    val updatedAt: String,
)

@kotlinx.serialization.Serializable
data class DeleteScopeCommandData(val id: String, val cascade: Boolean = false)

@kotlinx.serialization.Serializable
data class DeleteScopeResponseData(val deletedScopeId: String, val deletedChildrenCount: Int)

/**
 * Query data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class GetScopeQueryData(val id: String)

@kotlinx.serialization.Serializable
data class GetScopeResponseData(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: String,
    val updatedAt: String,
    val isArchived: Boolean = false,
    val aspects: Map<String, List<String>> = emptyMap(),
)

@kotlinx.serialization.Serializable
data class GetRootScopesQueryData(val offset: Int = 0, val limit: Int = 50)

@kotlinx.serialization.Serializable
data class ScopeResponseData(
    val id: String,
    val title: String,
    val description: String?,
    val parentId: String?,
    val canonicalAlias: String,
    val createdAt: String,
    val updatedAt: String,
    val isArchived: Boolean = false,
    val aspects: Map<String, List<String>> = emptyMap(),
)

@kotlinx.serialization.Serializable
data class GetRootScopesResponseData(val scopes: List<ScopeResponseData>, val totalCount: Int, val hasMore: Boolean)

@kotlinx.serialization.Serializable
data class GetChildrenQueryData(val parentId: String, val includeDescendants: Boolean = false, val offset: Int = 0, val limit: Int = 50)

@kotlinx.serialization.Serializable
data class GetChildrenResponseData(val scopes: List<ScopeResponseData>, val totalCount: Int, val hasMore: Boolean)

/**
 * Alias command data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class AddAliasCommandData(val scopeId: String, val aliasName: String)

@kotlinx.serialization.Serializable
data class AddAliasResponseData(val scopeId: String, val aliasName: String)

@kotlinx.serialization.Serializable
data class RemoveAliasCommandData(val scopeId: String, val aliasName: String)

@kotlinx.serialization.Serializable
data class RemoveAliasResponseData(val scopeId: String, val aliasName: String)

@kotlinx.serialization.Serializable
data class SetCanonicalAliasCommandData(val scopeId: String, val aliasName: String)

@kotlinx.serialization.Serializable
data class SetCanonicalAliasResponseData(val scopeId: String, val aliasName: String)

/**
 * Context command data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class CreateContextViewCommandData(val key: String, val name: String, val filter: String, val description: String? = null)

@kotlinx.serialization.Serializable
data class CreateContextViewResponseData(val key: String, val name: String, val filter: String, val description: String?, val createdAt: String)

@kotlinx.serialization.Serializable
data class UpdateContextViewCommandData(val key: String, val name: String? = null, val filter: String? = null, val description: String? = null)

@kotlinx.serialization.Serializable
data class UpdateContextViewResponseData(val key: String, val name: String, val filter: String, val description: String?, val updatedAt: String)

@kotlinx.serialization.Serializable
data class DeleteContextViewCommandData(val key: String)

@kotlinx.serialization.Serializable
data class DeleteContextViewResponseData(val deletedKey: String)

@kotlinx.serialization.Serializable
data class SetActiveContextCommandData(val key: String)

@kotlinx.serialization.Serializable
data class SetActiveContextResponseData(val key: String, val name: String, val filter: String, val description: String?, val activatedAt: String)

@kotlinx.serialization.Serializable
data class ClearActiveContextResponseData(val clearedAt: String)

/**
 * Context query data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class GetContextViewQueryData(val key: String)

@kotlinx.serialization.Serializable
data class GetContextViewResponseData(
    val key: String,
    val name: String,
    val filter: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
)

@kotlinx.serialization.Serializable
data class ContextViewResponseData(
    val key: String,
    val name: String,
    val filter: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
)

@kotlinx.serialization.Serializable
data class ListContextViewsResponseData(val contextViews: List<ContextViewResponseData>)

@kotlinx.serialization.Serializable
data class GetActiveContextResponseData(val activeContext: ContextViewResponseData?)

/**
 * Aspect command data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class CreateAspectDefinitionCommandData(val key: String, val description: String, val type: String)

@kotlinx.serialization.Serializable
data class UpdateAspectDefinitionCommandData(val key: String, val description: String? = null)

@kotlinx.serialization.Serializable
data class DeleteAspectDefinitionCommandData(val key: String)

/**
 * Alias query data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class ListAliasesQueryData(val scopeId: String)

@kotlinx.serialization.Serializable
data class ListAliasesResponseData(val scopeId: String, val aliases: List<AliasInfo>)

@kotlinx.serialization.Serializable
data class AliasInfo(val name: String, val isCanonical: Boolean)

/**
 * Stream event data classes for JSON serialization/deserialization
 */
@kotlinx.serialization.Serializable
data class ConnectedEventData(val message: String, val timestamp: String)

@kotlinx.serialization.Serializable
data class ProgressUpdateEventData(
    val operationId: String,
    val percentage: Int,
    val message: String,
    val estimatedSecondsRemaining: Int,
    val metadata: Map<String, String> = emptyMap(),
)

@kotlinx.serialization.Serializable
data class OperationCompletedEventData(val operationId: String, val message: String, val timestamp: String)

@kotlinx.serialization.Serializable
data class ErrorEventData(val error: String, val timestamp: String)
