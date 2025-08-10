package io.github.kamiazya.scopes.presentation.cli.commands

import arrow.core.fold
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.application.dto.CreateScopeResult
import io.github.kamiazya.scopes.application.usecase.command.CreateScope
import io.github.kamiazya.scopes.application.usecase.error.CreateScopeError
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import kotlinx.coroutines.runBlocking

/**
 * CLI command for creating scopes using UseCase pattern.
 * Demonstrates clean separation between presentation and application layers.
 */
class CreateScopeCommand(
    private val createScopeHandler: CreateScopeHandler,
) : CliktCommand(name = "create") {
    
    private val name by option("--name", help = "Scope name").required()
    private val description by option("--description", help = "Scope description")
    private val parent by option("--parent", help = "Parent scope ID")

    override fun run() = runBlocking {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            echo("❌ Error: Scope name cannot be empty or contain only whitespace", err = true)
            throw com.github.ajalt.clikt.core.ProgramResult(1)
        }
        
        val command = CreateScope(
            title = trimmedName,
            description = description,
            parentId = parent,
        )

        createScopeHandler(command).fold(
            ifLeft = { error: CreateScopeError ->
                val errorMessage = when (error) {
                    is CreateScopeError.ParentNotFound -> "Parent scope not found"
                    is CreateScopeError.ValidationFailed -> "Validation failed: ${error.reason} (field: ${error.field})"
                    is CreateScopeError.DomainRuleViolation -> "Domain rule violated: ${error.domainError}"
                    is CreateScopeError.SaveFailure -> "Save operation failed: ${error.saveError}"
                    is CreateScopeError.ExistenceCheckFailure -> "Existence check failed: ${error.existsError}"
                    is CreateScopeError.CountFailure -> "Count operation failed: ${error.countError}"
                    is CreateScopeError.HierarchyTraversalFailure -> "Hierarchy traversal failed: ${error.findError}"
                    is CreateScopeError.HierarchyDepthExceeded -> "Maximum hierarchy depth (${error.maxDepth}) exceeded"
                    is CreateScopeError.MaxChildrenExceeded -> "Parent ${error.parentId} already has maximum children (${error.maxChildren})"
                    
                    // New service-specific error types with improved user messages
                    is CreateScopeError.TitleValidationFailed -> formatTitleValidationError(error.titleError)
                    is CreateScopeError.BusinessRuleViolationFailed -> formatBusinessRuleError(error.businessRuleError)
                    is CreateScopeError.DuplicateTitleFailed -> formatDuplicateTitleError(error.uniquenessError)
                }
                echo("❌ Error creating scope: $errorMessage", err = true)
                throw com.github.ajalt.clikt.core.ProgramResult(1)
            },
            ifRight = { result: CreateScopeResult ->
                echo("✅ Created scope: ${result.id}")
                echo("   Title: ${result.title}")
                if (result.description != null) {
                    echo("   Description: ${result.description}")
                }
                if (result.parentId != null) {
                    echo("   Parent: ${result.parentId}")
                }
                echo("   Created at: ${result.createdAt}")
            }
        )
    }

    // ===== ERROR FORMATTING METHODS FOR USER-FRIENDLY MESSAGES =====

    /**
     * Formats title validation errors with user-friendly messages.
     */
    private fun formatTitleValidationError(error: io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.TitleValidationError): String {
        return when (error) {
            is io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.TitleValidationError.EmptyTitle ->
                "Title cannot be empty"
            is io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.TitleValidationError.TooShort ->
                "Title is too short (minimum ${error.minLength} characters, got ${error.actualLength})"
            is io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.TitleValidationError.TooLong ->
                "Title is too long (maximum ${error.maxLength} characters, got ${error.actualLength})"
            is io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.TitleValidationError.InvalidCharacters ->
                "Title contains invalid characters: ${error.invalidChars.joinToString(", ")}"
        }
    }

    /**
     * Formats business rule errors with user-friendly messages.
     */
    private fun formatBusinessRuleError(error: io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError): String {
        return when (error) {
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.ScopeBusinessRuleError.MaxDepthExceeded ->
                "Cannot create scope: maximum hierarchy depth of ${error.maxDepth} would be exceeded (attempted depth: ${error.attemptedDepth})"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.ScopeBusinessRuleError.MaxChildrenExceeded ->
                "Cannot create scope: parent already has the maximum number of children (${error.maxChildren})"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.ScopeBusinessRuleError.DuplicateTitleNotAllowed ->
                "Cannot create scope: duplicate title '${error.title}' not allowed in this context (${error.conflictContext})"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.ScopeBusinessRuleError.CheckFailed ->
                "Cannot create scope: validation check '${error.checkName}' failed - ${error.errorDetails}"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.HierarchyBusinessRuleError.SelfParentingNotAllowed ->
                "Cannot create scope: self-parenting is not allowed"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.HierarchyBusinessRuleError.CircularReferenceNotAllowed ->
                "Cannot create scope: circular reference detected in hierarchy"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.HierarchyBusinessRuleError.OrphanedScopeCreationNotAllowed ->
                "Cannot create scope: ${error.reason}"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.DataIntegrityBusinessRuleError.ConsistencyCheckFailed ->
                "Cannot create scope: data consistency check failed (${error.failedChecks.joinToString(", ")})"
            is io.github.kamiazya.scopes.domain.error.BusinessRuleServiceError.DataIntegrityBusinessRuleError.ReferentialIntegrityViolation ->
                "Cannot create scope: referential integrity violation in ${error.referenceType}"
        }
    }

    /**
     * Formats duplicate title errors with user-friendly messages.
     */
    private fun formatDuplicateTitleError(error: io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.UniquenessValidationError): String {
        return when (error) {
            is io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.UniquenessValidationError.DuplicateTitle -> {
                val context = if (error.parentId != null) "under the same parent" else "at the root level"
                "Title '${error.title}' already exists $context"
            }
            is io.github.kamiazya.scopes.domain.error.ScopeValidationServiceError.UniquenessValidationError.CheckFailed ->
                "Cannot validate title uniqueness: check '${error.checkName}' failed - ${error.errorDetails}"
        }
    }
}
