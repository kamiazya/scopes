package io.github.kamiazya.scopes.eventstore.domain.model

/**
 * Annotation to declare a stable type identifier for a domain event.
 *
 * This identifier is used for persistence and must remain stable across versions.
 * Use semantic versioning in the identifier to support schema evolution.
 *
 * Example:
 * ```kotlin
 * @EventTypeId("scope-management.scope.created.v1")
 * data class ScopeCreatedEvent(...) : DomainEvent
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventTypeId(val value: String)
