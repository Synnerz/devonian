package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.world.entity.player.Player
import java.awt.Color
import kotlin.math.PI

data class DungeonPlayer(
    val name: String,
    val profileInfo: PlayerInfo?,
    var role: DungeonClass,
    var classLevel: Int,
    var isDead: Boolean,
    var isDisconnected: Boolean = false,
    var entity: Player? = null,
    var position: PlayerComponentPosition? = null,
    var updateTime: Double? = null,
    var lastPosition: PlayerComponentPosition? = null,
    var lastUpdateTime: Double? = null,
) {
    fun tick() {
        if ((entity?.isDeadOrDying ?: true) || (entity?.isRemoved ?: true)) entity = null
        val ent = entity ?: return
        updatePosition(
            PlayerComponentPosition.fromWorld(
                ent.x,
                ent.z,
                -(ent.yRot.toDouble() + 90) * PI / 180.0
            )
        )
    }

    fun updatePosition(pos: PlayerComponentPosition) {
        val time = System.nanoTime() * 1.0e-6
        if (position == null) {
            position = pos
            updateTime = time
            return
        }
        lastPosition = position
        lastUpdateTime = updateTime
        position = pos
        updateTime = time
    }

    fun getLerpedPosition(): PlayerComponentPosition? {
        val pos = position ?: return null
        val lastPos = lastPosition ?: return pos
        val uTime = updateTime ?: return pos
        val luTime = lastUpdateTime ?: return pos
        val time = System.nanoTime() * 1.0e-6
        val f = (time - uTime) / (uTime - luTime)
        return PlayerComponentPosition(
            MathUtils.lerp(f, lastPos.x, pos.x),
            MathUtils.lerp(f, lastPos.z, pos.z),
            MathUtils.lerpAngle(f, lastPos.r, pos.r),
        )
    }
}

enum class DungeonClass(
    val shortName: String,
    val singleLetter: Char,
    initialColorCode: String,
    initialColor: Color,
) {
    Archer("Arch", 'a', "&c", Color(255, 85, 85, 255)),
    Berserk("Bers", 'b', "&6", Color(255, 170, 0, 255)),
    Mage("Mage", 'm', "&3", Color(0, 170, 170, 255)),
    Healer("Heal", 'h', "&5", Color(170, 0, 170, 255)),
    Tank("Tank", 't', "&a", Color(85, 255, 85, 255)),

    Unknown("Unknown", '\u0000', "", Color(0, 0, 0, 0));

    private val colorSetting = ConfigData.ColorPicker(
        "dungeonColor$name",
        initialColor.rgb,
        null,
        "Color of anything related to the $name class.",
        "$name Color",
        "Dungeon Colors",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Dungeon Colors")
    }

    val color: Color
        get() = colorSetting.getColor()

    val colorRgb: Int
        get() = colorSetting.get()

    private var cachedColorCode = initialColorCode.replaceCodes()
    private val colorCodeSetting = ConfigData.TextInput(
        "dungeonColorCode$name",
        initialColorCode,
        null,
        "Color code for anything related to the $name class. Use & instead of §.",
        "$name Color Code",
        "Dungeon Colors",
    ).also {
        Config.registerCategory(it, Categories.GLOBAL, "Dungeon Colors")
        it.onChange {
            cachedColorCode = it.replaceCodes()
        }
    }
    val colorCode: String
        get() = cachedColorCode

    companion object {
        fun from(fullName: String): DungeonClass = when (fullName) {
            "Archer" -> Archer
            "Berserk" -> Berserk
            "Mage" -> Mage
            "Healer" -> Healer
            "Tank" -> Tank
            else -> Unknown
        }
    }
}