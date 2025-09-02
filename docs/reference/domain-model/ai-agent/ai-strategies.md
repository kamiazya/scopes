# AI Strategy Pattern

This document describes the pluggable AI strategy pattern that enables entity-specific intelligence while maintaining a consistent interface across all entity types.

## Overview

The AI Strategy Pattern separates:
- **Generic Infrastructure**: Proposal management, review workflows, testing
- **Entity-Specific Intelligence**: Analysis logic, proposal generation, validation

This separation allows each entity type to have tailored AI capabilities without duplicating infrastructure code.

## Core Interface

### EntityAIStrategy

The fundamental contract for all AI strategies:

```kotlin
interface EntityAIStrategy {
    /**
     * Check if this strategy can handle the given entity type
     */
    fun canHandle(entityType: EntityType): Boolean
    
    /**
     * Analyze an entity and identify opportunities for improvement
     */
    suspend fun analyzeEntity(
        entity: EntitySnapshot,
        context: AIAnalysisContext
    ): Result<EntityAnalysis, AIError>
    
    /**
     * Generate proposals based on entity analysis
     */
    suspend fun generateProposals(
        entity: EntitySnapshot,
        analysis: EntityAnalysis,
        context: AIProposalContext
    ): Result<List<EntityChangeProposal>, AIError>
    
    /**
     * Validate a proposal against current entity state
     */
    suspend fun validateProposal(
        proposal: EntityChangeProposal,
        entity: EntitySnapshot
    ): Result<ValidationResult, AIError>
    
    /**
     * Learn from user feedback on proposals
     */
    suspend fun incorporateFeedback(
        proposal: EntityChangeProposal,
        feedback: ProposalFeedback
    ): Result<Unit, AIError>
}
```

## Analysis Context

### AIAnalysisContext

Rich context provided for better analysis:

```kotlin
data class AIAnalysisContext(
    val userGoals: List<String> = emptyList(),
    val recentActivity: List<RecentActivity> = emptyList(),
    val relatedEntities: Map<EntityId, EntitySnapshot> = emptyMap(),
    val userPreferences: UserPreferences,
    val timeContext: TimeContext,
    val workloadContext: WorkloadContext,
    val collaborationContext: CollaborationContext = CollaborationContext(),
    val modelPreferences: ModelPreferences? = null
)

data class TimeContext(
    val currentTime: Instant,
    val userTimezone: TimeZone,
    val upcomingDeadlines: List<Deadline>,
    val availableTimeSlots: List<TimeSlot>,
    val workingHours: WorkingHours
)

data class WorkloadContext(
    val currentWorkload: WorkloadLevel,
    val availableCapacity: Duration,
    val skillset: Set<Skill>,
    val preferences: WorkPreferences
)

data class CollaborationContext(
    val teamMembers: List<TeamMember> = emptyList(),
    val activeCollaborations: List<Collaboration> = emptyList(),
    val sharedGoals: List<String> = emptyList()
)
```

### Recent Activity

Helps AI understand user patterns:

```kotlin
data class RecentActivity(
    val entityId: EntityId,
    val entityType: EntityType,
    val activityType: ActivityType,
    val timestamp: Instant,
    val details: JsonObject
)

enum class ActivityType {
    CREATED,    // New entity created
    UPDATED,    // Entity modified
    COMPLETED,  // Task/scope completed
    DELETED,    // Entity removed
    VIEWED,     // Entity accessed
    COMMENTED,  // Comment added
    SHARED      // Entity shared
}
```

## Entity-Specific Implementations

### Scope AI Strategy

Handles task breakdown and project management:

