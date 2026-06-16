package com.github.synnerz.devonian.config.ui.talium

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.talium.components.UIBase
import com.github.synnerz.talium.components.UIRect
import com.github.synnerz.talium.components.UIScrollable
import com.github.synnerz.talium.components.UIText
import com.github.synnerz.talium.constraints.UIFlexWrapConstraint

class Category(
    val category: Categories,
    rightPanel: UIBase,
    leftPanel: UIBase,
) : SharedCategory(category.displayName) {
    val configs = Config.categories[category]!!
    private val subcategoryPanel = UIRect(0.0, 0.0, 100.0, 8.0, parent = rightPanel).apply {
        setColor(ColorPalette.TERTIARY_COLOR)
    }
    private val subcategoriesRect = mutableMapOf<String, Pair<UIScrollable, UIText>>()
    private var currentSubcategory = category.subcategories[0]

    init {
        UIRect(
            8.0, 0.0,
            92.0, 6.0,
            parent = leftPanel
        ).apply {
            yConstraint = UIFlexWrapConstraint(5.0)
            onMouseRelease {
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
        category.subcategories.forEachIndexed { jdx, name ->
            val text = UIText(0.0, 0.0, 100.0, 100.0, name, true).apply {
                if (jdx == 0) {
                    setColor(ColorPalette.TEXT_COLOR)
                    if (category.subcategories.size > 1) addEffect(UILineEffect())
                } else setColor(ColorPalette.LIGHT_TEXT_COLOR)

                onResize { _, w ->
                    textScale = 2f / w.scaleFactor
                }
            }
            subcategoriesRect[name] = UIScrollable(0.0, 9.0, 100.0, 81.0, parent = rightPanel).apply { hide() } to text
            subcategoryPanel.addChild(
                UIRect(subCatMargins + (subCatWidth + subCatGap) * jdx, 2.5, subCatWidth, 95.0).apply {
                    addChild(text)
                    onMouseRelease { event ->
                        if (event.button != 0) return@onMouseRelease
                        subcategoriesRect[currentSubcategory]?.second?.setColor(ColorPalette.LIGHT_TEXT_COLOR)
                        subcategoriesRect[currentSubcategory]?.second?.removeEffects(UILineEffect::class.java)
                        subcategoriesRect[currentSubcategory]?.first?.hide()
                        hideColorPickers()
                        currentSubcategory = name
                        subcategoriesRect[currentSubcategory]?.first?.unhide()
                        subcategoriesRect[currentSubcategory]?.second?.setColor(ColorPalette.TEXT_COLOR)
                        subcategoriesRect[currentSubcategory]?.second?.addEffect(UILineEffect())
                    }
                }
            )
        }

        configs.entries.forEach { (subname, list) ->
            val scrollable = subcategoriesRect[subname]!!

            var idx = 0
            list.sortedBy {
                if (it is ConfigData.Button)
                    it.parent?.configName?.trim()?.lowercase()
                        ?: it.displayName.trim().lowercase()
                else
                    it.configName?.trim()?.lowercase()
            }.forEach { data ->
                if (data.isHidden && !Devonian.isDev) return@forEach
                val i = idx++

                val y = 1 + i * 17.0
                val e = createComp(data, scrollable.first)
                e.container._y = y
            }
        }

        hide()
    }

    override fun hide() {
        super.hide()
        subcategoryPanel.hide()
        subcategoriesRect[currentSubcategory]?.first?.hide()
    }

    override fun unhide() {
        super.unhide()
        subcategoryPanel.unhide()
        subcategoriesRect[currentSubcategory]?.first?.unhide()
    }
}