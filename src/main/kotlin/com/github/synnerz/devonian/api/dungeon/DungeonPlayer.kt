package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.utils.math.MathUtils
import net.minecraft.entity.player.PlayerEntity
import kotlin.math.PI

data class DungeonPlayer(
    val name: String,
    var role: DungeonClass,
    var classLevel: Int,
    var isDead: Boolean,
    var entity: PlayerEntity? = null,
    var position: PlayerComponentPosition? = null,
    var updateTime: Double? = null,
    var lastPosition: PlayerComponentPosition? = null,
    var lastUpdateTime: Double? = null,
) {
    fun tick() {
        if (entity?.isDead ?: true) entity = null
        val ent = entity ?: return
        updatePosition(PlayerComponentPosition.fromWorld(
            ent.x,
            ent.z,
            -(ent.yaw.toDouble() + 90) * PI / 180.0
        ))
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

enum class DungeonClass(val shortName: String, val singleLetter: Char, val colorCode: String) {
    Archer("Arch", 'a', "§c"),
    Berserk("Bers", 'b', "§6"),
    Mage("Mage", 'm', "§3"),
    Healer("Heal", 'h', "§5"),
    Tank("Tank", 't', "§a"),

    Unknown("Unknown", '\u0000', "");

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