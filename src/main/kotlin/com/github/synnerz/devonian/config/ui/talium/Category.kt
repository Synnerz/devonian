package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.talium.components.UIBase
import com.github.synnerz.talium.components.UIElement
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIScrollable
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.constraints.UIFlexWrapConstraint
import java.awt.Color

class Category(
    val category: Categories,
    rightPanel: UIBase,
    leftPanel: UIBase,
) : SharedCategory(category.displayName) {
    val configs = Config.categories[category]!!
    private val subcategoryPanel = UIRect(0.0, 0.0, 100.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
    }
    private val subcategories = mutableMapOf<String, Subcategory>()
    private var currentSubcategory = category.subcategories[0]

    private class Subcategory(
        val scrollable: UIScrollable,
        val title: UIText,
        val comps: List<Pair<ConfigData<*>, UIPair>>,
    )

    init {
        UIRect(
            8.0, 0.0,
            92.0, 6.0,
            parent = leftPanel
        ).apply {
            yConstraint = UIFlexWrapConstraint(5.0)
            onMouseRelease {
                if (it.button != 1) return@onMouseRelease
                if (ConfigGui.selectedCategory === this@Category) return@onMouseRelease
                if (ConfigGui.selectedCategory == null)
                    ConfigGui.searchCategory.hide()
                else
                    ConfigGui.selectedCategory?.hide()
                ConfigGui.selectedCategory = this@Category
                this@Category.unhide()
            }
            addChild(categoryTitle)
        }

        val subCount = category.subcategories.size
        val subCatGap = 0.5
        val subCatMargins = 2.0
        val subCatWidth = (100.0 - subCatMargins * 2 - subCatGap * (subCount - 1)) / subCount

        configs.entries.forEachIndexed { i, (subname, list) ->
            val scrollable = UIScrollable(0.0, 9.0, 100.0, 81.0, parent = rightPanel).apply {
                hide()
                drawScrollbar = true
                scrollBgColor = Color(35, 35, 35, 255)
                scrollFgColor = Color(70, 70, 70, 255)
            }
            val title = UIText(0.0, 0.0, 100.0, 100.0, subname, true).apply {
                if (i == 0) {
                    setColor(ColorPalette.TEXT_COLOR)
                    if (category.subcategories.size > 1) addEffect(UILineEffect())
                } else setColor(ColorPalette.LIGHT_TEXT_COLOR)

                onResize { _, w ->
                    textScale = 2f / w.scaleFactor
                }
            }
            subcategoryPanel.addChild(
                UIRect(subCatMargins + (subCatWidth + subCatGap) * i, 2.5, subCatWidth, 95.0).apply {
                    addChild(title)
                    onMouseRelease { event ->
                        if (event.button != 1) return@onMouseRelease
                        if (currentSubcategory == subname) return@onMouseRelease

                        subcategories[currentSubcategory]?.title?.setColor(ColorPalette.LIGHT_TEXT_COLOR)
                        subcategories[currentSubcategory]?.title?.removeEffects(UILineEffect::class.java)
                        subcategories[currentSubcategory]?.scrollable?.hide()

                        currentSubcategory = subname

                        title.setColor(ColorPalette.TEXT_COLOR)
                        title.addEffect(UILineEffect())
                        scrollable.unhide()

                        rebuildComps()
                        scrollable.yOffset = 0.0
                        scrollable.updateScrollY(0.0)
                    }
                }
            )

            val comps = list.sortedBy {
                if (it is ConfigData.Button)
                    it.parent?.configName?.trim()?.lowercase()
                        ?: it.displayName.trim().lowercase()
                else
                    it.configName?.trim()?.lowercase()
            }.mapNotNull { data ->
                if (!data.shouldExist()) return@mapNotNull null

                data to createComp(data, scrollable)
            }

            subcategories[subname] = Subcategory(scrollable, title, comps)
        }

        hide()
    }

    override fun rebuildComps() {
        val sub = subcategories[currentSubcategory] ?: return

        var idx = 0
        sub.comps.forEach { (data, ui) ->
            if (!data.shouldShow()) {
                ui.container.hide()
                ui.container._y = 0.0
                ui.container.markDirty()
                ui.container.checkUpdate()
                return@forEach
            }

            val i = idx++
            val y = 1 + i * 17.0
            ui.container.unhide()
            ui.container._y = y
            ui.container.markDirty()
            ui.container.checkUpdate()
        }

        sub.scrollable.updateScrollY(0.0)
        sub.scrollable.markDirty()
        sub.scrollable.checkUpdate()
    }

    override fun hide() {
        super.hide()
        subcategoryPanel.hide()
        subcategories[currentSubcategory]?.scrollable?.hide()
    }

    override fun unhide() {
        super.unhide()
        subcategoryPanel.unhide()
        subcategories[currentSubcategory]?.scrollable?.unhide()
    }
}