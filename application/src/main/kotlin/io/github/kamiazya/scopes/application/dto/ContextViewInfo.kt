package io.github.kamiazya.scopes.application.dto

/**
 * DTO representing context view information.
 * Used to transfer context view data between layers.
 */
data class ContextViewInfo(
    val id: String,
    val name: String,
    val filterExpression: String,
    val description: String? = null,
    val createdAt: String,
    val updatedAt: String
) : DTO