package io.github.kamiazya.scopes.scopemanagement.domain.service.alias

/**
 * Strategy for handling alias conflicts when they occur.
 */
enum class AliasConflictResolutionStrategy {
    /** Fail immediately when conflicts are detected */
    FAIL_FAST,

    /** Log conflicts and continue with best effort */
    LOG_AND_CONTINUE,

    /** Attempt to resolve conflicts by generating alternative names */
    AUTO_RESOLVE,
}
