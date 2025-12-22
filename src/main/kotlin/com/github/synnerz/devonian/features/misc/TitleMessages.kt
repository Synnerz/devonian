package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.components.UITextInput
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color

object TitleMessages : Screen(Component.literal("Devonian.TitleMessages")) {
    private const val KEY_NAME = "TitleMessages"
    private val components = mutableListOf<UIRect>()
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(30.0, 17.5, 40.0, 65.0, parent = background).apply {
        setColor(Color(25, 25, 25, 255))
    }
    private val criteriaInputRect = UIRect(1.0, 1.0, 38.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Criteria", true))
    }
    private val messageInputRect = UIRect(40.0, 1.0, 38.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Message", true))
    }
    private val removeRect = UIRect(79.0, 1.0, 20.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§c-", true))
    }
    private val addRect = UIRect(39.5, 89.0, 20.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§a+", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            createCriteria(if (components.isEmpty()) 1 else 1 + (components.size % 7), "p", "placeholder")
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
    private val titleCriterias = mutableListOf<TitleCriteria>()

    data class TitleCriteria(var criteria: String, var message: String) {
        var cachedRegex: Regex? = null

        init {
            checkRegex()
        }

        fun onMessage(event: ChatEvent) {
            if (!shouldTrigger()) return
            // TODO: make Alert#show more customizable
            // TODO: allow color codes
            if (cachedRegex != null && isRegex()) {
                val matches = event.matches(cachedRegex!!) ?: return
                val fixedMsg = buildString {
                    for (idx in 0..matches.lastIndex) {
                        val msg = message.replace("\${${idx + 1}}", matches[idx])
                        append(msg)
                    }
                }

                Alert.show(fixedMsg, 1500, false)

                return
            }
            if (event.message != criteria) return

            Alert.show(message, 1500, false)
        }

        fun shouldTrigger(): Boolean {
            if (criteria.isBlank()) return false
            if (message.isBlank()) return false
            if (message == "placeholder") return false
            return true
        }

        fun isRegex(): Boolean {
            return criteria.startsWith("/") && criteria.endsWith("/")
        }

        fun checkRegex() {
            if (!isRegex()) return

            val reg = criteria.drop(1).dropLast(1)

            try {
                // Regex taken from <https://github.com/ChatTriggers/ChatTriggers> under MIT license
                // i was lazy
                cachedRegex = Regex(Regex.escape(reg)
                    .replace(Regex("\\\$\\{[^*]+?}"), "\\\\E(.+)\\\\Q")
                    .replace(Regex("\\$\\{\\*?}"), "\\\\E(?:.+)\\\\Q"))
            } catch (_: IllegalArgumentException) { println("failed creating regex for titlemsg") }
        }
    }

    fun initialize() {
        Config.set(KEY_NAME, JsonObject())

        Config.onAfterLoad {
            val cachedData = Config.get<Map<String, JsonElement>>(KEY_NAME) ?: return@onAfterLoad
            cachedData.forEach {
                val alias = it.key
                val command = it.value.asString
                createCriteria(if (components.isEmpty()) 1 else 1 + (components.size % 7), alias, command)
            }
        }

        DevonianCommand.command.subcommand("titlemsg") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            1
        }
        DevonianCommand.command.subcommand("titlemessages") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            1
        }

        EventBus.on<ChatEvent> { event ->
            for (title in titleCriterias)
                title.onMessage(event)
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

    private fun createCriteria(idx: Int, criteria: String, message: String) {
        val data = TitleCriteria(criteria, message)
        val yy = (11 * idx) + 1.0
        val parentBg = UIRect(0.0, yy, 100.0, 10.0, parent = main).apply {
            setColor(Color(35, 35, 35, 0))
            hide()
        }
        val criteriaInput = UITextInput(1.0, 0.0, 38.0, 100.0, criteria, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.criteria = text
                data.checkRegex()
                updateCache()
            }
        }
        val messageInput = UITextInput(40.0, 0.0, 38.0, 100.0, message, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.message = text
                updateCache()
            }
        }
        val remove = UIRect(79.0, 0.0, 20.0, 100.0, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "X", true).apply { setColor(Color.RED) })
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                titleCriterias.remove(data)
                components.remove(parentBg)
                ChatUtils.sendMessage("&cRemoved TitleMessage &7[${data.criteria} > ${data.message}]", true)
                updateCache()
                parentBg.remove()
                rebuildChildren()
            }
        }

        components.add(parentBg)
        titleCriterias.add(data)
        onUpdate()
    }

    private fun rebuildChildren() {
        // workaround to properly remove an element from the list and update the `y` axis of every other
        for (comp in components)
            main.children.remove(comp)
        components.clear()
        main.markDirty()

        val titleCriteriasCopy = titleCriterias.toMutableList()
        titleCriterias.clear()

        for (data in titleCriteriasCopy)
            createCriteria(if (components.isEmpty()) 1 else 1 + (components.size % 7), data.criteria, data.message)
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

        for (titleCriteria in titleCriterias)
            obj.addProperty(titleCriteria.criteria, titleCriteria.message)

        Config.set(KEY_NAME, obj)
    }
}