/**
 * Scope Management Domain Layer
 *
 * This package contains the core business logic and domain model for scope management.
 * Following Domain-Driven Design (DDD) principles, this layer is independent of
 * infrastructure concerns and defines the ubiquitous language of the bounded context.
 *
 * Key domain concepts:
 * - Scope: The central aggregate representing any unit of work
 * - ScopeAggregate: Event-sourced aggregate root with business logic
 * - Value Objects: ScopeId, ScopeTitle, ScopeDescription
 * - Domain Events: ScopeCreated, ScopeUpdated, ScopeDeleted, etc.
 * - Repository Interfaces: Contracts for persistence (implemented in infrastructure)
 *
 * Marked with @DomainLayer to explicitly indicate this is the domain layer
 * in the hexagonal/layered architecture.
 */
@file:org.jmolecules.architecture.layered.DomainLayer

package io.github.kamiazya.scopes.scopemanagement.domain
