package com.github.synnerz.devonian.features.misc.chat

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.config.ui.talium.UISpecialText
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.components.UITextInput
import com.github.synnerz.talium.constraints.UIFlexWrapConstraint
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import java.util.*

object CancelMessages : Screen(Component.literal("Devonian.CancelMessages")) {
    private const val KEY_NAME = "CancelMessages"
    private val components = mutableListOf<UIRect>()
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(30.0, 17.5, 40.0, 65.0, parent = background).apply {
        setColor(Color(25, 25, 25, 255))
    }
    private val messageSelection = mutableListOf<MessageSelection>()
    private val selectedMessages = mutableListOf<MessageData>()
    private val selectCheckbox = UIRect(1.0, 1.0, 10.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "[  ]", true).apply {
            var toggle = false
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                toggle = !toggle
                text = if (toggle) "§b[ x ]" else "[  ]"
                messageSelection.forEach { it.text.select(toggle) }
            }
        })
    }
    private val cancelMsgInput = UIRect(1.0, 1.0, 77.0, 10.0, parent = main).apply {
        xConstraint = UIFlexWrapConstraint(2.0)
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Message", true))
    }
    private val removeRect = UIRect(79.0, 1.0, 10.5, 10.0, parent = main).apply {
        xConstraint = UIFlexWrapConstraint(2.0)
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§c-", true))
    }
    private val importRect = UIRect(19.0, 89.0, 20.0, 10.0, parent = main).apply {
        // can't use constraint here ): because the left arrow is hidden sometimes
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Import", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            val encode = minecraft?.keyboardHandler?.clipboard
            if (encode.isNullOrEmpty()) return@onMouseRelease
            val decoded = Base64.getDecoder().decode(encode)
            val json = PersistentJson.gson.fromJson<List<String>>(
                decoded.toString(Charsets.UTF_8),
                object : TypeToken<List<String>>() {}.type
            )
            if (json.isNullOrEmpty()) return@onMouseRelease
            json.forEach { v ->
                if (messageSelection.any { it.data.message == v }) return@forEach
                createCancel(if (components.isEmpty()) 1 else 1 + (components.size % 7), v, true)
            }
            updateCache()
            rebuildCache()
            ChatUtils.sendMessage("&bImported CancelMessage from clipboard", true)
        }
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
    private val exportRect = UIRect(60.0, 89.0, 20.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Export", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            val json = PersistentJson.gson.toJson(selectedMessages.map { it.message })
            if (json.isEmpty()) return@onMouseRelease

            val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
            (minecraft ?: return@onMouseRelease).keyboardHandler.clipboard = encoded
            ChatUtils.sendMessage("&bExported CancelMessage to clipboard", true)
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
    private var exactMatch = emptySet<String>()
    private var regexList = emptyList<Regex>()

    private fun rebuildCache() {
        val str = mutableSetOf<String>()
        val reg = mutableListOf<Regex>()
        messagesList.forEach {
            if (!it.shouldTrigger()) return@forEach
            it.cachedRegex.let { r ->
                if (r == null) str.add(it.message)
                else reg.add(r)
            }
        }

        exactMatch = str
        regexList = reg
    }

    data class MessageSelection(val data: MessageData, val text: UISpecialText)
    data class MessageData(var message: String) {
        var cachedRegex: Regex? = null

        init {
            checkRegex()
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
            if (!isRegex()) return
            val reg = message.drop(1).dropLast(1)
            try {
                // Regex taken from <https://github.com/ChatTriggers/ChatTriggers> under MIT license
                // i wasn't lazy this simply works better for this specific feature
                cachedRegex = Regex(Regex.escape(reg)
                    .replace(Regex("\\\$\\{[^*]+?}"), "\\\\E(.*)\\\\Q")
                    .replace(Regex("\\$\\{\\*?}"), "\\\\E(?:.*)\\\\Q")
                    .replace(Regex("\\\$\\{[^+]+?}"), "\\\\E(.+)\\\\Q")
                    .replace(Regex("\\$\\{\\+?}"), "\\\\E(?:.+)\\\\Q"))
            } catch (_: IllegalArgumentException) {
                println("Devonian\$CancelMessage(IllegalArgumentException, $reg, $message)")
            }
        }
    }

    fun initialize() {
        ConfigData.Button(
            {
                Scheduler.scheduleTask {
                    Devonian.minecraft.setScreenAndShow(this)
                }
            },
            "Run",
            null,
            "Opens a gui where you can add messages that you want to be hidden from chat (/dv cmsg)",
            "Cancel Messages",
        ).also {
            Config.registerCategory(it, Categories.GLOBAL, "Commands")
        }

        Config.set(KEY_NAME, JsonArray())

        Config.onAfterLoad {
            val cachedData = Config.get<List<JsonElement>>(KEY_NAME) ?: return@onAfterLoad
            cachedData.forEach {
                createCancel(if (components.isEmpty()) 1 else 1 + (components.size % 7), it.asString)
            }

            rebuildCache()
        }

        DevonianCommand.command.subcommand("cmsg") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreenAndShow(this)
            }
            1
        }
        DevonianCommand.command.subcommand("cancelmsg") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreenAndShow(this)
            }
            1
        }

        EventBus.on<ChatEvent> { event ->
            if (exactMatch.contains(event.message)) event.cancel()
            else if (regexList.any { it.matches(event.message) }) event.cancel()
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

    private fun createCancel(idx: Int, message: String, imported: Boolean = false) {
        val data = MessageData(message)
        val yy = (11 * idx) + 1.0
        val parentBg = UIRect(0.0, yy, 100.0, 10.0, parent = main).apply {
            setColor(Color(35, 35, 35, 0))
            hide()
        }
        val selectionRect = UIRect(1.0, 1.0, 10.0, 100.0, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            addChild(UISpecialText(0.0, 0.0, 100.0, 100.0, "[  ]", true).apply {
                messageSelection.add(MessageSelection(data, this))
                var toggle = false
                onMouseRelease {
                    if (it.button != 0) return@onMouseRelease
                    toggle = !toggle
                    text = if (toggle) "§b[ x ]" else "[  ]"
                    if (toggle) selectedMessages.add(data) else selectedMessages.remove(data)
                }
                onSelect { state ->
                    toggle = state
                    text = if (toggle) "§b[ x ]" else "[  ]"
                    if (toggle) selectedMessages.add(data) else selectedMessages.remove(data)
                }
            })
        }
        val cancelMsg = UITextInput(0.0, 0.0, 77.0, 100.0, message, parent = parentBg).apply {
            xConstraint = UIFlexWrapConstraint(2.0)
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.message = text
                data.checkRegex()
                updateCache()
                rebuildCache()
            }
        }
        val remove = UIRect(0.0, 0.0, 10.5, 100.0, parent = parentBg).apply {
            xConstraint = UIFlexWrapConstraint(2.0)
            setColor(Color(35, 35, 35, 255))
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "X", true).apply { setColor(Color.RED) })
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                messagesList.remove(data)
                components.remove(parentBg)
                messageSelection.removeIf { it.data == data }
                ChatUtils.sendMessage("&cRemoved CancelMessage &7[${data.message}]", true)
                updateCache()
                rebuildCache()
                parentBg.remove()
                rebuildChildren()
            }
        }

        components.add(parentBg)
        messagesList.add(data)
        if (imported) data.checkRegex()
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

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        background.draw()
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.shortcutKey())
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        background.handleCharType(characterEvent.codepoint, characterEvent.codepointAsString(), -1)
        return super.charTyped(characterEvent)
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