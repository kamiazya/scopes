package io.github.kamiazya.scopes.domain.error

/**
 * Uniqueness validation specific errors with detailed context.
 */
sealed class UniquenessValidationError : ScopeValidationServiceError() {
    
    /**
     * Represents a duplicate title error.
     * 
     * @param title The original title that conflicts
     * @param normalizedTitle The normalized version used for comparison
     * @param parentId The parent scope ID where duplication occurred
     * @param existingScopeId The ID of the existing scope with the same title
     */
    data class DuplicateTitle(
        val title: String,
        val normalizedTitle: String,
        val parentId: String?,
        val existingScopeId: String
    ) : UniquenessValidationError()
}