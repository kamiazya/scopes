package com.kamiazya.scopes.presentation.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.kamiazya.scopes.application.error.ApplicationError
import com.kamiazya.scopes.application.service.ScopeService
import com.kamiazya.scopes.domain.entity.ScopeId
import kotlinx.coroutines.runBlocking

/**
 * CLI command for listing scopes.
 * Handles Result types with proper error presentation.
 */
class ListCommand(
    private val scopeService: ScopeService,
) : CliktCommand(name = "list") {
    private val parentId by option("--parent", help = "Show children of this parent scope ID")

    override fun run() =
        runBlocking {
            val result =
                if (parentId != null) {
                    scopeService.getChildScopes(ScopeId.from(parentId!!))
                } else {
                    scopeService.getAllScopes()
                }

            result.fold(
                ifLeft = { error ->
                    echo("❌ Error listing scopes: ${formatError(error)}", err = true)
                },
                ifRight = { scopes ->
                    if (scopes.isEmpty()) {
                        echo("No scopes found.")
                        return@runBlocking
                    }

                    echo("📋 Scopes:")
                    scopes.forEach { scope ->
                        echo("  ${scope.id} - ${scope.title}")
                        if (scope.description != null) {
                            echo("    Description: ${scope.description}")
                        }
                        if (scope.parentId != null) {
                            echo("    Parent: ${scope.parentId}")
                        }
                        echo()
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

