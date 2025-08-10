package io.github.kamiazya.scopes.presentation.cli.utils

import io.github.kamiazya.scopes.application.error.AppErrorTranslator
import io.github.kamiazya.scopes.application.error.ApplicationError

/**
 * Extension function to format ApplicationError into user-friendly messages.
 * Delegates to the application layer's AppErrorTranslator for consistent messaging.
 */
fun ApplicationError.toUserMessage(translator: AppErrorTranslator): String = 
    translator.translate(this)

/**
 * Extension function for multiple errors.
 */
fun List<ApplicationError>.toUserMessage(translator: AppErrorTranslator): String = 
    translator.translateMultiple(this)
