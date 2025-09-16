package io.github.kamiazya.scopes.scopemanagement.application.dto.common

/**
 * Generic paged result for application layer.
 */
data class PagedResult<T>(val items: List<T>, val totalCount: Int, val offset: Int, val limit: Int)
