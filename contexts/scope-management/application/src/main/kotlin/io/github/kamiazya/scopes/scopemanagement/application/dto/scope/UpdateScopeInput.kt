package io.github.kamiazya.scopes.scopemanagement.application.dto.scope

/**
 * Input data transfer object for updating an existing scope.
 *
 * This DTO is used within the application layer to transfer update request data.
 * It contains only primitive types to maintain layer separation.
 * All fields except `id` are optional to support partial updates.
 *
 * @property id The unique identifier of the scope to update (required)
 * @property title The new title for the scope (optional)
 * @property description The new description for the scope (optional)
 * @property parentId The new parent scope ID for hierarchy changes (optional)
 */
data class UpdateScopeInput(val id: String, val title: String? = null, val description: String? = null, val parentId: String? = null)
