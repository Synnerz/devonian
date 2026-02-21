package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.mixin.accessor.LocalPlayerAccessor
import com.github.synnerz.devonian.utils.BlockTypes
import com.github.synnerz.devonian.utils.math.DDA
import net.minecraft.client.multiplayer.ClientChunkCache
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import kotlin.math.abs

object WorldUtils {
    val world: ClientLevel? get() = Devonian.minecraft.level
    val chunkManager: ClientChunkCache? get() = world?.chunkSource

    fun isChunkLoaded(x: Double, z: Double): Boolean = isChunkLoaded(x.toInt(), z.toInt())

    fun isChunkLoaded(x: Int, z: Int): Boolean {
        val player = Devonian.minecraft.player as? LocalPlayerAccessor ?: return false
        val cx = x shr 4
        val cz = z shr 4
        val pcx = player.lastXClient.toInt() shr 4
        val pcz = player.lastZClient.toInt() shr 4
        val c = chunkManager?.getChunk(x shr 4, z shr 4, false)
        val b1 = c != null && c.javaClass === LevelChunk::class.java
        val b2 = abs(cx - pcx) + abs(cz - pcz) <= 9
        return b1 && b2
    }

    fun fromBlockTypeOrNull(x: Double, y: Double, z: Double, blockType: Block): BlockState? =
        fromBlockTypeOrNull(x.toInt(), y.toInt(), z.toInt(), blockType)

    fun fromBlockTypeOrNull(x: Int, y: Int, z: Int, blockType: Block): BlockState? {
        val blockState = getBlockState(x, y, z) ?: return null
        if (blockState.block != blockType) return null

        return blockState
    }

    fun getBlockState(x: Double, y: Double, z: Double): BlockState? =
        getBlockState(x.toInt(), y.toInt(), z.toInt())

    fun getBlockState(x: Int, y: Int, z: Int): BlockState? =
        world?.getBlockState(BlockPos(x, y, z))

    fun getBlockId(block: Block): Int = BuiltInRegistries.BLOCK.indexOf(block)

    fun getBlockIdAt(x: Double, y: Double, z: Double): Int? {
        val blockState = getBlockState(x, y, z) ?: return null
        val block = blockState.block ?: return null

        return getBlockId(block)
    }

    fun registryName(block: Block): String {
        val registry = BuiltInRegistries.BLOCK.getKey(block)
        return "${registry.namespace}:${registry.path}"
    }

    fun raycast(
        x: Double, y: Double, z: Double,
        dx: Double, dy: Double, dz: Double,
        firstBlock: Boolean,
    ): BlockPos? {
        val w = world ?: return null

        for (bp in DDA(x, y, z, dx, dy, dz)) {
            val bs = w.getBlockState(bp)
            if (firstBlock && !bs.isAir) return bp
            if (!BlockTypes.AirLike.contains(bs.block)) return bp
        }

        return null
    }
}
