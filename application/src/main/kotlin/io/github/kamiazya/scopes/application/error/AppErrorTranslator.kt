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
        is DomainError.ValidationError -> error.message
        is DomainError.BusinessRuleViolation -> "Business rule violation: ${error.rule}"
        is DomainError.EntityNotFound -> "The ${error.entityType} with ID '${error.id}' was not found"
        is DomainError.DuplicateEntity -> "A ${error.entityType} with the same identifier already exists"
        is DomainError.InvalidEntityState -> "The ${error.entityType} is in an invalid state: ${error.reason}"
    }
    
    private fun translateRepositoryError(error: RepositoryError): String = when (error) {
        is RepositoryError.ConnectionFailed -> 
            "Database connection failed. Please check your connection and try again."
            
        is RepositoryError.QueryFailed -> 
            "Database query failed. Please contact support if this persists."
            
        is RepositoryError.TransactionFailed -> 
            "Transaction failed. Changes were not saved. Please try again."
            
        is RepositoryError.OptimisticLockingFailed -> 
            "The data was modified by another user. Please refresh and try again."
            
        is RepositoryError.ConstraintViolation -> 
            "Data constraint violation: ${error.constraint}. Please check your input."
    }
}