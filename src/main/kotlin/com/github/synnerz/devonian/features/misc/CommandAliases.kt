package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.CancellableEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.components.UITextInput
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color

object CommandAliases : Screen(Component.literal("Devonian.CommandAliases")) {
    private const val KEY_NAME = "CommandAliases"
    private val components = mutableListOf<UIRect>()
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(30.0, 17.5, 40.0, 65.0, parent = background).apply {
        setColor(Color(25, 25, 25, 255))
    }
    private val aliasInputRect = UIRect(1.0, 1.0, 38.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Alias", true))
    }
    private val commandInputRect = UIRect(40.0, 1.0, 38.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Command", true))
    }
    private val removeRect = UIRect(79.0, 1.0, 20.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§c-", true))
    }
    private val addRect = UIRect(39.5, 89.0, 20.0, 10.0, parent = main).apply {
        // TODO: if the current page is full, add it to the next page and switch pages for the user
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§a+", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            createAlias(if (components.isEmpty()) 1 else 1 + (components.size % 7), "p", "placeholder")
        }
    }
    private val leftArrow = UIRect(1.0, 89.0, 10.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "<-", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            currentPage--
        }
        hide()
    }
    private val rightArrow = UIRect(89.0, 89.0, 10.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "->", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            currentPage++
        }
    }
    private var currentPage = 0
        set(value) {
            field = value.coerceIn(0, components.size / 7)
            onUpdate()
        }
    private val aliasesList = mutableListOf<CommandAlias>()
    private var lastTrigger = -1L

    data class CommandAlias(var alias: String, var command: String) {
        fun onCommand(msg: String, event: CancellableEvent) {
            if (!shouldTrigger()) return
            if (msg == alias) {
                event.cancel()
                ChatUtils.say("/$command")
                return
            }

            val cmd = msg.split(" ").getOrNull(0) ?: return
            val args = msg.split("$cmd ").getOrNull(1) ?: return
            if (cmd != alias) return
            event.cancel()
            ChatUtils.say("/$command $args")
        }

        fun shouldTrigger(): Boolean {
            if (alias.isBlank()) return false
            if (command.isBlank()) return false
            if (command == "placeholder") return false
            return true
        }
    }

    fun initialize() {
        Config.set(KEY_NAME, JsonObject())

        Config.onAfterLoad {
            val cachedData = Config.get<Map<String, JsonElement>>(KEY_NAME) ?: return@onAfterLoad
            cachedData.forEach {
                val alias = it.key
                val command = it.value.asString
                createAlias(if (components.isEmpty()) 1 else 1 + (components.size % 7), alias, command)
            }
        }

        DevonianCommand.command.subcommand("cmdali") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            1
        }
        DevonianCommand.command.subcommand("commandalias") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            1
        }

        ClientSendMessageEvents.ALLOW_COMMAND.register { msg ->
            val event = object : CancellableEvent() {}
            onCommand(msg, event)
            !event.isCancelled()
        }
    }

    private fun onUpdate() {
        val currentMax = components.size / 7
        when (currentPage) {
            0 -> {
                leftArrow.hide()
                if (currentMax == 0 && components.isEmpty()) rightArrow.hide()
                else rightArrow.unhide()
            }
            currentMax -> {
                rightArrow.hide()
                leftArrow.unhide()
            }
            else -> {
                leftArrow.unhide()
                rightArrow.unhide()
            }
        }

        for (idx in components.indices) {
            val comp = components[idx]
            val page = idx / 7
            if (page == currentPage) comp.unhide()
            else comp.hide()
        }
    }

    private fun createAlias(idx: Int, alias: String, command: String) {
        val data = CommandAlias(alias, command)
        val yy = (11 * idx) + 1.0
        val parentBg = UIRect(0.0, yy, 100.0, 10.0, parent = main).apply {
            setColor(Color(35, 35, 35, 0))
            hide()
        }
        val aliasInput = UITextInput(1.0, 0.0, 38.0, 100.0, alias, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.alias = text.replace("/", "")
                updateCache()
            }
        }
        val commandInput = UITextInput(40.0, 0.0, 38.0, 100.0, command, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.command = text.replace("/", "")
                updateCache()
            }
        }
        val remove = UIRect(79.0, 0.0, 20.0, 100.0, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "X", true).apply { setColor(Color.RED) })
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                aliasesList.remove(data)
                components.remove(parentBg)
                ChatUtils.sendMessage("&cRemoved CommandAlias &7[${data.alias} > ${data.command}]", true)
                updateCache()
                parentBg.remove()
                rebuildChildren()
            }
        }

        components.add(parentBg)
        aliasesList.add(data)
        onUpdate()
    }

    private fun rebuildChildren() {
        // workaround to properly remove an element from the list and update the `y` axis of every other
        for (comp in components)
            main.children.remove(comp)
        components.clear()
        main.markDirty()

        val aliasListCopy = aliasesList.toMutableList()
        aliasesList.clear()

        for (data in aliasListCopy)
            createAlias(if (components.isEmpty()) 1 else 1 + (components.size % 7), data.alias, data.command)
    }

    private fun onCommand(msg: String, event: CancellableEvent) {
        if (lastTrigger != -1L && System.currentTimeMillis() - lastTrigger < 200) return
        for (alias in aliasesList)
            alias.onCommand(msg, event)
        lastTrigger = System.currentTimeMillis()
    }

    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.render(guiGraphics, i, j, f)
        background.draw()
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.scancode)
        return super.keyPressed(keyEvent)
    }

    override fun isPauseScreen(): Boolean {
        return false
    }

    private fun updateCache() {
        val obj = JsonObject()

        for (alias in aliasesList)
            obj.addProperty(alias.alias, alias.command)

        Config.set(KEY_NAME, obj)
    }
}