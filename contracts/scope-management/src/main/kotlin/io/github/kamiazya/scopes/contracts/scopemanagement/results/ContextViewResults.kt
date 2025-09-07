package io.github.kamiazya.scopes.contracts.scopemanagement.results

import io.github.kamiazya.scopes.contracts.scopemanagement.types.ContextView

/**
 * Request/Response types for Create operation
 */
public sealed interface CreateContextViewResult {
    public data class Success(val contextView: ContextView) : CreateContextViewResult
    public data class DuplicateKey(val key: String) : CreateContextViewResult
    public data class InvalidFilter(val filter: String, val reason: String) : CreateContextViewResult
}

/**
 * Request/Response types for List operation
 */
public sealed interface ListContextViewsResult {
    public data class Success(val contextViews: List<ContextView>) : ListContextViewsResult
}

/**
 * Request/Response types for Get operation
 */
public sealed interface GetContextViewResult {
    public data class Success(val contextView: ContextView) : GetContextViewResult
    public data class NotFound(val key: String) : GetContextViewResult
}

/**
 * Request/Response types for Update operation
 */
public sealed interface UpdateContextViewResult {
    public data class Success(val contextView: ContextView) : UpdateContextViewResult
    public data class NotFound(val key: String) : UpdateContextViewResult
    public data class InvalidFilter(val filter: String, val reason: String) : UpdateContextViewResult
}

/**
 * Request/Response types for Delete operation
 */
public sealed interface DeleteContextViewResult {
    public object Success : DeleteContextViewResult
    public data class NotFound(val key: String) : DeleteContextViewResult
}

/**
 * Request/Response types for Active Context operations
 */
public sealed interface GetActiveContextResult {
    public data class Success(val contextView: ContextView?) : GetActiveContextResult
}

public sealed interface SetActiveContextResult {
    public object Success : SetActiveContextResult
    public data class NotFound(val key: String) : SetActiveContextResult
}
