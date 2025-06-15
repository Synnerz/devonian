package com.github.synnerz.devonian.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.LongArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

class BaseSubCommand(val name: String, val cb: (CommandContext<FabricClientCommandSource>, List<Any>) -> Int) {
    val commandArgs = mutableListOf<Pair<String, ArgumentType<*>>>()
    var isOptional = false

    /**
     * - Gets this [BaseSubCommand] entire command chain and combines it all together into a single command builder
     */
    fun command(): LiteralArgumentBuilder<FabricClientCommandSource>? {
        val base = literal(name)

        var current: ArgumentBuilder<FabricClientCommandSource, *>? = null

        if (isOptional)
            base.executes { ctx -> cb(ctx, emptyList()) }

        for ((argName, argType) in commandArgs.asReversed()) {
            val argBuilder = argument(argName, argType)

            if (current == null) {
                argBuilder.executes { ctx ->
                    val args = commandArgs.map { (key, type) -> getArgument(ctx, key, type) }
                    cb(ctx, args)
                }
            } else {
                argBuilder.then(current)
            }

            current = argBuilder
        }

        return if (current != null) base.then(current) else base.executes { ctx -> cb(ctx, emptyList()) }
    }

    /**
     * - Adds the specified argument type with its argument name
     * @param name The name of the argument
     * @Param type The type of the argument
     */
    fun add(name: String, type: ArgumentType<*>) = apply {
        commandArgs.add(name to type)
    }

    /**
     * - Sets a string argument with the specified name
     * @param name The name of the argument
     */
    fun string(name: String) = apply {
        add(name, StringArgumentType.string())
    }

    /**
     * - Sets a greedy string argument with the specified name
     * - Note: greedy means it'll take all the strings no matter whether there are spaces
     * @param name The name of the argument
     */
    fun greedyString(name: String) = apply {
        add(name, StringArgumentType.greedyString())
    }

    /**
     * - Sets a word argument with the specified name
     * - Note: a "word" argument is essentially just the "string" argument
     *  without the string being quotable
     *  @param name The name of the argument
     */
    fun word(name: String) = apply {
        add(name, StringArgumentType.word())
    }

    /**
     * - Sets a boolean argument with the specified name
     * @param name The name of the argument
     */
    fun bool(name: String) = apply {
        add(name, BoolArgumentType.bool())
    }

    /**
     * - Sets a double argument with the specified name and min/max (defaults to min/max values of each)
     * @param name The name of the argument
     */
    @JvmOverloads
    fun double(name: String, min: Double = Double.MIN_VALUE, max: Double = Double.MAX_VALUE) = apply {
        add(name, DoubleArgumentType.doubleArg(min, max))
    }

    /**
     * - Sets a float argument with the specified name and min/max (defaults to min/max values of each)
     * @param name The name of the argument
     */
    @JvmOverloads
    fun float(name: String, min: Float = Float.MIN_VALUE, max: Float = Float.MAX_VALUE) = apply {
        add(name, FloatArgumentType.floatArg(min, max))
    }

    /**
     * - Sets an integer argument with the specified name and min/max (defaults to min/max values of each)
     * @param name The name of the argument
     */
    @JvmOverloads
    fun integer(name: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE) = apply {
        add(name, IntegerArgumentType.integer(min, max))
    }

    /**
     * - Sets a long argument with the specified name and min/max (defaults to min/max values of each)
     * @param name The name of the argument
     */
    @JvmOverloads
    fun long(name: String, min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE) = apply {
        add(name, LongArgumentType.longArg(min, max))
    }

    private fun getArgument(ctx: CommandContext<FabricClientCommandSource>, name: String, type: ArgumentType<*>): Any {
        return when (type) {
            is StringArgumentType -> StringArgumentType.getString(ctx, name)
            is BoolArgumentType -> BoolArgumentType.getBool(ctx, name)
            is DoubleArgumentType -> DoubleArgumentType.getDouble(ctx, name)
            is FloatArgumentType -> FloatArgumentType.getFloat(ctx, name)
            is LongArgumentType -> LongArgumentType.getLong(ctx, name)
            else -> IntegerArgumentType.getInteger(ctx, name)
        }
    }
}