package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.talium.components.UIElement
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.components.UITextInput
import com.github.synnerz.talium.constraints.UIFlexWrapConstraint
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import java.util.*

object TitleMessages : Screen(Component.literal("Devonian.TitleMessages")) {
    private const val KEY_NAME = "TitleMessages"
    private val components = mutableListOf<UIRect>()
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(30.0, 17.5, 40.0, 65.0, parent = background).apply {
        setColor(Color(25, 25, 25, 255))
    }
    private val specialTitles = mutableListOf<SpecialTitle>()
    private val selectedTitles = mutableListOf<TitleCriteria>()
    private val selectCheckbox = UIRect(1.0, 1.0, 10.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "[  ]", true).apply {
            var toggle = false
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                toggle = !toggle
                text = if (toggle) "§b[ x ]" else "[  ]"
                specialTitles.forEach { it.text.select(toggle) }
            }
        })
    }
    private val criteriaInputRect = UIRect(0.0, 1.0, 38.0, 10.0, parent = main).apply {
        xConstraint = UIFlexWrapConstraint(2.0)
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Criteria", true))
    }
    private val messageInputRect = UIRect(0.0, 1.0, 38.0, 10.0, parent = main).apply {
        xConstraint = UIFlexWrapConstraint(2.0)
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Message", true))
    }
    private val removeRect = UIRect(0.0, 1.0, 10.5, 10.0, parent = main).apply {
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
            val json = PersistentJson.gson.fromJson<Map<String, String>>(
                decoded.toString(Charsets.UTF_8),
                object : TypeToken<Map<String, String>>() {}.type
            )
            if (json.isNullOrEmpty()) return@onMouseRelease
            json.forEach { (k, v) ->
                if (specialTitles.any { it.titleCriteria.criteria == k }) return@forEach
                createCriteria(if (components.isEmpty()) 1 else 1 + (components.size % 7), k, v)
            }
            ChatUtils.sendMessage("&bImported TitleMessages from clipboard", true)
        }
    }
    private val addRect = UIRect(39.5, 89.0, 20.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§a+", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            createCriteria(if (components.isEmpty()) 1 else 1 + (components.size % 7), "p", "placeholder")
        }
    }
    private val exportRect = UIRect(60.0, 89.0, 20.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Export", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 0) return@onMouseRelease
            val json = PersistentJson.gson.toJson(buildMap { selectedTitles.forEach { put(it.criteria, it.message) } })
            if (json.isEmpty()) return@onMouseRelease

            val encoded = Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
            (minecraft ?: return@onMouseRelease).keyboardHandler.clipboard = encoded
            ChatUtils.sendMessage("&bExported TitleMessages to clipboard", true)
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
    private var exactMatch = ImmutableListMultimap.of<String, String>()
    private var regexList = emptyList<Pair<Regex, String>>()

    private fun rebuildCache() {
        val str = ArrayListMultimap.create<String, String>()
        val reg = mutableListOf<Pair<Regex, String>>()
        titleCriterias.forEach {
            if (!it.shouldTrigger()) return@forEach
            it.cachedRegex.let { r ->
                if (r == null) str.put(it.criteria, it.message)
                else reg.add(r to it.message)
            }
        }

        exactMatch = ImmutableListMultimap.copyOf(str)
        regexList = reg
    }

    data class SpecialTitle(val titleCriteria: TitleCriteria, val text: UISpecialText)
    class UISpecialText(
        _x: Double,
        _y: Double,
        _width: Double,
        _height: Double,
        text: String = "",
        centered: Boolean = false,
        parent: UIElement? = null
    ) : UIText(_x, _y, _width, _height, text, centered, parent) {
        var _onSelectHook: ((Boolean) -> Unit)? = null

        fun select(state: Boolean) {
            _onSelectHook?.invoke(state)
        }

        fun onSelect(cb: (Boolean) -> Unit) {
            _onSelectHook = cb
        }
    }
    data class TitleCriteria(var criteria: String, var message: String) {
        var cachedRegex: Regex? = null

        init {
            checkRegex()
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

            rebuildCache()
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
            exactMatch.get(event.message).forEach {
                Alert.show(it, 1500, false)
            }

            regexList.forEach { (reg, msg) ->
                val m = reg.matchEntire(event.message) ?: return@forEach
                var _msg = msg
                for (i in 1 until m.groupValues.size) {
                    _msg = _msg.replace("\${${i}}", m.groupValues[i])
                }

                Alert.show(_msg, 1500, false)
            }
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
        val selectionRect = UIRect(1.0, 1.0, 10.0, 100.0, parent = parentBg).apply {
            setColor(Color(35, 35, 35, 255))
            addChild(UISpecialText(0.0, 0.0, 100.0, 100.0, "[  ]", true).apply {
                specialTitles.add(SpecialTitle(data, this))
                var toggle = false
                onMouseRelease {
                    if (it.button != 0) return@onMouseRelease
                    toggle = !toggle
                    text = if (toggle) "§b[ x ]" else "[  ]"
                    if (toggle) selectedTitles.add(data) else selectedTitles.remove(data)
                }
                onSelect { state ->
                    toggle = state
                    text = if (toggle) "§b[ x ]" else "[  ]"
                    if (toggle) selectedTitles.add(data) else selectedTitles.remove(data)
                }
            })
        }
        val criteriaInput = UITextInput(0.0, 0.0, 38.0, 100.0, criteria, parent = parentBg).apply {
            xConstraint = UIFlexWrapConstraint(2.0)
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.criteria = text
                data.checkRegex()
                rebuildCache()
                updateCache()
            }
        }
        val messageInput = UITextInput(0.0, 0.0, 38.0, 100.0, message, parent = parentBg).apply {
            xConstraint = UIFlexWrapConstraint(2.0)
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.message = text
                rebuildCache()
                updateCache()
            }
        }
        val remove = UIRect(0.0, 0.0, 10.5, 100.0, parent = parentBg).apply {
            xConstraint = UIFlexWrapConstraint(2.0)
            setColor(Color(35, 35, 35, 255))
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "X", true).apply { setColor(Color.RED) })
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                titleCriterias.remove(data)
                components.remove(parentBg)
                specialTitles.removeIf { it.titleCriteria == data }
                ChatUtils.sendMessage("&cRemoved TitleMessage &7[${data.criteria} > ${data.message}]", true)
                rebuildCache()
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

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        background.handleCharType(characterEvent.codepoint, characterEvent.codepointAsString(), characterEvent.modifiers)
        return super.charTyped(characterEvent)
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