```kotlin
class ScopeAIStrategy(
    private val aiClient: AIClient,
    private val scopeRepository: ScopeRepository,
    private val contextAnalyzer: ScopeContextAnalyzer
) : EntityAIStrategy {
    
    override fun canHandle(entityType: EntityType): Boolean = 
        entityType == EntityType.SCOPE
    
    override suspend fun analyzeEntity(
        entity: EntitySnapshot,
        context: AIAnalysisContext
    ): Result<EntityAnalysis, AIError> {
        val scope = deserializeScope(entity.data)
        val scopeContext = contextAnalyzer.analyzeScope(scope, context)
        
        return aiClient.analyze(
            prompt = buildAnalysisPrompt(scope, scopeContext),
            model = context.modelPreferences?.analysisModel ?: "gpt-4"
        ).map { response ->
            ScopeAnalysis(
                complexityScore = response.extractComplexityScore(),
                taskBreakdownOpportunities = response.extractBreakdownOpportunities(),
                hierarchyIssues = response.extractHierarchyIssues(),
                priorityRecommendations = response.extractPriorityRecommendations(),
                deadlineRisks = response.extractDeadlineRisks(),
                dependencyIssues = response.extractDependencyIssues(),
                resourceAllocationSuggestions = response.extractResourceSuggestions()
            )
        }
    }
    
    override suspend fun generateProposals(
        entity: EntitySnapshot,
        analysis: EntityAnalysis,
        context: AIProposalContext
    ): Result<List<EntityChangeProposal>, AIError> {
        val scopeAnalysis = analysis as ScopeAnalysis
        val proposals = mutableListOf<EntityChangeProposal>()
        
        // Task breakdown proposals
        if (scopeAnalysis.complexityScore > 0.7) {
            proposals.addAll(generateTaskBreakdownProposals(entity, scopeAnalysis, context))
        }
        
        // Priority optimization proposals
        if (scopeAnalysis.priorityRecommendations.isNotEmpty()) {
            proposals.addAll(generatePriorityProposals(entity, scopeAnalysis, context))
        }
        
        // Hierarchy reorganization proposals
        if (scopeAnalysis.hierarchyIssues.isNotEmpty()) {
            proposals.addAll(generateHierarchyProposals(entity, scopeAnalysis, context))
        }
        
        return Result.success(proposals.take(context.maxProposals))
    }
    
    private suspend fun generateTaskBreakdownProposals(
        entity: EntitySnapshot,
        analysis: ScopeAnalysis,
        context: AIProposalContext
    ): List<EntityChangeProposal> {
        return analysis.taskBreakdownOpportunities.map { opportunity ->
            val subTasks = generateSubTasks(opportunity, context)
            
            EntityChangeProposal(
                id = ProposalId.generate(),
                entityType = EntityType.SCOPE,
                entityId = entity.entityId,
                proposedChanges = createSubTaskChanges(subTasks, entity, context),
                metadata = ProposalMetadata(
                    aiAgent = context.agentId,
                    rationale = buildBreakdownRationale(opportunity, analysis),
                    confidence = ConfidenceLevel.fromValue(opportunity.confidence),
                    category = ProposalCategory.BREAKDOWN,
                    impactAssessment = assessBreakdownImpact(subTasks, entity),
                    context = context
                )
            )
        }
    }
}
```

### User Preferences AI Strategy

Optimizes user settings based on patterns:

