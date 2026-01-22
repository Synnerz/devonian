package com.github.synnerz.devonian.features.debug

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.PersistentJson
import com.github.synnerz.devonian.utils.Serializer
import com.github.synnerz.devonian.utils.math.MathUtils
import com.google.gson.JsonArray
import net.minecraft.world.phys.AABB
import kotlin.math.PI

object WAILA : Feature(
    "waila",
    "`/dv waila` to copy the entities you are looking at to your clipboard.",
    Categories.DEBUG,
    subcategory = "Utils",
    searchTags = setOf("entity", "copy", "nbt"),
) {
    override fun initialize() {
        DevonianCommand.command.subcommand("waila") { _, _ ->
            Scheduler.scheduleTask { dump() }
            return@subcommand 1
        }
    }

    private const val NEAR = 0.01
    private const val FAR = 8.0
    private const val FOV = 45.0 * PI / 180.0
    private const val ASPECT = 1.0

    fun dump() {
        val arr = JsonArray()

        val player = minecraft.player ?: return
        val world = minecraft.level ?: return

        val x = player.x
        val y = player.eyeY
        val z = player.z
        val look = player.lookAngle
        val hull = AABB(
            x - FAR, y - FAR, z - FAR,
            x + FAR, y + FAR, z + FAR,
        )
        val planes = arrayOf(
            // left
            MathUtils.rotate(look.x, look.y, look.z, (+FOV - PI) * 0.5, 0.0, 0.0)
                .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
            // right
            MathUtils.rotate(look.x, look.y, look.z, (-FOV + PI) * 0.5, 0.0, 0.0)
                .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
            // bottom
            MathUtils.rotate(look.x, look.y, look.z, 0.0, (+FOV * ASPECT - PI) * 0.5, 0.0)
                .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
            // top
            MathUtils.rotate(look.x, look.y, look.z, 0.0, (-FOV * ASPECT + PI) * 0.5, 0.0)
                .let { (nx, ny, nz) -> doubleArrayOf(nx, ny, nz, -(nx * x + ny * y + nz * z)) },
            // near
            doubleArrayOf(
                look.x,
                look.y,
                look.z,
                -(look.x * (x + look.x * NEAR) + look.y * (y + look.y * NEAR) + look.z * (z + look.z * NEAR))
            ),
            // far
            doubleArrayOf(
                -look.x,
                -look.y,
                -look.z,
                look.x * (x + look.x * FAR) + look.y * (y + look.y * FAR) + look.z * (z + look.z * FAR)
            ),
        )

        world.entitiesForRendering().forEach { ent ->
            if (ent === player) return@forEach
            val bb = ent.boundingBox
            if (!bb.intersects(hull)) return@forEach

            if (
                planes.any { bb.minX * it[0] + bb.minY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                planes.any { bb.minX * it[0] + bb.minY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 } &&
                planes.any { bb.minX * it[0] + bb.maxY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                planes.any { bb.minX * it[0] + bb.maxY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 } &&
                planes.any { bb.maxX * it[0] + bb.minY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                planes.any { bb.maxX * it[0] + bb.minY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 } &&
                planes.any { bb.maxX * it[0] + bb.maxY * it[1] + bb.minZ * it[2] + it[3] < 0.0 } &&
                planes.any { bb.maxX * it[0] + bb.maxY * it[1] + bb.maxZ * it[2] + it[3] < 0.0 }
            ) return@forEach

            arr.add(Serializer.serializeEntity(ent).json)
        }

        ChatUtils.sendMessage("Copied ${arr.size()} entities to clipboard.")
        minecraft.keyboardHandler.clipboard = PersistentJson.gson.toJson(arr)
    }
}