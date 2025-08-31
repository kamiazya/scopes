package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for validating aspect values against their definitions.
 * Usage: scopes aspect validate <key> --value <value> [--value <value2>...]
 */
class ValidateCommand :
    CliktCommand(
        name = "validate",
        help = "Validate aspect values against their definitions",
    ),
    KoinComponent {
    private val aspectCommandAdapter: AspectCommandAdapter by inject()

    private val key by argument(help = "The aspect key to validate")
    private val values by option("-v", "--value", help = "Value(s) to validate").multiple(required = true)

    override fun run() {
        runBlocking {
            aspectCommandAdapter.validateAspectValue(key, values).fold(
                { error ->
                    echo("Validation failed: ${formatError(error)}", err = true)
                },
                { validatedValues ->
                    echo("✓ All values are valid for aspect '$key'")
                    validatedValues.forEach { value ->
                        echo("  - $value")
                    }
                },
            )
        }
    }

    private fun formatError(error: Any): String = when (error) {
        is io.github.kamiazya.scopes.scopemanagement.domain.error.ScopesError.ValidationFailed -> {
            when (val details = error.details) {
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError.InvalidNumericValue ->
                    "Value '${details.value}' is not a valid number for aspect '${details.aspectKey}'"
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError.InvalidBooleanValue ->
                    "Value '${details.value}' is not a valid boolean for aspect '${details.aspectKey}'. Valid values: true, false, yes, no, 1, 0"
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError.ValueNotInAllowedList ->
                    "Value '${details.value}' is not allowed for aspect '${details.aspectKey}'. Allowed values: ${details.allowedValues.joinToString(", ")}"
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError.MultipleValuesNotAllowed ->
                    "Multiple values are not allowed for aspect '${details.aspectKey}'"
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError.RequiredAspectsMissing ->
                    "Required aspects are missing: ${details.missingKeys.joinToString(", ")}"
                is io.github.kamiazya.scopes.scopemanagement.domain.error.ValidationError.InvalidDurationValue ->
                    "Value '${details.value}' is not a valid ISO 8601 duration for aspect '${details.aspectKey}'. Examples: P1D, PT2H30M, P1W"
                else -> error.message
            }
        }
        else -> error.toString()
    }
}
