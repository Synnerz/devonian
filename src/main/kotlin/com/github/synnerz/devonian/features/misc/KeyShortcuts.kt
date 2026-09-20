package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.KeyPressEvent
import com.github.synnerz.devonian.api.events.MousePressEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.talium.components.*
import com.github.synnerz.talium.events.UIClickEvent
import com.github.synnerz.talium.events.UIFocusEvent
import com.github.synnerz.talium.events.UIKeyType
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ImmutableListMultimap
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.chat.Component
import java.awt.Color
import kotlin.math.abs

object KeyShortcuts : Screen(Component.literal("Devonian.KeyShortcuts")) {
    private const val KEY_NAME = "KeyShortcuts"
    private val components = mutableListOf<UIRect>()
    private val background = UIRect(0.0, 0.0, 100.0, 100.0)
    private val main = UIRect(30.0, 17.5, 40.0, 65.0, parent = background).apply {
        setColor(Color(25, 25, 25, 255))
    }
    private val inputRect = UIRect(1.0, 1.0, 53.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "Message", true))
    }
    private val keyRect = UIRect(55.0, 1.0, 23.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "KeyBind", true))
    }
    private val removeRect = UIRect(79.0, 1.0, 20.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§c-", true))
    }
    // TODO: add some kind of page display so the user knows what page they're in as well as how many are left
    private val addRect = UIRect(39.5, 89.0, 20.0, 10.0, parent = main).apply {
        // TODO: if the current page is full, add it to the next page and switch pages for the user
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "§a+", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 1) return@onMouseRelease
            createKeyBind(if (components.isEmpty()) 1 else 1 + (components.size % 7), "/placeholder", -1)
        }
    }
    private val leftArrow = UIRect(1.0, 89.0, 10.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "<-", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 1) return@onMouseRelease
            currentPage--
        }
        hide()
    }
    private val rightArrow = UIRect(89.0, 89.0, 10.0, 10.0, parent = main).apply {
        setColor(Color(50, 50, 50, 255))
        addChild(UIText(0.0, 0.0, 100.0, 100.0, "->", true).apply { textScale = 1.5f })
        onMouseRelease {
            if (it.button != 1) return@onMouseRelease
            currentPage++
        }
    }
    private var currentPage = 0
        set(value) {
            field = value.coerceIn(0, components.size / 7)
            onUpdate()
        }

    private val bindsList = mutableListOf<ShortCut>()
    private var keyCache = ImmutableListMultimap.of<Int, String>()

    private fun rebuildCache() {
        val cache = ArrayListMultimap.create<Int, String>()
        bindsList.forEach {
            if (!it.shouldTrigger()) return@forEach
            cache.put(it.bind, it.command)
        }

        keyCache = ImmutableListMultimap.copyOf(cache)
    }

    data class ShortCut(var bind: Int, var command: String) {
        fun shouldTrigger(): Boolean {
            if (bind == -1) return false
            if (command.isBlank()) return false
            if (command == "/placeholder") return false
            return true
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
            "Opens a gui where you can add your own keybind shortcuts (/dv ksho)",
            "Key Shortcuts",
        ).also {
            Config.registerCategory(it, Categories.GLOBAL, "Commands")
        }

        Config.set(KEY_NAME, JsonObject())

        Config.onAfterLoad {
            val cachedData = Config.get<Map<String, JsonElement>>(KEY_NAME) ?: return@onAfterLoad
            cachedData.forEach {
                val keycode = it.key.toIntOrNull() ?: return@forEach
                val v = it.value.asJsonObject
                val command = v.get("command").asString
                createKeyBind(if (components.isEmpty()) 1 else 1 + (components.size % 7), command, keycode)
            }

            rebuildCache()
        }

        EventBus.on<KeyPressEvent> { event ->
            triggerBind(event.key)
        }

        EventBus.on<MousePressEvent> { event ->
            triggerBind(-100 + event.button)
        }

        DevonianCommand.command.subcommand("ksho") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreenAndShow(this)
            }
            1
        }
        DevonianCommand.command.subcommand("keyshortcuts") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreenAndShow(this)
            }
            1
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

    private fun createKeyBind(idx: Int, command: String, bind: Int) {
        val data = ShortCut(bind, command)
        val yy = (11 * idx) + 1.0
        val bindRect = UIRect(0.0, yy, 100.0, 10.0, parent = main).apply {
            setColor(Color(35, 35, 35, 0))
            hide()
        }
        val input = UITextInput(1.0, 0.0, 53.0, 100.0, command, parent = bindRect).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                data.command = text
                updateCache()
                rebuildCache()
            }
        }
        val keybind = UIKeyBind(55.0, 0.0, 23.0, 100.0, bind, parent = bindRect).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                // TODO: add check for hotbar and movement keybinds so the player
                //  cannot accidentally bind one of these
                data.bind = this.bind
                updateCache()
                rebuildCache()
            }
        }
        val remove = UIRect(79.0, 0.0, 20.0, 100.0, parent = bindRect).apply {
            setColor(Color(35, 35, 35, 255))
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "X", true).apply { setColor(Color.RED) })
            onMouseRelease {
                if (it.button != 1) return@onMouseRelease
                bindsList.remove(data)
                components.remove(bindRect)
                ChatUtils.sendMessage("&cRemoved KeyShortcut &7[${UIKeyBind.keyName(data.bind)} > ${data.command}]", true)
                rebuildCache()
                updateCache()
                bindRect.remove()
                rebuildChildren()
            }
        }
        components.add(bindRect)
        bindsList.add(data)
        onUpdate()
    }

    private fun rebuildChildren() {
        for (comp in components)
            main.children.remove(comp)
        components.clear()
        main.markDirty()

        val bindsListCopy = bindsList.toMutableList()
        bindsList.clear()

        for (data in bindsListCopy)
            createKeyBind(if (components.isEmpty()) 1 else 1 + (components.size % 7), data.command, data.bind)
    }

    private fun triggerBind(bind: Int) {
        keyCache.get(bind).forEach {
            ChatUtils.say(it)
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, a)
        background.draw()
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.shortcutKey())
        if (keyEvent.isEscape && components.any {
                val keybind = it.children.find { c -> c is UIKeyBind } ?: return false
                val hadFocus = keybind.hasFocus()
                keybind.unfocus()
                hadFocus
        }) return false
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
        val obj = JsonObject()

        bindsList.forEach {
            if (!it.shouldTrigger()) return@forEach
            val obj2 = JsonObject()

            obj2.addProperty("command", it.command)
            obj.add("${it.bind}", obj2)
        }

        Config.set(KEY_NAME, obj)
    }
}