```kotlin
class UserPreferencesAIStrategy(
    private val aiClient: AIClient,
    private val usageAnalyzer: UsageAnalyzer
) : EntityAIStrategy {
    
    override fun canHandle(entityType: EntityType): Boolean = 
        entityType == EntityType.USER_PREFERENCES
    
    override suspend fun analyzeEntity(
        entity: EntitySnapshot,
        context: AIAnalysisContext
    ): Result<EntityAnalysis, AIError> {
        val preferences = deserializeUserPreferences(entity.data)
        val usagePatterns = usageAnalyzer.getPatterns(context.userId)
        
        return aiClient.analyze(
            buildAnalysisPrompt(preferences, usagePatterns, context)
        ).map { response ->
            UserPreferencesAnalysis(
                usagePatterns = response.patterns,
                optimizationOpportunities = response.opportunities,
                inconsistencies = response.inconsistencies,
                accessibilityRecommendations = response.accessibility
            )
        }
    }
    
    override suspend fun generateProposals(
        entity: EntitySnapshot,
        analysis: EntityAnalysis,
        context: AIProposalContext
    ): Result<List<EntityChangeProposal>, AIError> {
        val prefsAnalysis = analysis as UserPreferencesAnalysis
        val proposals = mutableListOf<EntityChangeProposal>()
        
        // Theme optimization based on time of day
        if (prefsAnalysis.usagePatterns.hasTimeBasedThemePattern()) {
            proposals += generateThemeProposal(entity, prefsAnalysis, context)
        }
        
        // Keyboard shortcut optimization
        if (prefsAnalysis.optimizationOpportunities.hasShortcutOpportunities()) {
            proposals += generateShortcutProposal(entity, prefsAnalysis, context)
        }
        
        return Result.success(proposals)
    }
    
    private fun generateThemeProposal(
        entity: EntitySnapshot,
        analysis: UserPreferencesAnalysis,
        context: AIProposalContext
    ): EntityChangeProposal {
        val pattern = analysis.usagePatterns.getThemePattern()
        
        return EntityChangeProposal(
            id = ProposalId.generate(),
            entityType = EntityType.USER_PREFERENCES,
            entityId = entity.entityId,
            proposedChanges = listOf(
                EntityChange(
                    id = ChangeId.generate(),
                    entityType = EntityType.USER_PREFERENCES,
                    entityId = entity.entityId,
                    fieldPath = FieldPath("appearance.autoThemeSchedule"),
                    operation = ChangeOperation.UPDATE,
                    beforeValue = JsonPrimitive(false),
                    afterValue = JsonPrimitive(true),
                    version = context.targetVersion,
                    causedBy = CausedBy.AIAgent(context.agentId),
                    timestamp = Clock.System.now()
                ),
                EntityChange(
                    id = ChangeId.generate(),
                    entityType = EntityType.USER_PREFERENCES,
                    entityId = entity.entityId,
                    fieldPath = FieldPath("appearance.darkModeStart"),
                    operation = ChangeOperation.UPDATE,
                    beforeValue = null,
                    afterValue = JsonPrimitive(pattern.darkModeStart),
                    version = context.targetVersion,
                    causedBy = CausedBy.AIAgent(context.agentId),
                    timestamp = Clock.System.now()
                )
            ),
            metadata = ProposalMetadata(
                aiAgent = context.agentId,
                rationale = "I noticed you switch to dark mode around ${pattern.darkModeStart} " +
                          "and back to light mode around ${pattern.lightModeStart}. " +
                          "Enabling automatic theme switching would save you time.",
                confidence = ConfidenceLevel.HIGH,
                category = ProposalCategory.OPTIMIZATION,
                impactAssessment = ImpactAssessment(
                    estimatedTimeToImplement = Duration.seconds(1),
                    riskLevel = RiskLevel.NONE,
                    affectedEntities = setOf(entity.entityId),
                    reversible = true
                ),
                context = context
            )
        )
    }
}
```

## Analysis Results

### Entity Analysis Interface

All analysis results implement a common interface:

```kotlin
interface EntityAnalysis {
    val overallScore: Double
    val issues: List<AnalysisIssue>
    val opportunities: List<ImprovementOpportunity>
    val metadata: AnalysisMetadata
}

data class AnalysisIssue(
    val severity: IssueSeverity,
    val category: String,
    val description: String,
    val affectedFields: List<FieldPath> = emptyList()
)

data class ImprovementOpportunity(
    val type: OpportunityType,
    val description: String,
    val estimatedBenefit: BenefitEstimate,
    val confidence: Double
)

data class AnalysisMetadata(
    val analyzedAt: Instant,
    val analysisVersion: String,
    val confidence: Double,
    val dataQuality: DataQuality
)
```

