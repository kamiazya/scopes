package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectQueryAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ErrorMessageMapper
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for validating aspect values against their definitions.
 * Usage: scopes aspect validate <key> --value <value> [--value <value2>...]
 *
 * Note: Uses CliktError for error handling.
 */
class ValidateCommand :
    CliktCommand(
        name = "validate",
        help = "Validate aspect values against their definitions",
    ),
    KoinComponent {
    private val aspectQueryAdapter: AspectQueryAdapter by inject()

    private val key by argument(help = "The aspect key to validate")
    private val values by option("-v", "--value", help = "Value(s) to validate").multiple(required = true)

    override fun run() {
        runBlocking {
            aspectQueryAdapter.validateAspectValue(key, values).fold(
                ifLeft = { error ->
                    throw CliktError(ErrorMessageMapper.toUserMessage(error))
                },
                ifRight = { validatedValues ->
                    echo("✓ All values are valid for aspect '$key'")
                    validatedValues.forEach { value ->
                        echo("  - $value")
                    }
                },
            )
        }
    }
}
