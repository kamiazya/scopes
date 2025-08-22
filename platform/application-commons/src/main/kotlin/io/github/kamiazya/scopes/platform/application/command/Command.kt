package io.github.kamiazya.scopes.platform.application.command

import io.github.kamiazya.scopes.platform.application.dto.DTO

/**
 * Marker interface for command inputs that modify state.
 * Commands represent write operations in CQRS pattern.
 */
interface Command : DTO
