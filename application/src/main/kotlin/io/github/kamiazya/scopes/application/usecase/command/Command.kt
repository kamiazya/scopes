package io.github.kamiazya.scopes.application.usecase.command

import io.github.kamiazya.scopes.application.dto.DTO

/**
 * Marker interface for command inputs that modify state.
 * Commands represent write operations in CQRS pattern.
 */
interface Command : DTO
