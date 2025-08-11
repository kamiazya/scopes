package io.github.kamiazya.scopes.application.service.error

/**
 * Template errors for message template processing failures.
 * These handle failures in template resolution and rendering.
 */
sealed class TemplateError : NotificationServiceError() {
    
    /**
     * Required message template was not found.
     */
    data class TemplateNotFound(
        val templateId: String,
        val locale: String?,
        val searchPaths: List<String>
    ) : TemplateError()
    
    /**
     * Template rendering failed due to missing or invalid variables.
     */
    data class TemplateRenderingFailure(
        val templateId: String,
        val missingVariables: List<String>,
        val invalidVariables: Map<String, String>,
        val cause: Throwable?
    ) : TemplateError()
    
    /**
     * Template syntax is invalid.
     */
    data class InvalidTemplateSyntax(
        val templateId: String,
        val syntaxError: String,
        val lineNumber: Int?,
        val columnNumber: Int?
    ) : TemplateError()
}