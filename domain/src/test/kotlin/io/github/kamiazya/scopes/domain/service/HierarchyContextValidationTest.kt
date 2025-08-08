package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.valueobject.ScopeId
import io.github.kamiazya.scopes.domain.error.DomainError
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for organizational hierarchy constraints focusing on business rules and user productivity.
 *
 * These constraints ensure the system remains usable and performant while supporting
 * complex organizational structures without becoming unwieldy for users.
 */
class HierarchyContextValidationTest : StringSpec({

    "system supports reasonable organizational hierarchy depth for complex project structures" {
        // When teams organize work in nested project structures (Project -> Epic -> Story -> Subtask -> Detail)
        val result = ScopeValidationService.validateHierarchyDepthWithContext(
            parentDepth = 5  // Reasonable nesting for complex organizations
        )

        // Then the system supports this level of organizational complexity
        result.shouldBeRight()
    }

    "system prevents excessively deep hierarchies that would harm user navigation and performance" {
        // When someone tries to create a scope at the maximum allowed depth
        val result = ScopeValidationService.validateHierarchyDepthWithContext(
            parentDepth = ScopeValidationService.MAX_HIERARCHY_DEPTH
        )

        // Then the system prevents this to maintain usable navigation and system performance
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded>()
    }

    "system enforces hierarchy limits to prevent infinite nesting scenarios" {
        // When someone tries to create a scope beyond the maximum organizational depth
        val result = ScopeValidationService.validateHierarchyDepthWithContext(
            parentDepth = ScopeValidationService.MAX_HIERARCHY_DEPTH + 1
        )

        // Then the system prevents this to protect against unwieldy project structures
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeMaxDepthExceeded>()
    }

    "system supports teams managing substantial numbers of parallel work items" {
        // When teams need to organize many parallel tasks or features under a parent scope
        val result = ScopeValidationService.validateChildrenLimitWithContext(
            currentChildrenCount = 50  // Realistic for large feature development
        )

        // Then the system accommodates substantial but manageable organizational breadth
        result.shouldBeRight()
    }

    "system prevents scope proliferation that would overwhelm project management interfaces" {
        // When a parent scope reaches the maximum number of direct children
        val result = ScopeValidationService.validateChildrenLimitWithContext(
            currentChildrenCount = ScopeValidationService.MAX_CHILDREN_PER_PARENT
        )

        // Then the system prevents additional children to maintain usable project views
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded>()
    }

    "system enforces child limits to maintain interface usability and system performance" {
        // When someone tries to exceed the reasonable limit for children under one parent
        val result = ScopeValidationService.validateChildrenLimitWithContext(
            currentChildrenCount = ScopeValidationService.MAX_CHILDREN_PER_PARENT + 1
        )

        // Then the system prevents this to ensure lists remain manageable and performant
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeMaxChildrenExceeded>()
    }

    "teams can create multiple scopes with clear, distinct names within project boundaries" {
        // When a team creates a new scope with a unique name within their project context
        val parentId = ScopeId.generate()
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            titleExists = false,
            title = "Authentication Feature",
            parentId = parentId
        )

        // Then the system allows this clear organizational structure
        result.shouldBeRight()
    }

    "system prevents scope name conflicts that would cause confusion in project navigation" {
        // When someone tries to create a scope with a name that already exists in the same parent
        val parentId = ScopeId.generate()
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            titleExists = true,
            title = "User Management",
            parentId = parentId
        )

        // Then the system prevents this to avoid confusion in project organization
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle>()
    }

    "multiple teams can use similar scope names at the root level for their independent projects" {
        // When different teams want to create root-level projects with similar names
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            titleExists = true,
            title = "Mobile App",  // Another team might also have "Mobile App" project
            parentId = null  // Root level - different organizational contexts
        )

        // Then the system allows this since root-level scopes serve different teams/contexts
        result.shouldBeRight()
    }
})
