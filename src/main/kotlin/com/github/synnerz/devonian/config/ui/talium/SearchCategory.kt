package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.talium.components.*
import kotlin.math.max

class SearchCategory(rightPanel: UIBase) : SharedCategory("Searching...") {
    private val components = mutableListOf<Pair<ConfigData<*>, UIElement>>()
    private var previousText = ""
    private var oldCategory: Category? = null
    private val categoryTitleBg = UIRect(0.0, 0.0, 100.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(categoryTitle)
    }
    private val contentContainer = UIScrollable(0.0, 9.0, 100.0, 81.0, parent = rightPanel)

    init {
        UITextInput(23.75, 92.0, 52.5, 8.0, parent = rightPanel).apply {
            setColor(ColorPalette.TERTIARY_COLOR)

            onKeyType {
                // Unhide
                if (previousText.isEmpty() && text.isNotEmpty()) {
                    oldCategory = ConfigGui.selectedCategory
                    oldCategory?.hide()
                    ConfigGui.selectedCategory = null
                    this@SearchCategory.unhide()
                }
                // Hide
                else if (previousText.isNotEmpty() && text.isEmpty()) {
                    hideColorPickers()
                    this@SearchCategory.hide()

                    if (oldCategory == null && ConfigGui.selectedCategory == null) {
                        ConfigGui.selectedCategory = ConfigGui.categories.first()
                        ConfigGui.selectedCategory?.unhide()
                    } else if (ConfigGui.selectedCategory == null)
                        oldCategory?.unhide()

                    oldCategory = null
                    previousText = ""
                    return@onKeyType
                }
                if (previousText !== text)
                    onSearch(text)
                previousText = text
            }

            onResize { _, w ->
                textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
            }
        }
        UIRect(72.25, 92.0, 4.0, 8.0, parent = rightPanel).apply {
            setColor(ColorPalette.TERTIARY_COLOR)
            addChild(UIText(0.0, 0.0, 100.0, 100.0, "\uD83D\uDD0E", true).apply {
                onResize { _, w ->
                    textScale = NORMAL_TEXT_SCALE / w.scaleFactor
                }
            })
        }
        UIText(
            75.0, 92.0,
            25.0, 8.0,
            parent = rightPanel,
            text = "(hold ctrl to scroll fast)",
            centered = true,
        ).apply {
            setColor(ColorPalette.LIGHT_TEXT_COLOR)
            onResize { _, w ->
                textScale = 1.5f / w.scaleFactor
            }
        }

        fun buildComp(data: ConfigData<*>) {
            if (data.isHidden && !Devonian.isDev) return

            val elem = createComp(data, contentContainer)
            components.add(data to elem.container)
        }

        Config.features.forEach {
            buildComp(it)
            it.subconfigs.forEach {
                buildComp(it)
            }
        }

        hide()
    }

    private fun onSearch(str: String) {
        val subConfigHits = Config.features.associateWith {
            it.subconfigs.associateWith {
                matchesConfig(it, str)
            }
        }
        val featHits = Config.features.associateWith {
            matchesConfig(it, str)
        }
        val featMaxHits = subConfigHits.entries.associate { (feat, sub) ->
            feat to max(featHits[feat] ?: 0, sub.entries.maxOfOrNull { it.value } ?: 0)
        }
        val hitCount = mutableListOf<Int>()
        val featOrdering = featMaxHits.entries.associate { (key, maxHits) ->
            while (maxHits >= hitCount.size) hitCount.add(0)
            key to (maxHits * 1000 - hitCount[maxHits]++)
        }

        var idx = 0
        components.sortedBy {
            -1000 * featOrdering[if (it.first is ConfigData.FeatureSwitch) it.first else it.first.parent]!! +
            (if (it.first is ConfigData.FeatureSwitch) 0 else 500 - (subConfigHits[it.first.parent]?.get(it.first) ?: 0))
        }.forEach { (data, comp) ->
            val show = if (data is ConfigData.FeatureSwitch) {
                featMaxHits[data]!! > 0
            } else {
                featHits[data.parent]!! > 0 ||
                subConfigHits[data.parent]!![data]!! > 0
            }
            if (show) {
                val i = idx++
                val y = 1.0 + i * 17.0
                comp._y = y
                comp.markDirty()
                comp.unhide()
                comp.update()
            } else comp.hide()
        }

        contentContainer.updateScrollY(0.0)
        contentContainer.yOffset = 0.0
        categoryTitle.text = "Searching \"$str\""
    }

    private fun matchesConfig(config: ConfigData<*>, str: String): Int {
        val tags = str.split(' ').map { it.lowercase().replace(ConfigData.searchStripReg, "") }
        return tags.count { s -> config.searchTags.any { it.contains(s) } }
    }

    override fun hide() {
        super.hide()
        components.forEach { it.second.hide() }
        categoryTitleBg.hide()
        categoryTitle.text = "Searching..."
        previousText = ""
        oldCategory = null
    }

    override fun unhide() {
        super.unhide()
        categoryTitleBg.unhide()
        categoryTitle.text = "Searching..."
        oldCategory = null
    }
}