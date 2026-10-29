package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.talium.components.*
import kotlin.math.max

class SearchCategory(rightPanel: UIBase) : SharedCategory("Searching...") {
    private val components: List<Pair<ConfigData<*>, UIElement>>
    private var currSort: List<Int> = emptyList()
    private var previousText = ""
    private var oldCategory: Category? = null
    private val categoryTitleBg = UIRect(0.0, 0.0, 100.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
        addChild(categoryTitle)
    }
    private val contentContainer = UIScrollable(0.0, 9.0, 100.0, 81.0, parent = rightPanel)

    val searchbar = UITextInput(23.75, 92.0, 52.5, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)

        onCharType {
            // Unhide
            if (previousText.isEmpty() && text.isNotEmpty()) {
                oldCategory = ConfigGui.selectedCategory
                oldCategory?.hide()
                ConfigGui.selectedCategory = null
                this@SearchCategory.unhide()
            }
            // Hide
            if (text.isEmpty()) {
                if (previousText.isNotEmpty()) {
                    this@SearchCategory.hide()

                    // if (ConfigGui.selectedCategory == null)
                    //     oldCategory?.unhide()

                    oldCategory = null
                    previousText = ""
                }
                return@onCharType
            }
            if (previousText != text)
                onSearch(text)
            previousText = text
        }

        onResize { _, w ->
            textScale = DESCRIPTION_TEXT_SCALE / w.scaleFactor
        }
    }

    init {
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

        val comps = mutableListOf<Pair<ConfigData<*>, UIElement>>()
        fun buildComp(data: ConfigData<*>) {
            if (!data.shouldExist()) return

            val elem = createComp(data, contentContainer)
            comps.add(data to elem.container)
        }

        Config.features.forEach {
            buildComp(it)
            it.subconfigs.forEach {
                buildComp(it)
            }
        }
        components = comps

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
            feat to max((featHits[feat] ?: 0) * 2, sub.entries.maxOfOrNull { it.value } ?: 0)
        }
        val hitCount = mutableListOf<Int>()
        val featOrdering = featMaxHits.entries.associate { (key, maxHits) ->
            while (maxHits >= hitCount.size) hitCount.add(0)
            key to (maxHits * 1000 - hitCount[maxHits]++)
        }

        currSort = components.withIndex().sortedBy { (_, p) ->
            val (data) = p
            -1000 * featOrdering[data as? ConfigData.FeatureSwitch ?: data.parent]!! +
            (if (data is ConfigData.FeatureSwitch) 0 else 500 - (subConfigHits[data.parent]?.get(data) ?: 0))
        }.filter { (_, p) ->
            val (data) = p
            if (data is ConfigData.FeatureSwitch) {
                featMaxHits[data]!! > 0
            } else {
                featHits[data.parent]!! > 0 ||
                subConfigHits[data.parent]!![data]!! > 0
            }
        }.map { it.index }

        rebuildComps()

        contentContainer.yOffset = 0.0
        contentContainer.updateScrollY(0.0)

        categoryTitle.text = "Searching \"$str\""
    }

    override fun rebuildComps() {
        val visible = BooleanArray(components.size)
        var idx = 0
        currSort.forEach {
            val (data, comp) = components[it]
            if (!data.shouldShow()) return@forEach

            visible[it] = true

            val i = idx++
            val y = 1.0 + i * 17.0
            comp.unhide()
            comp._y = y
            comp.markDirty()
            comp.checkUpdate()
        }

        visible.forEachIndexed { i, v ->
            if (v) return@forEachIndexed
            components[i].second.hide()
            components[i].second._y = 0.0
            components[i].second.markDirty()
            components[i].second.checkUpdate()
        }

        contentContainer.updateScrollY(0.0)
        contentContainer.markDirty()
        contentContainer.checkUpdate()
    }

    private fun matchesConfig(config: ConfigData<*>, str: String): Int {
        val tags = str.split(' ').map { it.lowercase().replace(ConfigData.searchStripReg, "") }
        return tags.sumOf { s ->
            config.searchTags.entries.sumOf { (k, v) ->
                if (k.startsWith(s)) 5 * v
                else if (k.contains(s)) v
                // yes this is necessary
                else 0.toInt()
            }
        }
    }

    override fun hide() {
        super.hide()
        components.forEach { it.second.hide() }
        currSort = emptyList()
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