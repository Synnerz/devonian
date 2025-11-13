package com.github.synnerz.devonian

import com.github.synnerz.devonian.api.events.AreaEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.SubAreaEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.diana.BurrowGuesser
import com.github.synnerz.devonian.features.diana.BurrowWaypoint
import com.github.synnerz.devonian.features.diana.DianaDropTracker
import com.github.synnerz.devonian.features.diana.DianaMobTracker
import com.github.synnerz.devonian.features.dungeons.*
import com.github.synnerz.devonian.features.end.*
import com.github.synnerz.devonian.features.garden.GardenDisplay
import com.github.synnerz.devonian.features.garden.PestsDisplay
import com.github.synnerz.devonian.features.inventory.*
import com.github.synnerz.devonian.features.misc.*
import com.github.synnerz.devonian.features.slayers.BossSlainTime
import com.github.synnerz.devonian.features.slayers.BossSpawnTime
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.utils.JsonUtils
import com.github.synnerz.devonian.utils.Location
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.MinecraftClient
import org.slf4j.LoggerFactory

object Devonian : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("devonian")
	val minecraft = MinecraftClient.getInstance()
	val features = mutableListOf<Feature>()
	private val featureInstances = mutableListOf(
		NoCursorReset,
		BoxStarMob,
		RemoveBlockBreakParticle,
		RemoveExplosionParticle,
		RemoveFallingBlocks,
		RemoveFireOverlay,
		PreventPlacingWeapons,
		MiddleClickGui,
		ProtectItem,
		NoHurtCamera,
		RemoveLightning,
		HideInventoryEffects,
		BlockOverlay,
		HidePotionEffectOverlay,
		EtherwarpOverlay,
		PreventPlacingPlayerHeads,
		AutoRequeueDungeons,
		ExtraStats,
		NoDeathAnimation,
		RemoveFrontView,
		ChatWaypoint,
		RemoveChatLimit,
		CopyChat,
		WorldAge,
		MimicKilled,
		CryptsDisplay,
		DeathsDisplay,
		MilestoneDisplay,
		PuzzlesDisplay,
		RemoveTabPing,
		RemoveDamageTag,
		HideNoStarTag,
		CompactChat,
		GardenDisplay,
		PestsDisplay,
		BossSlainTime,
		BossSpawnTime,
		FactoryHelper,
		DungeonBreakerCharges,
		SecretsClickedBox,
		GolemWaypoint,
		EyesPlacedDisplay,
		PreviousLobby,
		GolemDPS,
		GolemLootQuality,
		GolemSpawnTimer,
		GolemStage5Sound,
		SecretsSound,
		LividSolver,
		RunSplits,
		BossSplits,
		PrinceKilled,
		BurrowWaypoint,
		DianaMobTracker,
		BurrowGuesser,
		DianaDropTracker,
		EtherwarpSound,
		InventoryHistoryLog
	)

	override fun onInitializeClient() {
		featureInstances.forEach(Feature::initialize)
		HudManager.initialize()
		JsonUtils.load()
		Config.initialize()
		Location.initialize()
		DevonianCommand.initialize()

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