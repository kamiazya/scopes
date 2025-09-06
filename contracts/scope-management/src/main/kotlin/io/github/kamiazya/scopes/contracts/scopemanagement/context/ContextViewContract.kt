package io.github.kamiazya.scopes.contracts.scopemanagement.context

import kotlinx.datetime.Instant

/**
 * Contract definitions for context view operations.
 * These provide stable API contracts between bounded contexts.
 */
public interface ContextViewContract {
    // Request/Response types for Create operation
    public sealed interface CreateContextViewResponse {
        public data class Success(val contextView: ContextView) : CreateContextViewResponse
        public data class DuplicateKey(val key: String) : CreateContextViewResponse
        public data class InvalidFilter(val filter: String, val reason: String) : CreateContextViewResponse
    }

    // Request/Response types for List operation
    public sealed interface ListContextViewsResponse {
        public data class Success(val contextViews: List<ContextView>) : ListContextViewsResponse
    }

    // Request/Response types for Get operation
    public sealed interface GetContextViewResponse {
        public data class Success(val contextView: ContextView) : GetContextViewResponse
        public data class NotFound(val key: String) : GetContextViewResponse
    }

    // Request/Response types for Update operation
    public sealed interface UpdateContextViewResponse {
        public data class Success(val contextView: ContextView) : UpdateContextViewResponse
        public data class NotFound(val key: String) : UpdateContextViewResponse
        public data class InvalidFilter(val filter: String, val reason: String) : UpdateContextViewResponse
    }

    // Request/Response types for Delete operation
    public sealed interface DeleteContextViewResponse {
        public object Success : DeleteContextViewResponse
        public data class NotFound(val key: String) : DeleteContextViewResponse
    }

    // Request/Response types for Active Context operations
    public sealed interface GetActiveContextResponse {
        public data class Success(val contextView: ContextView?) : GetActiveContextResponse
    }

    public sealed interface SetActiveContextResponse {
        public object Success : SetActiveContextResponse
        public data class NotFound(val key: String) : SetActiveContextResponse
    }
}

/**
 * Data Transfer Object for ContextView across boundaries.
 */
public data class ContextView(val key: String, val name: String, val filter: String, val description: String?, val createdAt: Instant, val updatedAt: Instant)

/**
 * Command types for context view write operations.
 */
public data class CreateContextViewCommand(val key: String, val name: String, val filter: String, val description: String?)

public data class UpdateContextViewCommand(val key: String, val name: String?, val filter: String?, val description: String?)

public data class DeleteContextViewCommand(val key: String)

public data class SetActiveContextCommand(val key: String)

/**
 * Query types for context view read operations.
 */
public object ListContextViewsQuery

public data class GetContextViewQuery(val key: String)

public object GetActiveContextQuery