### Scope-Specific Analysis

```kotlin
data class ScopeAnalysis(
    val complexityScore: Double,
    val taskBreakdownOpportunities: List<BreakdownOpportunity>,
    val hierarchyIssues: List<HierarchyIssue>,
    val priorityRecommendations: List<PriorityRecommendation>,
    val deadlineRisks: List<DeadlineRisk>,
    val dependencyIssues: List<DependencyIssue>,
    val resourceAllocationSuggestions: List<ResourceSuggestion>,
    override val overallScore: Double,
    override val issues: List<AnalysisIssue>,
    override val opportunities: List<ImprovementOpportunity>,
    override val metadata: AnalysisMetadata
) : EntityAnalysis

data class BreakdownOpportunity(
    val reason: String,
    val suggestedSubtasks: List<SubtaskSuggestion>,
    val confidence: Double,
    val estimatedTimeReduction: Duration?
)

data class HierarchyIssue(
    val issueType: HierarchyIssueType,
    val affectedScopes: List<ScopeId>,
    val suggestedReorganization: ReorganizationSuggestion
)
```

## Strategy Registration

### Strategy Registry

Manages available AI strategies:

```kotlin
class AIStrategyRegistry {
    private val strategies = mutableListOf<EntityAIStrategy>()
    
    fun register(strategy: EntityAIStrategy) {
        strategies.add(strategy)
        logger.info("Registered AI strategy for ${strategy.javaClass.simpleName}")
    }
    
    fun findStrategy(entityType: EntityType): EntityAIStrategy? =
        strategies.find { it.canHandle(entityType) }
    
    fun getAllStrategies(): List<EntityAIStrategy> = strategies.toList()
    
    fun getCapabilities(entityType: EntityType): Set<AICapability> =
        findStrategy(entityType)?.let { strategy ->
            // Extract capabilities from strategy
            extractCapabilities(strategy)
        } ?: emptySet()
}
```

### Dependency Injection

```kotlin
@Module
class AIStrategyModule {
    
    @Provides
    @Singleton
    fun provideAIStrategyRegistry(): AIStrategyRegistry = 
        AIStrategyRegistry()
    
    @Provides
    @IntoSet
    fun provideScopeAIStrategy(
        aiClient: AIClient,
        scopeRepository: ScopeRepository
    ): EntityAIStrategy = 
        ScopeAIStrategy(aiClient, scopeRepository)
    
    @Provides
    @IntoSet
    fun provideUserPreferencesAIStrategy(
        aiClient: AIClient,
        usageAnalyzer: UsageAnalyzer
    ): EntityAIStrategy = 
        UserPreferencesAIStrategy(aiClient, usageAnalyzer)
    
    @Provides
    @Singleton
    fun provideAIAgentService(
        registry: AIStrategyRegistry,
        strategies: Set<EntityAIStrategy>,
        proposalService: EntityProposalService
    ): AIAgentService {
        strategies.forEach { registry.register(it) }
        return AIAgentService(registry, proposalService)
    }
}
```

## MCP Integration

### MCP Strategy Wrapper

External AI agents integrate through MCP:

