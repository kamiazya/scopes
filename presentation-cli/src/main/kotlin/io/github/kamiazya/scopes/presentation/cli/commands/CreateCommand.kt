package io.github.kamiazya.scopes.presentation.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.application.error.ApplicationError
import io.github.kamiazya.scopes.application.usecase.CreateScopeRequest
import io.github.kamiazya.scopes.application.usecase.CreateScopeUseCase
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.presentation.cli.utils.toUserMessage
import kotlinx.coroutines.runBlocking

/**
 * CLI command for creating new scopes.
 * Handles Result types with proper error presentation.
 */
class CreateCommand(
    private val createScopeUseCase: CreateScopeUseCase,
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

            createScopeUseCase.execute(request).fold(
                ifLeft = { error ->
                    echo("❌ Error creating scope: ${error.toUserMessage()}", err = true)
                    throw com.github.ajalt.clikt.core.ProgramResult(1)
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

}

