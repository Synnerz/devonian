package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.talium.components.*
import com.github.synnerz.talium.events.UIClickEvent
import com.github.synnerz.talium.events.UIFocusEvent
import com.github.synnerz.talium.events.UIKeyType
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonInfo
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
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
            if (it.button != 0) return@onMouseRelease
            createKeyBind(if (components.isEmpty()) 1 else 1 + (components.size % 7), "/placeholder", -1)
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
    private val bindsList = mutableListOf<ShortCut>()
    private var currentPage = 0
        set(value) {
            field = value.coerceIn(0, components.size / 7)
            onUpdate()
        }
    private var lastTrigger = -1L

    data class ShortCut(var bind: Int, var command: String) {
        fun onKeyPress(keyEvent: KeyEvent) {
            if (!shouldTrigger()) return
            if (keyEvent.key != bind) return
            ChatUtils.say(command)
        }

        fun onButtonPress(btnInfo: MouseButtonInfo) {
            if (!shouldTrigger()) return
            if (bind > 0) return
            if (-100 + btnInfo.button != bind) return
            ChatUtils.say(command)
        }

        fun shouldTrigger(): Boolean {
            if (bind == -1) return false
            if (command.isBlank()) return false
            if (command == "/placeholder") return false
            return true
        }
    }

    fun initialize() {
        Config.set(KEY_NAME, JsonObject())

        Config.onAfterLoad {
            val cachedData = Config.get<Map<String, JsonElement>>(KEY_NAME) ?: return@onAfterLoad
            cachedData.forEach {
                val keycode = it.key.toIntOrNull() ?: return@forEach
                val v = it.value.asJsonObject
                val command = v.get("command").asString
                createKeyBind(if (components.isEmpty()) 1 else 1 + (components.size % 7), command, keycode)
            }
        }

        DevonianCommand.command.subcommand("ksho") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
            }
            1
        }
        DevonianCommand.command.subcommand("keyshortcuts") { _, args ->
            Scheduler.scheduleTask {
                Devonian.minecraft.setScreen(this)
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
            }
        }
        val keybind = UIKeyBind(55.0, 0.0, 23.0, 100.0, bind, parent = bindRect).apply {
            setColor(Color(35, 35, 35, 255))
            onLostFocus {
                // TODO: add check for hotbar and movement keybinds so the player
                //  cannot accidentally bind one of these
                data.bind = this.bind
                updateCache()
            }
        }
        val remove = UIRect(79.0, 0.0, 20.0, 100.0, parent = bindRect).apply {
            setColor(Color(35, 35, 35, 255))
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "X", true).apply { setColor(Color.RED) })
            onMouseRelease {
                if (it.button != 0) return@onMouseRelease
                bindsList.remove(data)
                components.remove(bindRect)
                ChatUtils.sendMessage("&cRemoved KeyShortcut &7[${UIKeyBind.keyName(data.bind)} > ${data.command}]", true)
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

    fun onKeyPress(event: KeyEvent) {
        if (lastTrigger != -1L && System.currentTimeMillis() - lastTrigger < 200) return

        bindsList.forEach { it.onKeyPress(event) }
        lastTrigger = System.currentTimeMillis()
    }

    fun onButtonPress(btnInfo: MouseButtonInfo) {
        if (lastTrigger != -1L && System.currentTimeMillis() - lastTrigger < 200) return

        for (data in bindsList)
            data.onButtonPress(btnInfo)

        lastTrigger = System.currentTimeMillis()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        super.render(guiGraphics, mouseX, mouseY, deltaTicks)
        background.draw()
    }

    override fun renderBackground(context: GuiGraphics, mouseX: Int, mouseY: Int, deltaTicks: Float) {
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        background.handleKeyInput(keyEvent.key, keyEvent.scancode)
        if (keyEvent.isEscape && components.any {
                val keybind = it.children.find { c -> c is UIKeyBind } ?: return false
                val hadFocus = keybind.hasFocus()
                keybind.unfocus()
                hadFocus
        }) return false
        return super.keyPressed(keyEvent)
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
        if (event.keycode == GLFW.GLFW_KEY_ESCAPE) {
            bind = GLFW.GLFW_KEY_UNKNOWN
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
            val name = bySpecialKey(keycode) ?: GLFW.glfwGetKeyName(keycode, scanCode)?.uppercase()
            return "KEY ${name ?: "UNKNOWN"}"
        }
    }
}

