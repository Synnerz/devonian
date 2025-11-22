package com.github.synnerz.devonian.commands

import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

// TODO: add args to this api (?)

class BaseCommand(
    val name: String,
    val aliases: List<String> = emptyList(),
    val cb: (CommandContext<FabricClientCommandSource>) -> Int,
) {
    var subcommands = mutableListOf<BaseSubCommand>()

    /**
     * - Creates the command with all the sub commands combined, together and then proceeds
     *  to register the command
     */
    fun register() {
        val command = literal(name).executes(cb)
        for (subcommand in subcommands) {
            command.then(subcommand.command())
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(command)
        }

        if (aliases.isEmpty()) return
        aliases.forEach {
            val aliasCommand = literal(it).executes(cb)
            for (subcommand in subcommands)
                aliasCommand.then(subcommand.command())

            ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
                dispatcher.register(aliasCommand)
            }
        }
    }

    /**
     * - Adds a sub command that is then returned as a value from this method call
     * @param name The name of the sub command (or rather the sub command itself)
     * @param cb The callback that will trigger whenever this command is ran
     */
    fun subcommand(name: String, cb: (CommandContext<FabricClientCommandSource>, List<Any>) -> Int): BaseSubCommand {
        val subcommand = BaseSubCommand(name, cb)

        subcommands.add(subcommand)

        return subcommand
    }

    /**
     * - Adds a sub command that is then returned as a value from this method call
     * @param name The name of the sub command (or rather the sub command itself)
     * @param isOptional Whether the arguments for this sub command are optional or not.
     * By default, it's set to `false` which heavily requires each argument to have been passed through
     * otherwise it will not trigger.
     * If however, this is set to `true` it will avoid having to require each of the arguments and
     * trigger even if one of them is not filled in.
     * @param cb The callback that will trigger whenever this command is ran
     */
    fun subcommand(name: String, isOptional: Boolean, cb: (CommandContext<FabricClientCommandSource>, List<Any>) -> Int): BaseSubCommand {
        val subcommand = BaseSubCommand(name, cb)
        subcommand.isOptional = isOptional
        subcommands.add(subcommand)

        return subcommand
    }
}