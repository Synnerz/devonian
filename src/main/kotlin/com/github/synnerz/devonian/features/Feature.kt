package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.ui.Category
import com.github.synnerz.devonian.config.ui.ConfigData
import com.github.synnerz.devonian.config.ui.ConfigGui
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.talium.components.UISwitch
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Style

open class Feature @JvmOverloads constructor(
    val configName: String,
    description: String = "",
    category: String = "Misc",
    val area: String? = null,
    val subarea: String? = null,
    // To avoid conflict, maybe change the position later ?
    displayName: String = configName
) {
    val minecraft = Devonian.minecraft
    val id = 256652 + Devonian.features.size
    private val style = Style.EMPTY.withClickEvent(ClickEvent.RunCommand("devonian config $id"))
    private var displayed = false
    protected var isRegistered = false
    protected var _category: Category
    private var configComp: UISwitch? = null
    private val configData = ConfigData.FeatureSwitch(configName, false, this)
    val events = mutableListOf<EventBus.EventListener>()

    init {
        Devonian.features.add(this)
        _category = ConfigGui.category(category)

        if (configName != "hudManagerInstructions")
            _category.addSwitch(displayName, description, configData)
    }

    open fun initialize() {}

    fun isEnabled(): Boolean {
        return configData.get()
    }

    fun setEnabled() {
        configData.set(true)
        onToggle(true)
    }

    fun setDisabled() {
        configData.set(false)
        onToggle(false)
    }

    fun config(): ConfigData.Switch = configData

    fun toggle() {
        if (!isEnabled()) return setEnabled()
        setDisabled()
    }

    @JvmOverloads
    fun addSwitch(
        configName: String,
        description: String,
        displayName: String = configName,
        value: Boolean = false,
        cheeto: Boolean = false
    ): ConfigData.Switch {
        return _category.addSwitch(
            (if (cheeto) "§c" else "") + displayName,
            (if (cheeto) "§4Warning: use at your own risk. " else "") + description,
            ConfigData.Switch("${this.configName}$${configName}", value)
        )
    }

    @JvmOverloads
    fun addSlider(
        configName: String,
        description: String,
        displayName: String = configName,
        min: Double = 0.0, max: Double = 100.0,
        value: Double = min
    ): ConfigData.Slider<Double> {
        return _category.addSlider(
            displayName,
            description,
            ConfigData.Slider("${this.configName}$${configName}", value, min, max)
        )
    }

    @JvmOverloads
    fun addButton(
        displayName: String,
        description: String,
        buttonTitle: String = "Click!",
        onClick: () -> Unit
    ) {
        _category.addButton(displayName, description, buttonTitle, onClick)
    }

    @JvmOverloads
    fun addTextInput(
        configName: String,
        description: String,
        displayName: String = configName,
        value: String = ""
    ): ConfigData.TextInput {
        return _category.addTextInput(
            displayName,
            description,
            ConfigData.TextInput("${this.configName}$${configName}", value)
        )
    }

    fun addSelection(
        configName: String,
        description: String,
        displayName: String = configName,
        options: List<String>,
        value: Int = 0
    ): ConfigData.Selection {
        return _category.addSelection(
            displayName,
            description,
            ConfigData.Selection("${this.configName}$${configName}", value, options)
        )
    }

    fun addColorPicker(
        configName: String,
        description: String,
        displayName: String = configName,
        value: Int = -1 // argb
    ): ConfigData.ColorPicker {
        return _category.addColorPicker(
            displayName,
            description,
            ConfigData.ColorPicker(configName, value)
        )
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