private fun bySpecialKey(key: Int): String? =
    when (key) {
        GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT_SHIFT"
        GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT_CONTROL"
        GLFW.GLFW_KEY_LEFT_ALT -> "LEFT_ALT"
        GLFW.GLFW_KEY_LEFT_SUPER -> "LEFT_SUPER"
        GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT_SHIFT"
        GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT_CONTROL"
        GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT_ALT"
        GLFW.GLFW_KEY_RIGHT_SUPER -> "RIGHT_SUPER"
        GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS_LOCK"
        GLFW.GLFW_KEY_SCROLL_LOCK -> "SCROLL_LOCK"
        GLFW.GLFW_KEY_F1 -> "F1"
        GLFW.GLFW_KEY_F2 -> "F2"
        GLFW.GLFW_KEY_F3 -> "F3"
        GLFW.GLFW_KEY_F4 -> "F4"
        GLFW.GLFW_KEY_F5 -> "F5"
        GLFW.GLFW_KEY_F6 -> "F6"
        GLFW.GLFW_KEY_F7 -> "F7"
        GLFW.GLFW_KEY_F8 -> "F8"
        GLFW.GLFW_KEY_F9 -> "F9"
        GLFW.GLFW_KEY_F10 -> "F10"
        GLFW.GLFW_KEY_F11 -> "F11"
        GLFW.GLFW_KEY_F12 -> "F12"
        GLFW.GLFW_KEY_F13 -> "F13"
        GLFW.GLFW_KEY_F14 -> "F14"
        GLFW.GLFW_KEY_F15 -> "F15"
        GLFW.GLFW_KEY_F16 -> "F16"
        GLFW.GLFW_KEY_F17 -> "F17"
        GLFW.GLFW_KEY_F18 -> "F18"
        GLFW.GLFW_KEY_F19 -> "F19"
        GLFW.GLFW_KEY_F20 -> "F20"
        GLFW.GLFW_KEY_F21 -> "F21"
        GLFW.GLFW_KEY_F22 -> "F22"
        GLFW.GLFW_KEY_F23 -> "F23"
        GLFW.GLFW_KEY_F24 -> "F24"
        GLFW.GLFW_KEY_F25 -> "F25"
        GLFW.GLFW_KEY_UP -> "UP"
        GLFW.GLFW_KEY_DOWN -> "DOWN"
        GLFW.GLFW_KEY_LEFT -> "LEFT"
        GLFW.GLFW_KEY_RIGHT -> "RIGHT"
        GLFW.GLFW_KEY_ESCAPE -> "ESCAPE"
        GLFW.GLFW_KEY_SPACE -> "SPACE"
        GLFW.GLFW_KEY_ENTER -> "ENTER"
        GLFW.GLFW_KEY_TAB  -> "TAB"
        GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE"
        GLFW.GLFW_KEY_INSERT -> "INSERT"
        GLFW.GLFW_KEY_DELETE -> "DELETE"
        GLFW.GLFW_KEY_HOME -> "HOME"
        GLFW.GLFW_KEY_END -> "END"
        GLFW.GLFW_KEY_PAGE_UP -> "PAGE_UP"
        GLFW.GLFW_KEY_PAGE_DOWN -> "PAGE_DOWN"
        GLFW.GLFW_KEY_PRINT_SCREEN -> "PRINT_SCREEN"
        GLFW.GLFW_KEY_PAUSE -> "PAUSE"
        GLFW.GLFW_KEY_MENU -> "MENU"
        GLFW.GLFW_KEY_KP_ENTER -> "KP_ENTER"
        GLFW.GLFW_KEY_KP_EQUAL -> "KP_EQUAL"
        GLFW.GLFW_KEY_KP_ADD -> "KP_ADD"
        GLFW.GLFW_KEY_KP_SUBTRACT -> "KP_SUBTRACT"
        GLFW.GLFW_KEY_KP_MULTIPLY -> "KP_MULTIPLY"
        GLFW.GLFW_KEY_KP_DIVIDE -> "KP_DIVIDE"
        GLFW.GLFW_KEY_KP_DECIMAL -> "KP_DOT" // probably not good for consistency but idc
        GLFW.GLFW_KEY_UNKNOWN -> "UNKNOWN"
        GLFW.GLFW_KEY_WORLD_1 -> "WORLD_1"
        GLFW.GLFW_KEY_WORLD_2 -> "WORLD_2"
        else -> null
    }
