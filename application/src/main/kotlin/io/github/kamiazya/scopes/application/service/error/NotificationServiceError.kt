package io.github.kamiazya.scopes.application.service.error

/**
 * Notification service errors for message delivery and event distribution failures.
 * 
 * This hierarchy provides comprehensive error types for notification concerns
 * including message delivery, event distribution, template processing, and configuration.
 * 
 * Based on Serena MCP research on notification patterns:
 * - Reliable message delivery with retry mechanisms
 * - Event-driven notification distribution
 * - Template-based message generation
 * - Channel-specific configuration management
 * 
 * Following functional error handling principles for composability and recovery.
 */
sealed class NotificationServiceError