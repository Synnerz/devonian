package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.mixin.accessor.ClientTextTooltipAccessor
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import java.text.NumberFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

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

    private val romanNums = listOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX").let { r0to9 ->
        listOf("", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC").flatMap { pre ->
            r0to9.map { pre + it }
        }
    }.toTypedArray()

    fun formatRoman(num: Int): String {
        if (num < 0) return '-' + formatRoman(-num)
        if (num < romanNums.size) return romanNums[num]
        return buildString {
            append("M".repeat(num / 1000))
            var num = num % 1000
            if (num >= 900) {
                num -= 900
                append("CM")
            }
            if (num >= 500) {
                num -= 500
                append("D")
            }
            if (num >= 400) {
                num -= 400
                append("CD")
            }
            while (num >= 100) {
                num -= 100
                append("C")
            }
            append(romanNums[num])
        }
    }

    fun colorForNumber(num: Double, max: Double) = when {
        num >= max * 0.75 -> "§2"
        num >= max * 0.50 -> "§e"
        num >= max * 0.25 -> "§6"
        else -> "§4"
    }

    fun colorForNumber(num: Int, max: Int) = colorForNumber(num.toDouble(), max.toDouble())
    fun colorForNumber(num: Long, max: Long) = colorForNumber(num.toDouble(), max.toDouble())

    fun parseStyle(style: Style): String = buildString {
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

    fun fromLegacy(string: String): Component {
        val component = Component.literal("")
        var oldStr = ""
        var hadSS = false
        var style = Style.EMPTY

        for (char in string) {
            if (char == '§') {
                hadSS = true
                if (oldStr.isNotEmpty()) {
                    component.append(Component.literal(oldStr).withStyle(style))
                    oldStr = ""
                }
                continue
            }
            if (hadSS && (char.isLetter() || char.isDigit())) {
                hadSS = false

                style = when (char) {
                    'l' -> style.withBold(true)
                    'o' -> style.withItalic(true)
                    'n' -> style.withUnderlined(true)
                    'm' -> style.withStrikethrough(true)
                    'k' -> style.withObfuscated(true)
                    '0' -> Style.EMPTY.withColor(ChatFormatting.BLACK)
                    '1' -> Style.EMPTY.withColor(ChatFormatting.DARK_BLUE)
                    '2' -> Style.EMPTY.withColor(ChatFormatting.DARK_GREEN)
                    '3' -> Style.EMPTY.withColor(ChatFormatting.DARK_AQUA)
                    '4' -> Style.EMPTY.withColor(ChatFormatting.DARK_RED)
                    '5' -> Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)
                    '6' -> Style.EMPTY.withColor(ChatFormatting.GOLD)
                    '7' -> Style.EMPTY.withColor(ChatFormatting.GRAY)
                    '8' -> Style.EMPTY.withColor(ChatFormatting.DARK_GRAY)
                    '9' -> Style.EMPTY.withColor(ChatFormatting.BLUE)
                    'a' -> Style.EMPTY.withColor(ChatFormatting.GREEN)
                    'b' -> Style.EMPTY.withColor(ChatFormatting.AQUA)
                    'c' -> Style.EMPTY.withColor(ChatFormatting.RED)
                    'd' -> Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE)
                    'e' -> Style.EMPTY.withColor(ChatFormatting.YELLOW)
                    'f' -> Style.EMPTY.withColor(ChatFormatting.WHITE)
                    'r' -> Style.EMPTY
                    else -> Style.EMPTY
                }
                continue
            }

            oldStr += char
        }
        if (oldStr.isNotEmpty())
            component.append(Component.literal(oldStr).withStyle(style))

        return component
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

    fun addCommasTruncate(number: Number): String {
        return number.let {
            NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 0
                minimumFractionDigits = 0
            }.format(it)
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

    // "1h 02m 13.42s"
    fun formatTime(time: Long, decimals: Int, maxUnits: Int = 4): String {
        if (time < 0L) return '-' + formatTime(-time, decimals)
        val ms = time % 1000
        var t = time / 1000
        val s = t % 60
        t /= 60
        val m = t % 60
        t /= 60
        val h = t % 24
        val d = t / 24
        var i = maxUnits

        return buildString {
            if (d > 0) {
                append("${d}d ")
                if (--i <= 0) return@buildString
                append("%02dh ".format(h))
                if (--i <= 0) return@buildString
                append("%02dm ".format(m))
                if (--i <= 0) return@buildString
                append("%02d".format(s))
                if (--i <= 0) return@buildString
            } else if (h > 0) {
                append("${h}h ")
                if (--i <= 0) return@buildString
                append("%02dm ".format(m))
                if (--i <= 0) return@buildString
                append("%02d".format(s))
                if (--i <= 0) return@buildString
            } else if (m > 0) {
                append("${m}m ")
                if (--i <= 0) return@buildString
                append("%02d".format(s))
                if (--i <= 0) return@buildString
            } else append(s)
            append("%.${decimals}f".format(ms / 1000.0).substring(1))
            append("s")
        }.trim()
    }

    fun formatDuration(time: Long): String {
        if (time < 0) return '-' + formatDuration(-time)
        val s = time / 1000L
        if (s < 60) return "$s second${if (s == 1L) "" else "s"}"
        val m = s / 60
        if (m < 60) return "$m minute${if (m == 1L) "" else "s"}"
        val h = m / 60
        if (h < 24) return "$h hour${if (h == 1L) "" else "s"}"
        val d = h / 24
        if (d < 31) return "$d day${if (d == 1L) "" else "s"}"
        val months = d / 30.5
        if (months < 12) return "%.1f month${if (months >= 2.0) "s" else ""}".format(months)
        val years = d / 365.0
        return "%.1f year${if (years >= 2.0) "s" else ""}".format(years)
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

    // uses as few sig figs as possible
    private val digitChars = Array(10) { '0' + it }
    private val placePrefix = arrayOf(
        "",
        "", "", "",
        "k", "k", "k",
        "M", "M", "M",
        "B",
    )
    fun formatShortest(num: Int, maxDigits: Int = 10): String {
        if (num < 0) return '-' + formatShortest(-num, maxDigits)
        if (num == 0) return "0"

        val digits = mutableListOf<Int>()
        var num = num
        while (num > 0) {
            digits.add(num % 10)
            num /= 10
        }
        digits.reverse()

        val count = max(
            ((digits.size - 1) % 3) + 1,
            min(
                maxDigits,
                digits.indexOf(0).let { if (it == -1) digits.size else it }
            )
        )

        return buildString {
            for (i in 0 until count) {
                if (i == 1 && digits.size > 3) append('.')
                append(digitChars[digits[i]])
            }
            append(placePrefix[digits.size])
        }
    }

    fun tooltipAsString(tooltip: ClientTooltipComponent): String? {
        val tip = tooltip as? ClientTextTooltipAccessor ?: return null
        val seq = tip.text
        return buildString {
            seq.accept { _, _, c ->
                append(c.toChar())
                return@accept true
            }
        }
    }

    private val camelCaseRegex = "[a-z]+|[A-Z](?:[a-z]+|[A-Z]*(?![a-z]))|[.\\d]+".toRegex()
    fun String.camelCaseToSentence(): String = camelCaseRegex.replace(this) {
        it.value.replaceFirstChar { it.uppercaseChar() } + " "
    }.trim()

    private val snakeCaseRegex = "_\\w".toRegex()
    fun String.snakeCaseToSentence(): String = snakeCaseRegex.replace(this) {
        " " + it.value.last().uppercaseChar()
    }.replaceFirstChar { it.uppercaseChar() }
}