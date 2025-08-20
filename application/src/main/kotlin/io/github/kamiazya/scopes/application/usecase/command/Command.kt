package io.github.kamiazya.scopes.application.usecase.command

import io.github.kamiazya.scopes.application.dto.DTO

/**
 * Marker interface for command inputs that modify state.
 * Commands represent write operations in CQRS pattern.
 */
interface Command : DTO

/**
 * Command to assign a canonical alias to a scope.
 * Canonical aliases are the primary identifiers and only one exists per scope.
 */
data class AssignCanonicalAlias(val scopeId: String, val aliasName: String) : Command

/**
 * Command to assign a custom alias to a scope.
 * Multiple custom aliases can exist per scope.
 */
data class AssignCustomAlias(val scopeId: String, val aliasName: String) : Command

/**
 * Command to remove an alias.
 * Only custom aliases can be removed; canonical aliases must be replaced.
 */
data class RemoveAlias(val aliasName: String) : Command

/**
 * Command to generate and assign a canonical alias using Haikunator pattern.
 */
data class GenerateCanonicalAlias(val scopeId: String) : Command
