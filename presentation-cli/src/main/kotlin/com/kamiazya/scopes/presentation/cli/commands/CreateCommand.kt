package com.kamiazya.scopes.presentation.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.kamiazya.scopes.application.error.ApplicationError
import com.kamiazya.scopes.application.service.ScopeService
import com.kamiazya.scopes.application.usecase.CreateScopeRequest
import com.kamiazya.scopes.domain.entity.ScopeId
import kotlinx.coroutines.runBlocking

/**
 * CLI command for creating new scopes.
 * Handles Result types with proper error presentation.
 */
class CreateCommand(
    private val scopeService: ScopeService,
) : CliktCommand(name = "create") {
    private val title by option("-t", "--title", help = "Scope title").required()
    private val description by option("-d", "--description", help = "Scope description")
    private val parentId by option("--parent", help = "Parent scope ID")

    override fun run() =
        runBlocking {
            val request =
                CreateScopeRequest(
                    title = title,
                    description = description,
                    parentId = parentId?.let { ScopeId.from(it) },
                )

            scopeService.createScope(request).fold(
                ifLeft = { error ->
                    echo("❌ Error creating scope: ${formatError(error)}", err = true)
                },
                ifRight = { response ->
                    echo("✅ Created scope: ${response.scope.id}")
                    echo("   Title: ${response.scope.title}")
                    if (response.scope.description != null) {
                        echo("   Description: ${response.scope.description}")
                    }
                    if (response.scope.parentId != null) {
                        echo("   Parent: ${response.scope.parentId}")
                    }
                }
            )
        }

    private fun formatError(error: ApplicationError): String =
        when (error) {
            is ApplicationError.DomainError -> "Domain error: ${error.domainError}"
            is ApplicationError.RepositoryError -> "Repository error: ${error.repositoryError}"
            is ApplicationError.UseCaseError.InvalidRequest ->
                "Invalid request: ${error.message}"
            is ApplicationError.UseCaseError.AuthorizationFailed ->
                "Authorization failed: ${error.reason}"
            is ApplicationError.UseCaseError.ConcurrencyConflict ->
                "Concurrency conflict: ${error.message}"
            is ApplicationError.IntegrationError.ServiceUnavailable ->
                "Service unavailable: ${error.serviceName}"
            is ApplicationError.IntegrationError.ServiceTimeout ->
                "Service timeout: ${error.serviceName} (${error.timeoutMs}ms)"
            is ApplicationError.IntegrationError.InvalidResponse ->
                "Invalid response: ${error.serviceName} - ${error.message}"
        }
}

