package io.github.kamiazya.scopes.domain.service

import io.github.kamiazya.scopes.domain.error.DomainError
import io.github.kamiazya.scopes.domain.valueobject.ScopeId
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

    "system allows creating scope at maximum allowed depth boundary" {
        // When creating a scope at exactly the maximum allowed depth (MAX_HIERARCHY_DEPTH - 1)
        val result = ScopeValidationService.validateHierarchyDepthWithContext(
            parentDepth = ScopeValidationService.MAX_HIERARCHY_DEPTH - 1
        )

        // Then the system allows this as it's within the valid boundary
        result.shouldBeRight()
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

    "system allows creating child at maximum allowed children boundary" {
        // When adding a child at exactly the maximum allowed children count (MAX_CHILDREN_PER_PARENT - 1)
        val result = ScopeValidationService.validateChildrenLimitWithContext(
            currentChildrenCount = ScopeValidationService.MAX_CHILDREN_PER_PARENT - 1
        )

        // Then the system allows this as it's within the valid boundary
        result.shouldBeRight()
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
            existsInParentContext = false,
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
            existsInParentContext = true,
            title = "User Management",
            parentId = parentId
        )

        // Then the system prevents this to avoid confusion in project organization
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle>()
    }

    "system prevents duplicate scope names at the root level to maintain consistent uniqueness" {
        // When someone tries to create a root-level scope with a name that already exists
        val result = ScopeValidationService.validateTitleUniquenessWithContext(
            existsInParentContext = true,
            title = "Mobile App",  // A scope with this name already exists at root level
            parentId = null  // Root level - unified uniqueness applies here too
        )

        // Then the system prevents this to maintain consistent title uniqueness across all levels
        val error = result.shouldBeLeft()
        error.shouldBeInstanceOf<DomainError.ScopeBusinessRuleViolation.ScopeDuplicateTitle>()
    }
})
