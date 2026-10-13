package com.github.synnerz.devonian.hud.texthud

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes

abstract class SplitsFeature(
    configName: String,
    description: String = "",
    category: Categories = Categories.MISC,
    area: String? = null,
    subarea: String? = null,
    searchTags: Set<String> = emptySet(),
    hudConfigName: String = configName[0].uppercase() + configName.substring(1),
    displayName: String = configName.camelCaseToSentence(),
    cheeto: Boolean = false,
    isInternal: Boolean = false,
    subcategory: String = "General",
    isHidden: Boolean = false,
    subcategories: List<String> = emptyList(),
) : TextHudFeature(
    configName,
    description,
    category,
    area,
    subarea,
    searchTags,
    hudConfigName,
    displayName,
    cheeto,
    isInternal,
    subcategory,
    isHidden,
    subcategories,
) {
    private val SETTING_SEND_ALL_END = addSwitch(
        "sendAllOnEnd",
        false,
        "Sends all of the splits in chat whenever the run ends. §cIGNORES PARENT TOGGLE",
        "Send Splits In Chat",
    )
    private val SETTING_FORMAT = addSelection(
        "format",
        0,
        listOf("Real Time", "Server Ticks", "Both", "Difference"),
        "",
        "Time Format",
    )
    private val SETTING_PRECISION = addSlider(
        "precision",
        2.0,
        0.0, 2.0,
        "Number of decimal places.",
        "Time Precision",
    )

    protected var editTime = TimeUnit.EMPTY

    abstract fun getMaxName(): String

    override fun createHud(): StylizedTextHud = object : FixedWidthTextHud(configName, this@SplitsFeature) {
        override fun getMaxLine(): String = when (SETTING_FORMAT.get()) {
            0, 1 -> "${getMaxName()}: 000.00s"
            2 -> "${getMaxName()}: 000.00s (000.00s)"
            else -> "${getMaxName()}: 000.00s (+00.00s)"
        }
    }

    private fun getStyle() = TimeUnit.Style(
        TimeUnit.Format.entries[SETTING_FORMAT.get()],
        SETTING_PRECISION.get().toInt(),
    )

    protected abstract fun getSplits(style: TimeUnit.Style, force: TimeUnit?): List<String>
    protected fun getSplits(force: TimeUnit? = null) =
        getSplits(getStyle(), force)

    protected open fun getChatSplits(style: TimeUnit.Style, force: TimeUnit?) = getSplits(style, force)
    protected fun getChatSplits(force: TimeUnit? = null) =
        getChatSplits(getStyle(), force)

    open fun sendMessage() {
        // if (!isEnabled()) return
        if (!SETTING_SEND_ALL_END.get()) return
        Scheduler.scheduleServerTask(2) {
            getChatSplits().forEach {
                ChatUtils.sendMessage(it.replaceCodes())
            }
        }
    }

    override fun initialize() {
        on<ClientThreadServerTickEvent> {
            setLines(getSplits())
        }.setEnabled(SETTING_FORMAT.state.map { it != 0 })

        on<TickEvent> {
            setLines(getSplits())
        }.setEnabled(SETTING_FORMAT.state.map { it != 1 })

        on<RenderOverlayEvent> { event ->
            if (!isEditing) editTime = TimeUnit.EMPTY
            draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> {
        if (editTime.isEmpty()) editTime = TimeUnit.now()
        return getSplits(editTime)
    }
}