package io.github.kamiazya.scopes.presentation.cli

import com.github.ajalt.clikt.testing.test
import io.github.kamiazya.scopes.application.service.CrossAggregateValidationService
import io.github.kamiazya.scopes.application.usecase.handler.CreateScopeHandler
import io.github.kamiazya.scopes.domain.service.ScopeAliasManagementService
import io.github.kamiazya.scopes.domain.service.ScopeHierarchyService
import io.github.kamiazya.scopes.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeAliasRepository
import io.github.kamiazya.scopes.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.infrastructure.transaction.NoopTransactionManager
import io.github.kamiazya.scopes.presentation.cli.commands.CreateScopeCommand
import io.github.kamiazya.scopes.presentation.cli.error.CliErrorMessageFormatter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * End-to-End demonstration test for CreateScopeCommand.
 *
 * This test showcases the complete DDD UseCase pattern working across all layers:
 * - CLI → Application → Domain → Infrastructure
 *
 * Serves as both regression testing and live documentation of the E2E slice.
 */
class CreateScopeE2EDemoTest : FunSpec({

    test("E2E Demo: Create scope shows complete vertical slice") {
        // Arrange: Set up the complete dependency chain
        val repository = InMemoryScopeRepository()
        val aliasRepository = InMemoryScopeAliasRepository()
        val transactionManager = NoopTransactionManager()
        val hierarchyService = ScopeHierarchyService()
        val wordProvider = DefaultWordProvider()
        val haikunatorStrategy = HaikunatorStrategy()
        val aliasGenerationService = DefaultAliasGenerationService(haikunatorStrategy, wordProvider)
        val aliasManagementService = ScopeAliasManagementService(aliasRepository, aliasGenerationService)
        val crossAggregateValidationService = CrossAggregateValidationService(repository)
        val handler = CreateScopeHandler(repository, transactionManager, hierarchyService, crossAggregateValidationService, aliasManagementService)
        val command = CreateScopeCommand(handler, CliErrorMessageFormatter)

        // Act: Execute CLI command (same as `./gradlew run --args="create --name Hello"`)
        val result = command.test("--name=Hello")

        // Assert: Verify the complete E2E behavior
        result.statusCode shouldBe 0
        result.output shouldContain "✅ Created scope:"
        result.output shouldContain "Title: Hello"
        result.output shouldContain "Created at:"
        // ULID format validation
        result.output shouldContain Regex("Created scope: [0-9A-Z]{26}")
        result.stderr shouldBe ""
    }

    test("E2E Demo: Basic functionality verification") {
        // Verification that the CLI-Application-Domain-Infrastructure flow works
        val repository = InMemoryScopeRepository()
        val aliasRepository = InMemoryScopeAliasRepository()
        val transactionManager = NoopTransactionManager()
        val hierarchyService = ScopeHierarchyService()
        val wordProvider = DefaultWordProvider()
        val haikunatorStrategy = HaikunatorStrategy()
        val aliasGenerationService = DefaultAliasGenerationService(haikunatorStrategy, wordProvider)
        val aliasManagementService = ScopeAliasManagementService(aliasRepository, aliasGenerationService)
        val crossAggregateValidationService = CrossAggregateValidationService(repository)
        val handler = CreateScopeHandler(repository, transactionManager, hierarchyService, crossAggregateValidationService, aliasManagementService)
        val command = CreateScopeCommand(handler, CliErrorMessageFormatter)

        // Demonstrate that E2E flow succeeds
        val result = command.test("--name=DemoProject")

        // This test serves as regression testing for the E2E pattern
        result.statusCode shouldBe 0
        result.output shouldContain "✅ Created scope:"
        result.output shouldContain "Title: DemoProject"
    }
})
