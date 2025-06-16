package com.github.synnerz.devonian

import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.events.AreaEvent
import com.github.synnerz.devonian.events.EventBus
import com.github.synnerz.devonian.events.SubAreaEvent
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.dungeons.BoxStarMob
import com.github.synnerz.devonian.features.misc.*
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Location
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

object Devonian : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("devonian")
	val minecraft = MinecraftClient.getInstance()
	val features = mutableListOf<Feature>()

	override fun onInitializeClient() {
		NoCursorReset.initialize()
		BoxStarMob.initialize()
		RemoveBlockBreakParticle.initialize()
		RemoveExplosionParticle.initialize()
		RemoveFallingBlocks.initialize()
		RemoveFireOverlay.initialize()
		JsonUtils.load()
		Config.initialize()
		Location.initialize()

		EventBus.on<AreaEvent> {
			for (feat in features)
				feat.onToggle(feat.isEnabled())
		}

		EventBus.on<SubAreaEvent> {
			for (feat in features)
				feat.onToggle(feat.isEnabled())
		}
	}
}