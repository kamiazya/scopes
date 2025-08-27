package io.github.kamiazya.scopes.scopemanagement.application.integration

import io.github.kamiazya.scopes.scopemanagement.application.handler.*
import io.github.kamiazya.scopes.scopemanagement.domain.service.AliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.domain.service.HierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.domain.service.TransactionManager
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.DefaultAliasGenerationService
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.providers.DefaultWordProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.alias.generation.strategies.HaikunatorStrategy
import io.github.kamiazya.scopes.scopemanagement.infrastructure.policy.DefaultHierarchyPolicyProvider
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeAliasRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.repository.InMemoryScopeRepository
import io.github.kamiazya.scopes.scopemanagement.infrastructure.transaction.NoopTransactionManager
import io.mockk.mockk
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.slf4j.Logger

/**
 * Integration test fixture providing complete wiring for application layer testing.
 * Uses in-memory implementations for all repositories and services.
 */
object IntegrationTestFixture {

    fun setupTestDependencies() {
        stopKoin() // Clean any existing Koin context

        startKoin {
            modules(createTestModule())
        }
    }

    fun tearDownTestDependencies() {
        stopKoin()
    }

    private fun createTestModule() = module {
        // Repositories - In-memory implementations
        single { InMemoryScopeRepository() } bind io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository::class
        single { InMemoryScopeAliasRepository() } bind io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository::class

        // Domain Services
        single<AliasGenerationService> {
            DefaultAliasGenerationService(
                strategy = HaikunatorStrategy(),
                wordProvider = DefaultWordProvider(),
            )
        }

        single<HierarchyPolicyProvider> {
            DefaultHierarchyPolicyProvider()
        }

        single<TransactionManager> {
            NoopTransactionManager()
        }

        // Application Handlers
        singleOf(::CreateScopeCommandHandler)
        singleOf(::UpdateScopeCommandHandler)
        singleOf(::DeleteScopeCommandHandler)
        singleOf(::GetScopeByIdQueryHandler)
        singleOf(::GetScopesQueryHandler)

        // Alias Handlers
        singleOf(::AddCustomAliasCommandHandler)
        singleOf(::RemoveAliasCommandHandler)
        singleOf(::GenerateCanonicalAliasCommandHandler)
        singleOf(::GetAliasesByScopeIdQueryHandler)
        singleOf(::GetScopeByAliasQueryHandler)
        singleOf(::SearchAliasesQueryHandler)

        // Mock logger
        single<Logger> { mockk(relaxed = true) }
    }

    /**
     * Create a test context with pre-configured handlers and repositories.
     */
    fun createTestContext(): IntegrationTestContext = IntegrationTestContext(
        scopeRepository = org.koin.core.component.KoinComponent().getKoin().get(),
        aliasRepository = org.koin.core.component.KoinComponent().getKoin().get(),
        createScopeHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        updateScopeHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        deleteScopeHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        getScopeByIdHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        getScopesHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        addCustomAliasHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        removeAliasHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        generateCanonicalAliasHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        getAliasesByScopeIdHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        getScopeByAliasHandler = org.koin.core.component.KoinComponent().getKoin().get(),
        searchAliasesHandler = org.koin.core.component.KoinComponent().getKoin().get(),
    )
}

/**
 * Test context providing access to all handlers and repositories for integration testing.
 */
data class IntegrationTestContext(
    val scopeRepository: io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeRepository,
    val aliasRepository: io.github.kamiazya.scopes.scopemanagement.domain.repository.ScopeAliasRepository,
    val createScopeHandler: CreateScopeCommandHandler,
    val updateScopeHandler: UpdateScopeCommandHandler,
    val deleteScopeHandler: DeleteScopeCommandHandler,
    val getScopeByIdHandler: GetScopeByIdQueryHandler,
    val getScopesHandler: GetScopesQueryHandler,
    val addCustomAliasHandler: AddCustomAliasCommandHandler,
    val removeAliasHandler: RemoveAliasCommandHandler,
    val generateCanonicalAliasHandler: GenerateCanonicalAliasCommandHandler,
    val getAliasesByScopeIdHandler: GetAliasesByScopeIdQueryHandler,
    val getScopeByAliasHandler: GetScopeByAliasQueryHandler,
    val searchAliasesHandler: SearchAliasesQueryHandler,
) : org.koin.core.component.KoinComponent
