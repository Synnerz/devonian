package com.github.synnerz.devonian.utils

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.text.NumberFormat
import java.util.*

object StringUtils {
    private val removeCodesRegex = "[\\u00a7&][0-9a-fk-or]".toRegex(RegexOption.IGNORE_CASE)
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
    private val escapedAmp = "&{2}".toRegex()
    private val ampToSection = "&(?=[0-9a-fklmnor])".toRegex()

    fun String.clearCodes(): String = this.replace(removeCodesRegex, "")

    fun String.replaceCodes(): String = this
        .split(escapedAmp)
        .joinToString("&") {
            it.replace(ampToSection, "§")
        }

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
    fun colorForNumber(num: Long, max: Long) = colorForNumber(num.toDouble(), max.toDouble())

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

    // "1:02:13.42"
    fun formatClock(time: Long, decimals: Int): String {
        if (time < 0L) return '-' + formatClock(-time, decimals)
        val ms = time % 1000
        var t = time / 1000
        val s = t % 60
        t /= 60
        val m = t % 60
        val h = t / 60

        return buildString {
            if (h > 0) {
                append("$h:")
                append("%02d:".format(m))
                append("%02d".format(s))
            } else if (m > 0) {
                append("$m:")
                append("%02d".format(s))
            } else append(s)
            if (decimals == 0) append('s')
            else append("%.${decimals}f".format(ms / 1000.0).substring(1))
        }
    }

    // "1h 2m 13.42s"
    fun formatTime(time: Long, decimals: Int): String {
        if (time < 0L) return '-' + formatTime(-time, decimals)
        val ms = time % 1000
        var t = time / 1000
        val s = t % 60
        t /= 60
        val m = t % 60
        val h = t / 60

        return buildString {
            if (h > 0) {
                append("${h}h ")
                append("%02dm ".format(m))
                append("%02d".format(s))
            } else if (m > 0) {
                append("${m}m ")
                append("%02d".format(s))
            } else append(s)
            append("%.${decimals}f".format(ms / 1000.0).substring(1))
            append("s")
        }
    }

    fun shortenNumber(num: Int): String {
        if (num < 0) return '-' + shortenNumber(-num)
        if (num < 1000) return num.toString()
        var num = num / 1000.0
        if (num < 10.0) return "%.2fK".format(num)
        if (num < 100.0) return "%.1fK".format(num)
        if (num < 1000.0) return "%.0fK".format(num)

        num /= 1000.0
        if (num < 10.0) return "%.2fM".format(num)
        if (num < 100.0) return "%.1fM".format(num)
        if (num < 1000.0) return "%.0fM".format(num)

        num /= 1000.0
        return "%.2fB".format(num)
    }

    // 677.9M -> 677_900_000
    private val nonNumReg = "[^\\d.]".toRegex()
    fun parseShortenedNumber(str: String): Int {
        val factor = when (str.lastOrNull()) {
            null -> return 0
            'K' -> 1_000
            'M' -> 1_000_000
            'B' -> 1_000_000_000
            else -> 1
        }
        val raw = str.replace(nonNumReg, "")
        val base = raw.toDoubleOrNull() ?: return 0
        return (base * factor).toInt()
    }

    // 1h 54m 21s -> 6861
    fun parseTimer(str: String): Int {
        return str.split(' ').sumOf {
            if (it.isEmpty()) 0
            else (it.dropLast(1).toIntOrNull() ?: 0) * when (it.last()) {
                's', 'S' -> 1
                'm', 'M' -> 60
                'h', 'H' -> 3600
                'd', 'D' -> 86400
                else -> 0
            }
        }
    }

    private val camelCaseRegex = "[a-z]+|[A-Z](?:[a-z]+|[A-Z]*(?![a-z]))|[.\\d]+".toRegex()
    fun String.camelCaseToSentence(): String = camelCaseRegex.replace(this) {
        it.value.replaceFirstChar { it.uppercaseChar() } + " "
    }.trim()
}