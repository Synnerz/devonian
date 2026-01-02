package com.github.synnerz.devonian.features.misc.hiders

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.components.UITextInput
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color

object CancelMessages : Screen(Component.literal("Devonian.CancelMessages")) {
    private const val KEY_NAME = "CancelMessages"
    private val components = mutableListOf<UIRect>()
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(30.0, 17.5, 40.0, 65.0, parent = background).apply {
        setColor(Color(25, 25, 25, 255))
    }
    private val cancelMsgInput = UIRect(1.0, 1.0, 77.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Message", true))
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
            createCancel(if (components.isEmpty()) 1 else 1 + (components.size % 7), "placeholder")
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
    private val messagesList = mutableSetOf<MessageData>()

    data class MessageData(var message: String) {
        var cachedRegex: Regex? = null

        init {
            checkRegex()
        }

        fun onMessage(event: ChatEvent): Boolean {
            if (!shouldTrigger()) return false
            if (cachedRegex != null && event.matches(cachedRegex!!) != null) return true
            if (event.message != message) return false
            event.cancel()
            return true
        }

        fun shouldTrigger(): Boolean {
            if (message.isBlank()) return false
            if (message == "placeholder") return false
            return true
        }

        fun isRegex(): Boolean {
            return message.startsWith("/") && message.endsWith("/")
        }

        fun checkRegex() {
            if (isRegex()) {
                val reg = message.drop(1).dropLast(1)
                try {
                    // Regex taken from <https://github.com/ChatTriggers/ChatTriggers> under MIT license
                    // i wasn't lazy this simply works better for this specific feature
                    cachedRegex = Regex(Regex.escape(reg)
                        .replace(Regex("\\\$\\{[^*]+?}"), "\\\\E(.+)\\\\Q")
                        .replace(Regex("\\$\\{\\*?}"), "\\\\E(?:.+)\\\\Q"))
                } catch (_: IllegalArgumentException) {  }
            }
        }
    }

    fun initialize() {
        Config.set(KEY_NAME, JsonArray())

        Config.onAfterLoad {
            val cachedData = Config.get<List<JsonElement>>(KEY_NAME) ?: return@onAfterLoad
            cachedData.forEach {
                createCancel(if (components.isEmpty()) 1 else 1 + (components.size % 7), it.asString)
            }
        }

        DevonianCommand.command.subcommand("cmsg") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            1
        }
        DevonianCommand.command.subcommand("cancelmsg") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            1
        }

        EventBus.on<ChatEvent> { event ->
            for (data in messagesList)
                if (data.onMessage(event))
                    event.cancel()
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

    private fun createCancel(idx: Int, message: String) {
        val data = MessageData(message)
        val yy = (11 * idx) + 1.0
        val parentBg = UIRect(0.0, yy, 100.0, 10.0, parent = main).apply {
            setColor(Color(35, 35, 35, 0))
            hide()
        }
        val cancelMsg = UITextInput(1.0, 0.0, 77.0, 100.0, message, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.message = text
                data.checkRegex()
                updateCache()
            }
        }
        val remove = UIRect(79.0, 0.0, 20.0, 100.0, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "X", true).apply { setColor(Color.RED) })
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                messagesList.remove(data)
                components.remove(parentBg)
                ChatUtils.sendMessage("&cRemoved CancelMessage &7[${data.message}]", true)
                updateCache()
                parentBg.remove()
                rebuildChildren()
            }
        }

        components.add(parentBg)
        messagesList.add(data)
        onUpdate()
    }

    private fun rebuildChildren() {
        // workaround to properly remove an element from the list and update the `y` axis of every other
        for (comp in components)
            main.children.remove(comp)
        components.clear()
        main.markDirty()

        val msgListCopy = messagesList.toMutableList()
        messagesList.clear()

        for (data in msgListCopy)
            createCancel(if (components.isEmpty()) 1 else 1 + (components.size % 7), data.message)
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
        val obj = JsonArray()

        for (data in messagesList)
            if (data.shouldTrigger())
                obj.add(data.message)

        Config.set(KEY_NAME, obj)
    }
}