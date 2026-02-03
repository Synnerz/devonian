package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.utils.StringUtils.camelCaseToSentence
import com.google.gson.Gson
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.TreeSet

object EnchantRegistry {
    val Normal = mutableListOf<NormalEnchants>()
    val Ultimate = mutableListOf<UltimateEnchants>()
    val Stacking = mutableListOf<StackingEnchants>()
    val Cumulative = mutableMapOf<String, Enchantment>()

    init {
        // TODO: get from api
        this::class.java.getResourceAsStream("/assets/devonian/enchants.json")?.bufferedReader()?.use {
            Gson().fromJson(it, EnchantJson::class.java)
        }?.also {
            Normal.addAll(it.NORMAL)
            Ultimate.addAll(it.ULTIMATE)
            Stacking.addAll(it.STACKING)

            it.NORMAL.forEach { Cumulative[it.nbtName] = it }
            it.ULTIMATE.forEach { Cumulative[it.nbtName] = it }
            it.STACKING.forEach { Cumulative[it.nbtName] = it }
        }
    }

    fun getOrUnknown(nbt: String) =
        Cumulative[nbt] ?: UnknownEnchant(nbt.camelCaseToSentence(), nbt)

    private data class EnchantJson(
        val NORMAL: List<NormalEnchants>,
        val ULTIMATE: List<UltimateEnchants>,
        val STACKING: List<StackingEnchants>,
    )
}

// TODO: chroma
private val bestColor = Style.EMPTY.withColor(ChatFormatting.AQUA)!!
private val greatColor = Style.EMPTY.withColor(ChatFormatting.GOLD)!!
private val goodColor = Style.EMPTY.withColor(ChatFormatting.BLUE)!!
private val poorColor = Style.EMPTY.withColor(ChatFormatting.GRAY)!!
private val ultColor = Style.EMPTY.withColor(ChatFormatting.RED).withBold(true)
private val unkColor = Style.EMPTY.withColor(ChatFormatting.DARK_RED)
interface Enchantment {
    val type: EnchantType
    val loreName: String
    val nbtName: String
    val abbreviation: String
    val goodLevel: Int
    val maxLevel: Int

    fun getStyle(level: Int): Style = when {
        level >= maxLevel -> bestColor
        level > goodLevel -> greatColor
        level == goodLevel -> goodColor
        else -> poorColor
    }
    fun getFormatted(level: Int, str: String): Component {
        return Component.literal(str).withStyle(getStyle(level))
    }
}

data class NormalEnchants(
    override val loreName: String,
    override val nbtName: String,
    override val abbreviation: String,
    override val goodLevel: Int,
    override val maxLevel: Int,
) : Enchantment {
    override val type = EnchantType.NORMAL
}

data class UltimateEnchants(
    override val loreName: String,
    override val nbtName: String,
    override val abbreviation: String,
    override val maxLevel: Int,
) : Enchantment {
    override val type = EnchantType.ULTIMATE
    override val goodLevel: Int = 0

    override fun getStyle(level: Int): Style = ultColor
}

data class StackingEnchants(
    override val loreName: String,
    override val nbtName: String,
    override val abbreviation: String,
    val nbtTag: String,
    val displayName: String,
    val progress: List<Int>,
) : Enchantment {
    override val type = EnchantType.STACKING
    override val goodLevel: Int = 0
    override val maxLevel: Int = 10
    val progressTree = TreeSet(progress)
}

data class UnknownEnchant(
    override val loreName: String,
    override val nbtName: String,
) : Enchantment {
    override val type = EnchantType.UNKNOWN
    override val goodLevel: Int = 0
    override val maxLevel: Int = 0
    override val abbreviation: String = loreName.take(3)

    override fun getStyle(level: Int): Style = unkColor
}

enum class EnchantType {
    NORMAL,
    ULTIMATE,
    STACKING,
    UNKNOWN;
}