package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.client.world.ClientChunkManager
import net.minecraft.client.world.ClientWorld
import net.minecraft.registry.Registries
import net.minecraft.util.math.BlockPos

object WorldUtils {
    val world: ClientWorld? get() = Devonian.minecraft.world
    val chunkManager: ClientChunkManager? get() = world?.chunkManager

    fun isChunkLoaded(x: Double, z: Double): Boolean = isChunkLoaded(x.toInt(), z.toInt())

    fun isChunkLoaded(x: Int, z: Int): Boolean = chunkManager?.isChunkLoaded(x shr 4, z shr 4) ?: false

    fun getBlockState(x: Double, y: Double, z: Double): BlockState? =
        getBlockState(x.toInt(), y.toInt(), z.toInt())

    fun getBlockState(x: Int, y: Int, z: Int): BlockState? =
        world?.getBlockState(BlockPos(x, y, z))

    fun getBlockId(block: Block): Int = Registries.BLOCK.indexOf(block)

    fun getBlockIdAt(x: Double, y: Double, z: Double): Int? {
        val blockState = getBlockState(x, y, z) ?: return null
        val block = blockState.block ?: return null

        return getBlockId(block)
    }

    fun registryName(block: Block): String {
        val registry = Registries.BLOCK.getId(block)
        return "${registry.namespace}:${registry.path}"
    }
}
