package io.github.kamiazya.scopes.application.service

/**
 * Default aspect definitions with rules for development workflows.
 * Provides common aspects that most development teams use with validation rules.
 */
object AspectDefinitionDefaultsWithRules {

    /**
     * Get all default aspect definitions with rules using DSL.
     */
    fun all(): List<AspectDefinitionWithRules> = aspectDefinitionsWithRules {
        ordered("priority") {
            description = "Task priority level"
            values("low", "medium", "high", "critical")
        }

        ordered("status") {
            description = "Work item status in workflow"
            values("todo", "ready", "in_progress", "review", "done")
        }

        ordered("type") {
            description = "Type of work item"
            values("feature", "bug", "docs", "refactor", "test", "chore")
        }

        ordered("complexity") {
            description = "Implementation complexity level"
            values("simple", "medium", "complex")
        }

        numeric("effort") {
            description = "Estimated effort in hours"
            rules {
                range(min = 0.5, max = 100.0, "Effort must be between 0.5 and 100 hours")
                required("Effort is required when status is in progress") {
                    aspectEquals("status", "in_progress")
                }
            }
        }

        boolean("blocked") {
            description = "Whether the item is blocked"
        }

        ordered("estimate") {
            description = "Story point estimation (Fibonacci sequence)"
            values("1", "2", "3", "5", "8", "13")
        }

        ordered("tags") {
            description = "Categorization tags"
            allowMultiple = true
            values("frontend", "backend", "mobile", "api", "ui", "database", "testing", "documentation")
        }
    }

    /**
     * Get a specific default definition by key name.
     */
    fun getByKey(keyName: String): AspectDefinitionWithRules? =
        all().find { it.definition.key.value == keyName }

    /**
     * Get all available default aspect keys.
     */
    fun availableKeys(): List<String> = all().map { it.definition.key.value }
}
