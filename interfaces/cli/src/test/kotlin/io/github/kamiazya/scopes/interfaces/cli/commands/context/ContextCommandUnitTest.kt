package io.github.kamiazya.scopes.interfaces.cli.commands.context

import io.github.kamiazya.scopes.interfaces.cli.commands.ContextCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.configureSubcommands
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ContextCommandUnitTest :
    DescribeSpec({
        describe("ContextCommand") {
            it("should have context as the command name") {
                val command = ContextCommand()
                command.commandName shouldBe "context"
            }

            it("should show help text") {
                val command = ContextCommand()
                command.commandHelp shouldContain "Manage context views"
            }

            it("should have subcommands when configured") {
                val command = ContextCommand().configureSubcommands()
                val subcommandNames = command.registeredSubcommands().map { it.commandName }

                subcommandNames shouldBe listOf("create", "list", "show", "edit", "delete", "switch", "current")
            }
        }
    })
