package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.utils.StringUtils.snakeCaseToSentence
import com.github.synnerz.devonian.utils.render.ChromaText
import com.google.gson.Gson
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import java.util.*

object EnchantRegistry {
    val Normal = mutableListOf<NormalEnchant>()
    val Ultimate = mutableListOf<UltimateEnchant>()
    val Stacking = mutableListOf<StackingEnchant>()
    val CumulativeNbt = mutableMapOf<String, Enchantment>()
    val CumulativeLore = mutableMapOf<String, Enchantment>()

    init {
        // TODO: get from api
        this::class.java.getResourceAsStream("/assets/devonian/enchants.json")?.bufferedReader()?.use {
            Gson().fromJson(it, EnchantJson::class.java)
        }?.also {
            Normal.addAll(it.NORMAL)
            Ultimate.addAll(it.ULTIMATE)
            Stacking.addAll(it.STACKING)

            it.NORMAL.forEach {
                CumulativeNbt[it.nbtName] = it
                CumulativeLore[it.loreName] = it
            }
            it.ULTIMATE.forEach {
                CumulativeNbt[it.nbtName] = it
                CumulativeLore[it.loreName] = it
            }
            it.STACKING.forEach {
                CumulativeNbt[it.nbtName] = it
                CumulativeLore[it.loreName] = it
            }
        }
    }

    fun getOrUnknownNbt(nbt: String) =
        CumulativeNbt[nbt.lowercase()] ?: UnknownEnchant(nbt.lowercase().snakeCaseToSentence(), nbt.lowercase())

    fun getOrUnknownLore(lore: String) =
        CumulativeLore[lore] ?: UnknownEnchant(lore, lore.lowercase().replace(' ', '_'))

    private data class EnchantJson(
        val NORMAL: List<NormalEnchant>,
        val ULTIMATE: List<UltimateEnchant>,
        val STACKING: List<StackingEnchant>,
    )
}

private val bestColor = ChromaText.createStyle()
private val greatColor = Style.EMPTY.withColor(ChatFormatting.GOLD)!!
private val goodColor = Style.EMPTY.withColor(ChatFormatting.BLUE)!!
private val poorColor = Style.EMPTY.withColor(ChatFormatting.GRAY)!!
private val ultColor = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true)
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

data class NormalEnchant(
    override val loreName: String,
    override val nbtName: String,
    override val abbreviation: String,
    override val goodLevel: Int,
    override val maxLevel: Int,
) : Enchantment {
    override val type = EnchantType.NORMAL
}

data class UltimateEnchant(
    override val loreName: String,
    override val nbtName: String,
    override val abbreviation: String,
    override val maxLevel: Int,
) : Enchantment {
    override val type = EnchantType.ULTIMATE
    override val goodLevel: Int
        get() = 0

    override fun getStyle(level: Int): Style = ultColor
}

data class StackingEnchant(
    override val loreName: String,
    override val nbtName: String,
    override val abbreviation: String,
    val nbtTag: String,
    val displayName: String,
    val progress: List<Int>,
) : Enchantment {
    override val type = EnchantType.STACKING
    override val goodLevel: Int
        get() = 0
    override val maxLevel: Int
        get() = 10
    val progressTree = TreeSet(progress)
}

data class UnknownEnchant(
    override val loreName: String,
    override val nbtName: String,
) : Enchantment {
    override val type = EnchantType.UNKNOWN
    override val goodLevel: Int
        get() = 0
    override val maxLevel: Int
        get() = 0
    override val abbreviation: String = loreName.take(3)

    override fun getStyle(level: Int): Style = unkColor
}

enum class EnchantType {
    NORMAL,
    ULTIMATE,
    STACKING,
    UNKNOWN;
}