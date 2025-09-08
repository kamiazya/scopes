package io.github.kamiazya.scopes.scopemanagement.application.dto.scope
data class UpdateScopeInput(val id: String, val title: String? = null, val description: String? = null, val parentId: String? = null)
