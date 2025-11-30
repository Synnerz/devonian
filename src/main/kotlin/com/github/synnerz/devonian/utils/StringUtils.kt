package com.github.synnerz.devonian.utils

object StringUtils {
    private val removeCodesRegex = "[\\u00a7&][0-9a-fk-or]".toRegex()

    fun String.clearCodes(): String = this.replace(removeCodesRegex, "")

    private val romanValues = mapOf(
        'I' to 1,
        'V' to 5,
        'X' to 10,
        'L' to 50,
        'C' to 100,
        'D' to 500,
        'M' to 1000,
    )
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
}