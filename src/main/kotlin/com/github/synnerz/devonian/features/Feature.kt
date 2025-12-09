package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ui.ConfigData
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import com.github.synnerz.devonian.utils.Toggleable
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Style

open class Feature @JvmOverloads constructor(
    val configName: String,
    description: String = "",
    val category: String = "Misc",
    area: String? = null,
    subarea: String? = null,
    // To avoid conflict, maybe change the position later ?
    displayName: String = configName.camelCaseToSentence()
) : Toggleable() {
    protected val isInternal = configName == "hudManagerInstructions" || configName == "hudManagerHider"
    val minecraft = Devonian.minecraft
    val id = 256652 + Devonian.features.size
    private val style = Style.EMPTY.withClickEvent(ClickEvent.RunCommand("devonian config $id"))
    private var displayed = false
    val children = mutableListOf<Toggleable>()
    val area = area?.lowercase()
    val subarea = subarea?.lowercase()
    private val configSwitch: ConfigData.Switch?

    init {
        Devonian.features.add(this)

        configSwitch = if (isInternal) null
        else addSwitch(configName, false, description, displayName)
            .also {
                it.onChange {
                    setRegistered(it)
                }
            }

        setEnabled((
            if (area == null) BasicState(true)
            else Location.stateInArea(area)
        ).zip(
            if (subarea == null) BasicState(true)
            else Location.stateInSubarea(subarea)
        ) { a, b -> a && b })
    }

    override fun add() {
        children.forEach { it.register() }
    }

    override fun remove() {
        children.forEach { it.unregister() }
    }

    open fun initialize() {}

    fun isEnabled(): Boolean {
        return configData.get()
    }

    fun setEnabled() {
        configSwitch?.set(true)
        onToggle(true)
    }

    fun setDisabled() {
        configSwitch?.set(false)
        onToggle(false)
    }

    fun toggle() {
        if (isEnabled()) setDisabled()
        else setEnabled()
    }

    @JvmOverloads
    fun addSwitch(
        configName: String,
        value: Boolean,
        description: String? = null,
        displayName: String? = null,
        cheeto: Boolean = false,
    ): ConfigData.Switch {
        return ConfigData.Switch(
            "${this.configName}$$configName",
            value,
            (if (cheeto) "§4Warning: use at your own risk. " else "") + (description ?: ""),
            (if (cheeto) "§c" else "") + (displayName ?: configName.camelCaseToSentence()),
        ).also { Config.categories[category]!!.add(it) }
    }

    @JvmOverloads
    fun addSlider(
        configName: String,
        value: Double,
        min: Double, max: Double,
        description: String? = null,
        displayName: String? = null,
    ): ConfigData.Slider<Double> {
        return ConfigData.Slider(
            "${this.configName}$$configName",
            value,
            min, max,
            description,
            displayName,
        ).also { Config.categories[category]!!.add(it) }
    }

    @JvmOverloads
    fun addDecimalSlider(
        configName: String,
        value: Double,
        min: Double, max: Double,
        description: String? = null,
        displayName: String? = null,
    ): ConfigData.DecimalSlider<Double> {
        return ConfigData.DecimalSlider(
            "${this.configName}$$configName",
            value,
            min, max,
            description,
            displayName,
        ).also { Config.categories[category]!!.add(it) }
    }

    @JvmOverloads
    fun addButton(
        onClick: () -> Unit,
        buttonTitle: String = "Click!",
        description: String? = null,
        displayName: String? = null,
    ): ConfigData.Button {
        return ConfigData.Button(
            onClick,
            buttonTitle,
            description,
            displayName,
        )
    }

    @JvmOverloads
    fun addTextInput(
        configName: String,
        value: String = "",
        description: String? = null,
        displayName: String? = null,
    ): ConfigData.TextInput {
        return ConfigData.TextInput(
            "${this.configName}$$configName",
            value,
            description,
            displayName,
        ).also { Config.categories[category]!!.add(it) }
    }

    fun addSelection(
        configName: String,
        value: Int,
        options: List<String>,
        description: String? = null,
        displayName: String? = null,
    ): ConfigData.Selection {
        return ConfigData.Selection(
            "${this.configName}$$configName",
            value,
            options,
            description,
            displayName,
        ).also { Config.categories[category]!!.add(it) }
    }

    fun addColorPicker(
        configName: String,
        value: Int, // argb
        description: String? = null,
        displayName: String? = null,
    ): ConfigData.ColorPicker {
        return ConfigData.ColorPicker(
            "${this.configName}$$configName",
            value,
            description,
            displayName,
        ).also { Config.categories[category]!!.add(it) }
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
        setRegistered(state)
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit) {
        children.add(EventBus.on<T>(cb, false))
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