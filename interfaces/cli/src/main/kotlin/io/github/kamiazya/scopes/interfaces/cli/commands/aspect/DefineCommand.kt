package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.int
import io.github.kamiazya.scopes.interfaces.cli.adapters.AspectCommandAdapter
import io.github.kamiazya.scopes.interfaces.cli.mappers.ContractErrorMessageMapper
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectType
import io.github.kamiazya.scopes.scopemanagement.domain.valueobject.AspectValue
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Command for defining a new aspect.
 * Usage: scopes aspect define <key> --type <type> --description <desc> [type-specific options]
 */
class DefineCommand :
    CliktCommand(
        name = "define",
        help = "Define a new aspect",
    ),
    KoinComponent {
    private val aspectCommandAdapter: AspectCommandAdapter by inject()

    private val key by argument(help = "The aspect key (e.g., 'priority', 'status')")
    private val description by option("-d", "--description", help = "Description of the aspect").required()
    private val type by option("-t", "--type", help = "Type of the aspect")
        .choice("text", "numeric", "boolean", "ordered", "duration")
        .required()

    // Text type options
    private val maxLength by option("--max-length", help = "Maximum length for text type").int()

    // Numeric type options
    private val min by option("--min", help = "Minimum value for numeric type").double()
    private val max by option("--max", help = "Maximum value for numeric type").double()

    // Ordered type options
    private val values by option("--values", help = "Comma-separated list of allowed values for ordered type")

    override fun run() {
        runBlocking {
            // Validate and trim inputs
            val trimmedKey = key.trim()
            val trimmedDescription = description.trim()

            if (trimmedKey.isBlank()) {
                echo("Error: Aspect key cannot be empty or blank", err = true)
                return@runBlocking
            }

            if (trimmedDescription.isBlank()) {
                echo("Error: Description cannot be empty or blank", err = true)
                return@runBlocking
            }

            // Parse aspect type based on the type parameter and options
            val aspectType = when (type) {
                "text" -> {
                    // Note: maxLength constraint is not supported in current domain model
                    if (maxLength != null) {
                        echo("Warning: max-length constraint is not supported in current implementation", err = true)
                    }
                    AspectType.Text
                }
                "numeric" -> {
                    // Note: min/max constraints are not supported in current domain model
                    if (min != null || max != null) {
                        echo("Warning: min/max constraints are not supported in current implementation", err = true)
                    }
                    AspectType.Numeric
                }
                "boolean" -> AspectType.BooleanType
                "ordered" -> {
                    if (values == null) {
                        echo("Error: --values is required for ordered type", err = true)
                        return@runBlocking
                    }
                    val valueList = values!!.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (valueList.isEmpty()) {
                        echo("Error: --values cannot be empty after trimming", err = true)
                        return@runBlocking
                    }
                    val aspectValues = valueList.map { value ->
                        AspectValue.create(value).fold(
                            { error ->
                                echo("Error: Invalid value '$value': $error", err = true)
                                return@runBlocking
                            },
                            { it },
                        )
                    }
                    AspectType.Ordered(allowedValues = aspectValues)
                }
                "duration" -> AspectType.Duration
                else -> {
                    echo("Error: Invalid type '$type'", err = true)
                    return@runBlocking
                }
            }

            // Define the aspect
            val result = aspectCommandAdapter.defineAspect(
                key = trimmedKey,
                description = trimmedDescription,
                type = aspectType.toString(),
            )

            result.fold(
                ifLeft = { error ->
                    echo("Error: Failed to define aspect '$trimmedKey': ${ContractErrorMessageMapper.getMessage(error)}", err = true)
                },
                ifRight = {
                    echo("Aspect '$trimmedKey' defined successfully")
                    echo("Type: $aspectType")
                    echo("Description: $trimmedDescription")
                },
            )
        }
    }

    private fun formatType(type: AspectType): String = when (type) {
        is AspectType.Text -> "Text"
        is AspectType.Numeric -> "Numeric"
        is AspectType.BooleanType -> "Boolean"
        is AspectType.Ordered -> "Ordered (${type.allowedValues.joinToString(", ") { it.value }})"
        is AspectType.Duration -> "Duration (ISO 8601)"
    }
}
