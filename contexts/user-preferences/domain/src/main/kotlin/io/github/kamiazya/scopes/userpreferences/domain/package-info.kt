/**
 * User Preferences Domain Layer
 *
 * This package contains the core business logic for user preferences management.
 * Following Domain-Driven Design (DDD) principles with a customer-supplier relationship
 * to other bounded contexts (Scope Management, Workspace Management, etc.).
 *
 * Key domain concepts:
 * - UserPreferencesAggregate: Aggregate root for user settings
 * - HierarchyPreferences: Scope hierarchy configuration (maxDepth, maxChildrenPerScope)
 * - Value semantics: null = unlimited/no preference (use system defaults)
 * - Repository Interface: Contract for persistence
 *
 * Design principle: Zero-configuration start
 * - All preferences have sensible defaults
 * - Users never required to configure before using the system
 * - Preferences enhance but never block functionality
 *
 * Marked with @DomainLayer to explicitly indicate this is the domain layer
 * in the hexagonal/layered architecture.
 */
@file:org.jmolecules.architecture.layered.DomainLayer

package io.github.kamiazya.scopes.userpreferences.domain
