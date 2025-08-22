package io.github.kamiazya.scopes.scopemanagement.application.dto

data class UpdateScopeInput(val id: String, val title: String? = null, val description: String? = null, val parentId: String? = null)
