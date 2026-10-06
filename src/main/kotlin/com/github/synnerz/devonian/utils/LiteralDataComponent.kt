package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import net.minecraft.locale.Language
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.PlainTextContents
import net.minecraft.util.FormattedCharSequence

class LiteralDataComponent<T>(str: String, var data: T) : Component {
    private var style_ = Style.EMPTY
    private val contents = PlainTextContents.create(str.replaceCodes())
    private val siblings_ = mutableListOf<Component>()
    private var language: Language? = null
    private var fcsCache: FormattedCharSequence? = null

    override fun getStyle(): Style = style_
    fun setStyle(style: Style) = apply {
        style_ = style
    }

    override fun getContents(): ComponentContents = contents
    override fun getSiblings(): List<Component> = siblings_
    override fun getVisualOrderText(): FormattedCharSequence {
        val lang = Language.getInstance()
        if (lang != language) {
            fcsCache = lang.getVisualOrder(this)
            language = lang
        }
        return fcsCache!!
    }

    fun append(text: String) = apply {
        if (text.isNotEmpty()) append(ChatUtils.literal(text))
    }

    fun append(comp: Component) = apply {
        siblings_.add(comp)
    }
}