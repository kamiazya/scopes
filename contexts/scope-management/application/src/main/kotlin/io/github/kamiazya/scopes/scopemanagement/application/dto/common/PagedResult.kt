package io.github.kamiazya.scopes.scopemanagement.application.dto.common
import io.github.kamiazya.scopes.scopemanagement.application.dto.common.DTO
/**
 * Generic paged result for application layer.
 */
data class PagedResult<T>(val items: List<T>, val totalCount: Int, val offset: Int, val limit: Int)
