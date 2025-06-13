package com.github.synnerz.devonian

import com.github.synnerz.devonian.features.dungeons.BoxStarMob
import com.github.synnerz.devonian.features.misc.NoCursorReset
import com.github.synnerz.devonian.features.misc.RemoveParticles
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

object Devonian : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("devonian")
	val minecraft = MinecraftClient.getInstance()

	override fun onInitializeClient() {
		NoCursorReset.initialize()
		BoxStarMob.initialize()
		RemoveParticles.initialize()
	}
}