class UIKeyBind(
    _x: Double,
    _y: Double,
    _width: Double,
    _height: Double,
    var bind: Int = -1,
    parent: UIBase? = null
) : UIBase(_x, _y, _width, _height, parent) {
    val keyNameText by lazy { UIWrappedText(0.0, 0.0, 100.0, 100.0, "UNKNOWN", true, this@UIKeyBind) }
    var isEnabled = false

    override fun render() {
        UIRect.drawRect(x, y, width, height, color = bgColor)
    }

    override fun onUpdate() = apply {
        // Workaround to delegate the key name getter after mc has loaded
        // otherwise glfw has not been loaded properly and a crash occurs
        keyNameText.text = keyName(bind)
    }

    override fun onMouseRelease(event: UIClickEvent) = apply {
        if (!focused) return@apply
        if (!isEnabled) {
            isEnabled = true
            return@apply
        }

        bind = -100 + event.button
        keyNameText.text = keyName(bind)
        unfocus()
        isEnabled = false
    }

    override fun onKeyType(event: UIKeyType) = apply {
        if (!focused) return@apply
        if (event.keycode == InputConstants.KEY_ESCAPE) {
            bind = InputConstants.UNKNOWN.value
            keyNameText.text = keyName(bind)
            return@apply
        }

        bind = event.keycode
        keyNameText.text = keyName(event.keycode)
        unfocus()
    }

    override fun onFocus(event: UIFocusEvent) = apply {
        keyNameText.text = "****"
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun keyName(keycode: Int, scanCode: Int = 0): String {
            if (keycode < -1) return "M${abs(-100 % keycode)}"
            if (keycode == -1) return "UNKNOWN"
            val name = bySpecialKey(keycode) ?: InputConstants.Type.KEYBOARD.getOrCreate(keycode).displayName.string
            return "KEY ${name.replace("Keypad ", "KP")}"
        }
    }
}

