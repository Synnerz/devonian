package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import com.github.synnerz.devonian.utils.Toggleable
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Style

open class Feature @JvmOverloads constructor(
    val configName: String,
    description: String = "",
    val category: Categories = Categories.MISC,
    val area: String? = null,
    val subarea: String? = null,
    searchTags: Set<String> = emptySet(),
    // To avoid conflict, maybe change the position later ?
    val displayName: String = configName.camelCaseToSentence(),
    cheeto: Boolean = false,
    val isInternal: Boolean = false,
    val subcategory: String = "General",
    val isHidden: Boolean = false,
    val subcategories: List<String> = emptyList(),
) : Toggleable() {
    val minecraft = Devonian.minecraft
    val id = 256652 + Devonian.features.size
    private val style = Style.EMPTY.withClickEvent(ClickEvent.RunCommand("devonian config $id"))
    private var displayed = false
    val children = mutableListOf<Toggleable>()
    val configSwitch = addFeatureSwitch(description, displayName, searchTags, cheeto, isHidden)
        .also {
            it.onChange {
                setRegistered(it)
            }
        }

    protected open fun createRequirements(): List<BasicState<Boolean>?> = listOf(
        area?.let { Location.stateInArea(it.lowercase()) },
        subarea?.let { Location.stateInSubarea(it.lowercase()) },
    )

    init {
        Devonian.features.add(this)
    }

    override fun add() {
        children.forEach { it.register() }
    }

    override fun remove() {
        children.forEach { it.unregister() }
    }

    fun preinitialize() {
        createRequirements().reduceOrNull { a, v ->
            if (a == null) v
            else if (v == null) a
            else a.zip(v, Boolean::and)
        }?.let { setEnabled(it) }
    }

    open fun initialize() {}

    fun isEnabled(): Boolean {
        return isRegistered()
    }

    fun setEnabled() {
        configSwitch.set(true)
        setRegistered(true)
    }

    fun setDisabled() {
        configSwitch.set(false)
        setRegistered(false)
    }

    fun toggle() {
        if (isEnabled()) setDisabled()
        else setEnabled()
    }

    @JvmOverloads
    protected fun addFeatureSwitch(
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        cheeto: Boolean = false,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.FeatureSwitch {
        return ConfigData.FeatureSwitch(
            configName,
            false,
            (if (cheeto) "§4Warning: use at your own risk. " else "") + (description ?: ""),
            (if (cheeto) "§c" else "") + (displayName ?: configName.camelCaseToSentence()),
            subcategory,
            searchTags,
            isHidden,
        ).also {
            if (isInternal && configName != "hudManagerHider" && configName != "hudManagerRenderer" && configName != "hudManagerGrid") return@also
            subcategories.forEach { s -> Config.registerCategory(it, category, s) }
            Config.registerCategory(it, category, subcategory)
            Config.features.add(it)
        }
    }

    @JvmOverloads
    fun addSwitch(
        configName: String,
        value: Boolean,
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        cheeto: Boolean = false,
        subcategory: String = this.subcategory,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.Switch {
        return ConfigData.Switch(
            "${this.configName}$$configName",
            value,
            configSwitch,
            (if (cheeto) "§4Warning: use at your own risk. " else "") + (description ?: ""),
            (if (cheeto) "§c" else "") + (displayName ?: configName.camelCaseToSentence()),
            subcategory,
            searchTags,
            isHidden,
        ).also {
            Config.registerCategory(it, category, subcategory)
            configSwitch.subconfigs.add(it)
        }
    }

    @JvmOverloads
    fun addSlider(
        configName: String,
        value: Double,
        min: Double, max: Double,
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        subcategory: String = this.subcategory,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.Slider<Double> {
        return ConfigData.Slider(
            "${this.configName}$$configName",
            value,
            configSwitch,
            min, max,
            description,
            displayName,
            subcategory,
            searchTags,
            isHidden,
        ).also {
            Config.registerCategory(it, category, subcategory)
            configSwitch.subconfigs.add(it)
        }
    }

    @JvmOverloads
    fun addDecimalSlider(
        configName: String,
        value: Double,
        min: Double, max: Double,
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        subcategory: String = this.subcategory,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.DecimalSlider<Double> {
        return ConfigData.DecimalSlider(
            "${this.configName}$$configName",
            value,
            configSwitch,
            min, max,
            description,
            displayName,
            subcategory,
            searchTags,
            isHidden,
        ).also {
            Config.registerCategory(it, category, subcategory)
            configSwitch.subconfigs.add(it)
        }
    }

    @JvmOverloads
    fun addButton(
        onClick: () -> Unit,
        buttonTitle: String = "Click!",
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        subcategory: String = this.subcategory,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.Button {
        return ConfigData.Button(
            onClick,
            buttonTitle,
            configSwitch,
            description,
            displayName,
            subcategory,
            searchTags,
            isHidden,
        ).also {
            Config.registerCategory(it, category, subcategory)
            configSwitch.subconfigs.add(it)
        }
    }

    @JvmOverloads
    fun addTextInput(
        configName: String,
        value: String = "",
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        subcategory: String = this.subcategory,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.TextInput {
        return ConfigData.TextInput(
            "${this.configName}$$configName",
            value,
            configSwitch,
            description,
            displayName,
            subcategory,
            searchTags,
            isHidden,
        ).also {
            Config.registerCategory(it, category, subcategory)
            configSwitch.subconfigs.add(it)
        }
    }

    fun addSelection(
        configName: String,
        value: Int,
        options: List<String>,
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        subcategory: String = this.subcategory,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.Selection {
        return ConfigData.Selection(
            "${this.configName}$$configName",
            value,
            options,
            configSwitch,
            description,
            displayName,
            subcategory,
            searchTags,
            isHidden,
        ).also {
            Config.registerCategory(it, category, subcategory)
            configSwitch.subconfigs.add(it)
        }
    }

    fun addColorPicker(
        configName: String,
        value: Int, // argb
        description: String? = null,
        displayName: String? = null,
        searchTags: Set<String> = emptySet(),
        subcategory: String = this.subcategory,
        isHidden: Boolean = this.isHidden,
    ): ConfigData.ColorPicker {
        return ConfigData.ColorPicker(
            "${this.configName}$$configName",
            value,
            configSwitch,
            description,
            displayName,
            subcategory,
            searchTags,
            isHidden,
        ).also {
            Config.registerCategory(it, category, subcategory)
            configSwitch.subconfigs.add(it)
        }
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
                id
            )
        )
    }

    fun deleteChat() {
        ChatUtils.removeLines { ChatUtils.chatLineIds[it] == id }
        displayed = false
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit): EventListener<T> {
        val listener = EventBus.on<T>(cb, false)
        children.add(listener)
        return listener
    }

    init {
        EventBus.on<AreaEvent>(::onAreaChange)
        EventBus.on<SubAreaEvent>(::onSubAreaChange)
        EventBus.on<WorldChangeEvent>(::onWorldChange)
    }

    open fun onAreaChange(event: AreaEvent) {}

    open fun onSubAreaChange(event: SubAreaEvent) {}

    open fun onWorldChange(event: WorldChangeEvent) {}
}