```kotlin
class MCPEntityAIStrategy(
    private val mcpClient: MCPClient,
    private val serverUri: String,
    private val supportedEntityTypes: Set<EntityType>,
    private val agentCapabilities: Set<AICapability>
) : EntityAIStrategy {
    
    override fun canHandle(entityType: EntityType): Boolean =
        entityType in supportedEntityTypes
    
    override suspend fun analyzeEntity(
        entity: EntitySnapshot,
        context: AIAnalysisContext
    ): Result<EntityAnalysis, AIError> {
        val request = MCPAnalysisRequest(
            entityType = entity.entityType.value,
            entityData = entity.data,
            analysisContext = context.toMCPContext()
        )
        
        return mcpClient.callTool(
            server = serverUri,
            tool = "analyze_entity",
            arguments = Json.encodeToJsonElement(request)
        ).mapCatching { response ->
            parseMCPAnalysisResponse(response, entity.entityType)
        }.mapError { 
            AIError.MCPError("Analysis failed", it)
        }
    }
    
    override suspend fun generateProposals(
        entity: EntitySnapshot,
        analysis: EntityAnalysis,
        context: AIProposalContext
    ): Result<List<EntityChangeProposal>, AIError> {
        val request = MCPProposalRequest(
            entityType = entity.entityType.value,
            entityData = entity.data,
            analysis = analysis.toMCPAnalysis(),
            proposalContext = context.toMCPContext(),
            maxProposals = context.maxProposals
        )
        
        return mcpClient.callTool(
            server = serverUri,
            tool = "generate_proposals",
            arguments = Json.encodeToJsonElement(request)
        ).mapCatching { response ->
            parseMCPProposalResponse(response, entity, context)
        }.mapError {
            AIError.MCPError("Proposal generation failed", it)
        }
    }
}
```

### MCP Discovery

Automatically discover and register MCP-based strategies:

```kotlin
class MCPAgentDiscovery(
    private val mcpRegistry: MCPServerRegistry,
    private val strategyRegistry: AIStrategyRegistry
) {
    suspend fun discoverAndRegisterAgents() {
        val servers = mcpRegistry.discoverServers()
        
        for (server in servers) {
            try {
                val capabilities = server.getCapabilities()
                
                if (capabilities.supports("entity_ai_strategy")) {
                    val supportedTypes = capabilities.getSupportedEntityTypes()
                    val agentCapabilities = capabilities.getAICapabilities()
                    
                    val strategy = MCPEntityAIStrategy(
                        mcpClient = server.client,
                        serverUri = server.uri,
                        supportedEntityTypes = supportedTypes,
                        agentCapabilities = agentCapabilities
                    )
                    
                    strategyRegistry.register(strategy)
                    logger.info("Registered MCP AI strategy for ${server.name}")
                }
            } catch (e: Exception) {
                logger.warn("Failed to register MCP server ${server.name}", e)
            }
        }
    }
}
```

## Validation

### Proposal Validation

Ensure proposals won't break entities:

```kotlin
override suspend fun validateProposal(
    proposal: EntityChangeProposal,
    entity: EntitySnapshot
): Result<ValidationResult, AIError> {
    val validationErrors = mutableListOf<ValidationError>()
    
    // Entity-specific validation
    proposal.proposedChanges.forEach { change ->
        validateChange(change, entity)?.let { error ->
            validationErrors += error
        }
    }
    
    // Cross-field validation
    validateCrossFieldConsistency(proposal, entity)?.let { errors ->
        validationErrors.addAll(errors)
    }
    
    // Business rule validation
    validateBusinessRules(proposal, entity)?.let { errors ->
        validationErrors.addAll(errors)
    }
    
    return Result.success(
        ValidationResult(
            isValid = validationErrors.none { it.severity == ValidationSeverity.ERROR },
            errors = validationErrors,
            warnings = validationErrors.filter { it.severity == ValidationSeverity.WARNING }
        )
    )
}

data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>,
    val warnings: List<ValidationError>
)

data class ValidationError(
    val field: FieldPath,
    val message: String,
    val severity: ValidationSeverity,
    val code: String? = null
)

enum class ValidationSeverity {
    ERROR,    // Prevents proposal application
    WARNING,  // Allows application but warns user
    INFO      // Informational only
}
```

## Learning and Feedback

### Incorporating Feedback

Strategies learn from user decisions:

