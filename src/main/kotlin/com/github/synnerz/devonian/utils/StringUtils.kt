package com.github.synnerz.devonian.utils

object StringUtils {
    private val removeCodesRegex = "[\\u00a7&][0-9a-fk-or]".toRegex()

    fun String.clearCodes(): String = this.replace(removeCodesRegex, "")
}