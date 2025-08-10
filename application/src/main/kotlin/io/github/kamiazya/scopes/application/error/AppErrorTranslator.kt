package io.github.kamiazya.scopes.application.error

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.error.RepositoryError

/**
 * Translates application errors into user-friendly messages.
 * This interface allows for different translation strategies (e.g., localization).
 */
interface AppErrorTranslator {
    
    /**
     * Translate an ApplicationError into a user-friendly message.
     * 
     * @param error The application error to translate
     * @return A user-friendly error message
     */
    fun translate(error: ApplicationError): String
    
    /**
     * Translate multiple ApplicationErrors into a combined message.
     * 
     * @param errors List of application errors to translate
     * @return A combined user-friendly error message
     */
    fun translateMultiple(errors: List<ApplicationError>): String
}

/**
 * Default implementation of AppErrorTranslator with English messages.
 * Production applications should consider implementing localized versions.
 */
class DefaultAppErrorTranslator : AppErrorTranslator {
    
    override fun translate(error: ApplicationError): String = when (error) {
        is ApplicationError.DomainErrors -> {
            val messages = error.errors.map { translateDomainError(it) }
            if (messages.size == 1) {
                messages.first()
            } else {
                "Multiple validation errors occurred: ${messages.joinToString("; ")}"
            }
        }
        
        is ApplicationError.Repository -> translateRepositoryError(error.cause)
        
        is ApplicationError.UseCaseError.InvalidRequest -> 
            "Invalid request: ${error.message}"
            
        is ApplicationError.UseCaseError.AuthorizationFailed -> 
            "Access denied for operation '${error.operation}': ${error.reason}"
            
        is ApplicationError.UseCaseError.ConcurrencyConflict -> 
            "The item (${error.entityId}) was modified by another user. ${error.message}"
            
        is ApplicationError.IntegrationError.ServiceUnavailable -> 
            "The service '${error.serviceName}' is currently unavailable. Please try again later."
            
        is ApplicationError.IntegrationError.ServiceTimeout -> 
            "The service '${error.serviceName}' took too long to respond (${error.timeoutMs}ms)."
            
        is ApplicationError.IntegrationError.InvalidResponse -> 
            "The service '${error.serviceName}' returned an invalid response: ${error.message}"
    }
    
    override fun translateMultiple(errors: List<ApplicationError>): String {
        return when (errors.size) {
            0 -> "No errors occurred"
            1 -> translate(errors.first())
            else -> {
                val messages = errors.map { translate(it) }
                "Multiple errors occurred: ${messages.joinToString("; ")}"
            }
        }
    }
    
    private fun translateDomainError(error: DomainError): String = when (error) {
        is DomainError.ScopeError -> translateScopeError(error)
        is DomainError.ScopeValidationError -> translateValidationError(error)
        is DomainError.ScopeBusinessRuleViolation -> translateBusinessRuleViolation(error)
        is DomainError.InfrastructureError -> translateRepositoryError(error.repositoryError)
    }
    
    private fun translateScopeError(error: DomainError.ScopeError): String = when (error) {
        is DomainError.ScopeError.ScopeNotFound -> "The scope was not found"
        is DomainError.ScopeError.InvalidTitle -> "Invalid scope title: ${error.reason}"
        is DomainError.ScopeError.InvalidDescription -> "Invalid scope description: ${error.reason}"
        is DomainError.ScopeError.InvalidParent -> "Invalid parent scope '${error.parentId}': ${error.reason}"
        is DomainError.ScopeError.CircularReference -> 
            "Cannot set scope '${error.parentId}' as parent of '${error.scopeId}' - would create circular reference"
        is DomainError.ScopeError.SelfParenting -> "A scope cannot be its own parent"
    }
    
    private fun translateValidationError(error: DomainError.ScopeValidationError): String = when (error) {
        is DomainError.ScopeValidationError.EmptyScopeTitle -> "Scope title cannot be empty"
        is DomainError.ScopeValidationError.ScopeTitleTooShort -> "Scope title is too short"
        is DomainError.ScopeValidationError.ScopeTitleTooLong -> 
            "Scope title is too long (max: ${error.maxLength}, actual: ${error.actualLength})"
        is DomainError.ScopeValidationError.ScopeTitleContainsNewline -> "Scope title cannot contain newlines"
        is DomainError.ScopeValidationError.ScopeDescriptionTooLong -> 
            "Scope description is too long (max: ${error.maxLength}, actual: ${error.actualLength})"
        is DomainError.ScopeValidationError.ScopeInvalidFormat -> 
            "Invalid format for ${error.field}: expected ${error.expected}"
    }
    
    private fun translateBusinessRuleViolation(error: DomainError.ScopeBusinessRuleViolation): String = when (error) {
        is DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded -> 
            "Maximum scope depth exceeded (max: ${error.maxDepth}, actual: ${error.actualDepth})"
        is DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded -> 
            "Maximum children exceeded (max: ${error.maxChildren}, actual: ${error.actualChildren})"
        is DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle -> {
            val parentInfo = error.parentId?.let { " under parent '$it'" } ?: ""
            "A scope with title '${error.title}' already exists$parentInfo"
        }
    }
    
    private fun translateRepositoryError(error: RepositoryError): String = when (error) {
        is RepositoryError.ConnectionError -> 
            "Database connection failed. Please check your connection and try again."
            
        is RepositoryError.DatabaseError -> 
            "Database error: ${error.message}. Please contact support if this persists."
            
        is RepositoryError.DataIntegrityError -> 
            "Data integrity error: ${error.message}. Please check your input."
            
        is RepositoryError.NotFound -> 
            "The scope with ID '${error.id}' was not found."
            
        is RepositoryError.ConflictError -> 
            "Conflict error for scope '${error.id}': ${error.message}"
            
        is RepositoryError.SerializationError -> 
            "Data serialization error: ${error.message}"
            
        is RepositoryError.UnknownError -> 
            "Unknown error: ${error.message}"
    }
}
