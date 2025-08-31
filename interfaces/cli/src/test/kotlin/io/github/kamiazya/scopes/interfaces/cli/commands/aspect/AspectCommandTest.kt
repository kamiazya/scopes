package io.github.kamiazya.scopes.interfaces.cli.commands.aspect

import io.github.kamiazya.scopes.interfaces.cli.commands.AspectCommand
import io.github.kamiazya.scopes.interfaces.cli.commands.configureSubcommands
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class AspectCommandTest :
    DescribeSpec({
        describe("AspectCommand") {
            it("should have aspect as the command name") {
                val command = AspectCommand()
                command.commandName shouldBe "aspect"
            }

            it("should show help text") {
                val command = AspectCommand()
                command.commandHelp shouldContain "Manage scope aspects"
            }

            it("should have subcommands when configured") {
                val command = AspectCommand().configureSubcommands()
                val subcommandNames = command.registeredSubcommands().map { it.commandName }

                subcommandNames shouldBe listOf("define", "show", "edit", "rm", "definitions", "validate", "set", "list", "remove")
            }
        }
    })
