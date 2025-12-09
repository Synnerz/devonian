package com.github.synnerz.devonian.utils

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.text.NumberFormat
import java.util.*

object StringUtils {
    private val removeCodesRegex = "[\\u00a7&][0-9a-fk-or]".toRegex()
    private val romanValues = mapOf(
        'I' to 1,
        'V' to 5,
        'X' to 10,
        'L' to 50,
        'C' to 100,
        'D' to 500,
        'M' to 1000,
    )
    private val colorToFormat = ChatFormatting.entries.mapNotNull { format ->
        TextColor.fromLegacyFormat(format)?.let { it to format }
    }.toMap()

    fun String.clearCodes(): String = this.replace(removeCodesRegex, "")

    fun parseRoman(roman: String): Int {
        var lastValue = 0
        var total = 0
        roman.toCharArray().forEach {
            val value = romanValues[it] ?: return@forEach
            if (lastValue < value) total -= lastValue
            else total += lastValue
            lastValue = value
        }
        return total + lastValue
    }

    fun colorForNumber(num: Double, max: Double) = when {
        num >= max * 0.75 -> "§2"
        num >= max * 0.50 -> "§e"
        num >= max * 0.25 -> "§6"
        else -> "§4"
    }

    fun colorForNumber(num: Int, max: Int) = colorForNumber(num.toDouble(), max.toDouble())

    private fun parseStyle(style: Style): String = buildString {
        append("§r")

        style.color?.let(colorToFormat::get)?.run(::append)

        when {
            style.isBold -> append("§l")
            style.isItalic -> append("§o")
            style.isUnderlined -> append("§n")
            style.isStrikethrough -> append("§m")
            style.isObfuscated -> append("§k")
        }
    }

    private fun parseFormat(_text: Component): String {
        var str = ""

        _text.contents.visit({ style, text ->
            val styleFormat = parseStyle(style)
            str += "${styleFormat}$text"
            Optional.empty<Any>()
        }, _text.style)

        return str
    }

    fun Component.colorCodes(): String {
        var str = parseFormat(this)

        str += this.siblings.joinToString("", transform = ::parseFormat)

        return str
    }

    fun addCommas(number: Number): String {
        return number.let {
            NumberFormat.getNumberInstance(Locale.US).format(it)
        }
    }

    fun formatSeconds(seconds: Long): String {
        val s = seconds % 60
        val m = (seconds / 60) % 60
        val h = seconds / 3600

        return buildString {
            if (h > 0) append("%02dh ".format(h))
            if (m > 0 || h > 0) append("%02dm ".format(m))
            append("%02ds".format(s))
        }
    }

    private val camelCaseRegex = "[a-z]+|[A-Z](?:[a-z]+|[A-Z]*(?![a-z]))|[.\\d]+".toRegex()
    fun String.camelCaseToSentence(): String = camelCaseRegex.replace(this) {
        it.value.replaceFirstChar { it.uppercaseChar() } + " "
    }.trim()
}