package io.github.kamiazya.scopes.platform.application.query

import io.github.kamiazya.scopes.platform.application.dto.DTO

/**
 * Marker interface for query inputs that read state.
 * Queries represent read operations in CQRS pattern.
 */
interface Query : DTO
