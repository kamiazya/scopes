package io.github.kamiazya.scopes.platform.domain.event

/**
 * Annotation to declare a stable type identifier for a domain event.
 *
 * This identifier is used for persistence and must remain stable across versions.
 * Use semantic versioning in the identifier to support schema evolution.
 *
 * Example:
 * @EventTypeId("scope-management.scope.created.v1")
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventTypeId(val value: String)
