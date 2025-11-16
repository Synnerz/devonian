package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.ui.Category
import com.github.synnerz.devonian.config.ui.ConfigGui
import com.github.synnerz.devonian.config.ui.ConfigType
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.talium.components.UISwitch
import net.minecraft.text.ClickEvent
import net.minecraft.text.Style

open class Feature @JvmOverloads constructor(
    val configName: String,
    description: String = "",
    category: String = "Misc",
    val area: String? = null,
    val subarea: String? = null
) {
    val minecraft = Devonian.minecraft
    val id = 256652 + Devonian.features.size
    private val style = Style.EMPTY.withClickEvent(ClickEvent.RunCommand("devonian config $id"))
    private var displayed = false
    protected var isRegistered = false
    private lateinit var _category: Category
    private var configComp: UISwitch? = null
    val events = mutableListOf<EventBus.EventListener>()

    init {
        Devonian.features.add(this)
        if (configName != "hudManagerInstructions") {
            _category = ConfigGui.category(category).addConfig(configName, description, ConfigType.SWITCH)
        }
        JsonUtils.setConfig(configName, false)
    }

    open fun initialize() {}

    fun isEnabled(): Boolean {
        return JsonUtils.getConfig(configName) ?: false
    }

    fun setEnabled() {
        JsonUtils.setConfig(configName, true)
        onToggle(true)
    }

    fun setDisabled() {
        JsonUtils.setConfig(configName, false)
        onToggle(false)
    }

    fun toggle() {
        if (!isEnabled()) return setEnabled()
        setDisabled()
    }

    fun displayChat() {
        if (displayed) deleteChat()
        ChatUtils.sendMessageWithId(
            ChatUtils.literal("&7- &b$configName ${if (isEnabled()) "&a[\uD83D\uDDF8]" else "&c[x]"}").setStyle(style),
            id
        )
        displayed = true
    }

    fun onCommand(id: Int) {
        if (id != this.id) return

        toggle()
        ChatUtils.editLines(
            { ChatUtils.chatLineIds[it] == id },
            ChatUtils.fromText(
                ChatUtils.literal("&7- &b$configName ${if (isEnabled()) "&a[\uD83D\uDDF8]" else "&c[x]"}")
                    .setStyle(style),
                id)
        )
    }

    fun deleteChat() {
        ChatUtils.removeLines { ChatUtils.chatLineIds[it] == id }
        displayed = false
    }

    open fun onToggle(state: Boolean) {
        if (configComp == null) configComp = _category.elements[configName] as UISwitch?

        configComp?.state = state

        if (!state) {
            for (event in events)
                event.remove()
            isRegistered = false
            return
        }
        if (!inEnvironment()) {
            if (isRegistered)
                for (event in events)
                    event.remove()
            isRegistered = false
            return
        }

        if (!isRegistered) {
            for (event in events)
                event.add()
        }
        isRegistered = true
    }

    fun inArea(): Boolean {
        if (area == null) return true

        return Location.area == area.lowercase()
    }

    fun inSubarea(): Boolean {
        if (subarea == null) return true

        return Location.subarea?.lowercase()?.contains(subarea.lowercase()) ?: false
    }

    fun inEnvironment(): Boolean {
        if (!inSubarea()) return false
        if (!inArea()) return false

        return true
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit) {
        events.add(EventBus.on<T>(cb, false))
    }

    init {
        on<AreaEvent>(::onAreaChange)
        on<SubAreaEvent>(::onSubAreaChange)
        on<WorldChangeEvent>(::onWorldChange)
    }

    open fun onAreaChange(event: AreaEvent) {}

    open fun onSubAreaChange(event: SubAreaEvent) {}

    open fun onWorldChange(event: WorldChangeEvent) {}
}