private fun bySpecialKey(key: Int): String? =
    when (key) {
        InputConstants.KEY_LSHIFT -> "LEFT_SHIFT"
        InputConstants.KEY_LCONTROL -> "LEFT_CONTROL"
        InputConstants.KEY_LALT -> "LEFT_ALT"
        InputConstants.KEY_LGUI -> "LEFT_SUPER"
        InputConstants.KEY_RSHIFT -> "RIGHT_SHIFT"
        InputConstants.KEY_RCONTROL -> "RIGHT_CONTROL"
        InputConstants.KEY_RALT -> "RIGHT_ALT"
        InputConstants.KEY_RGUI -> "RIGHT_SUPER"
        InputConstants.KEY_CAPSLOCK -> "CAPS_LOCK"
        InputConstants.KEY_SCROLLLOCK -> "SCROLL_LOCK"
        InputConstants.KEY_F1 -> "F1"
        InputConstants.KEY_F2 -> "F2"
        InputConstants.KEY_F3 -> "F3"
        InputConstants.KEY_F4 -> "F4"
        InputConstants.KEY_F5 -> "F5"
        InputConstants.KEY_F6 -> "F6"
        InputConstants.KEY_F7 -> "F7"
        InputConstants.KEY_F8 -> "F8"
        InputConstants.KEY_F9 -> "F9"
        InputConstants.KEY_F10 -> "F10"
        InputConstants.KEY_F11 -> "F11"
        InputConstants.KEY_F12 -> "F12"
        InputConstants.KEY_F13 -> "F13"
        InputConstants.KEY_F14 -> "F14"
        InputConstants.KEY_F15 -> "F15"
        InputConstants.KEY_F16 -> "F16"
        InputConstants.KEY_F17 -> "F17"
        InputConstants.KEY_F18 -> "F18"
        InputConstants.KEY_F19 -> "F19"
        InputConstants.KEY_F20 -> "F20"
        InputConstants.KEY_F21 -> "F21"
        InputConstants.KEY_F22 -> "F22"
        InputConstants.KEY_F23 -> "F23"
        InputConstants.KEY_F24 -> "F24"
//        SDLKeycode.SDLK_F25 -> "F25"
        InputConstants.KEY_UP -> "UP"
        InputConstants.KEY_DOWN -> "DOWN"
        InputConstants.KEY_LEFT -> "LEFT"
        InputConstants.KEY_RIGHT -> "RIGHT"
        InputConstants.KEY_ESCAPE -> "ESCAPE"
        InputConstants.KEY_SPACE -> "SPACE"
        InputConstants.KEY_RETURN -> "ENTER"
        InputConstants.KEY_TAB  -> "TAB"
        InputConstants.KEY_BACKSPACE -> "BACKSPACE"
        InputConstants.KEY_INSERT -> "INSERT"
        InputConstants.KEY_DELETE -> "DELETE"
        InputConstants.KEY_HOME -> "HOME"
        InputConstants.KEY_END -> "END"
        InputConstants.KEY_PAGEUP -> "PAGE_UP"
        InputConstants.KEY_PAGEDOWN -> "PAGE_DOWN"
        InputConstants.KEY_PRINTSCREEN -> "PRINT_SCREEN"
        InputConstants.KEY_PAUSE -> "PAUSE"
//        InputConstants.SDLK_MENU -> "MENU"
        InputConstants.KEY_NUMPADENTER -> "KP_ENTER"
        InputConstants.KEY_NUMPADEQUALS -> "KP_EQUAL"
        InputConstants.KEY_ADD -> "KP_ADD"
        InputConstants.KEY_MINUS -> "KP_SUBTRACT"
        InputConstants.KEY_MULTIPLY -> "KP_MULTIPLY"
        InputConstants.KEY_SLASH -> "KP_DIVIDE"
        InputConstants.KEY_PERIOD -> "KP_DOT" // probably not good for consistency but idc
        InputConstants.UNKNOWN.value -> "UNKNOWN"
//        GLFW.GLFW_KEY_WORLD_1 -> "WORLD_1"
//        GLFW.GLFW_KEY_WORLD_2 -> "WORLD_2"
        else -> null
    }