```kotlin
override suspend fun incorporateFeedback(
    proposal: EntityChangeProposal,
    feedback: ProposalFeedback
): Result<Unit, AIError> {
    when (feedback.feedbackType) {
        FeedbackType.REJECTION_FEEDBACK -> {
            // Learn what types of proposals users reject
            learnFromRejection(proposal, feedback)
        }
        FeedbackType.MODIFICATION_FEEDBACK -> {
            // Learn how users modify proposals
            learnFromModification(proposal, feedback)
        }
        FeedbackType.OUTCOME_FEEDBACK -> {
            // Learn from actual outcomes
            learnFromOutcome(proposal, feedback)
        }
    }
    
    // Update strategy parameters
    updateStrategyParameters(feedback)
    
    return Result.success(Unit)
}

private suspend fun learnFromRejection(
    proposal: EntityChangeProposal,
    feedback: ProposalFeedback
) {
    // Track rejection patterns
    val rejectionPattern = RejectionPattern(
        category = proposal.metadata.category,
        confidence = proposal.metadata.confidence,
        reason = feedback.comments ?: "No reason provided",
        entityCharacteristics = extractCharacteristics(proposal)
    )
    
    // Store for future analysis
    storeRejectionPattern(rejectionPattern)
    
    // Adjust confidence thresholds
    if (shouldAdjustConfidence(rejectionPattern)) {
        adjustConfidenceThreshold(proposal.metadata.category)
    }
}
```

## Best Practices

### Strategy Implementation

1. **Single Responsibility**: Each strategy handles one entity type
2. **Stateless Design**: Strategies should not maintain state between calls
3. **Error Handling**: Return Result types with meaningful errors
4. **Performance**: Consider caching expensive computations
5. **Testing**: Provide test doubles for AI clients

### Analysis Quality

1. **Comprehensive Analysis**: Check multiple aspects of entities
2. **Contextual Awareness**: Use provided context effectively
3. **Confidence Calibration**: Be realistic about confidence levels
4. **Clear Rationale**: Explain reasoning in understandable terms
5. **Actionable Insights**: Focus on practical improvements

### Proposal Generation

1. **User-Centric**: Generate proposals that align with user goals
2. **Incremental Changes**: Prefer small, safe changes over large rewrites
3. **Respect Limits**: Honor maxProposals and other constraints
4. **Alternative Options**: Provide alternatives when possible
5. **Impact Assessment**: Accurately estimate time and risk

## Testing Strategies

### Unit Testing

```kotlin
class ScopeAIStrategyTest {
    private val mockAIClient = mockk<AIClient>()
    private val mockScopeRepository = mockk<ScopeRepository>()
    
    private val strategy = ScopeAIStrategy(mockAIClient, mockScopeRepository)
    
    @Test
    fun `should generate breakdown proposal for complex scope`() = runTest {
        val complexScope = createComplexScope()
        val entity = complexScope.toEntitySnapshot()
        val context = createTestContext()
        
        // Mock AI response
        every { mockAIClient.analyze(any(), any()) } returns Result.success(
            mockk<AIResponse> {
                every { extractComplexityScore() } returns 0.8
                every { extractBreakdownOpportunities() } returns listOf(
                    BreakdownOpportunity(
                        reason = "High complexity",
                        suggestedSubtasks = listOf(/* ... */),
                        confidence = 0.85
                    )
                )
            }
        )
        
        // Execute
        val analysisResult = strategy.analyzeEntity(entity, context)
        val analysis = analysisResult.getOrThrow()
        
        val proposalsResult = strategy.generateProposals(entity, analysis, context)
        val proposals = proposalsResult.getOrThrow()
        
        // Verify
        proposals shouldNotBe empty
        proposals.first().metadata.category shouldBe ProposalCategory.BREAKDOWN
        proposals.first().metadata.confidence shouldBe ConfidenceLevel.HIGH
    }
}
```

## Next Steps

- [Proposal Lifecycle](./proposal-lifecycle.md) - How proposals flow through the system
- [Entity Lifecycle](../entity-lifecycle/) - Foundation for AI strategies
- [Adding New Entity Types](../../../../tmp/adding-new-entity-types.md) - Implement your own strategy
