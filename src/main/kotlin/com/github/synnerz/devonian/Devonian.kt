package com.github.synnerz.devonian

import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.TextConfig
import com.github.synnerz.devonian.config.ui.talium.ConfigGui
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.HudManagerHider
import com.github.synnerz.devonian.features.HudManagerInstructions
import com.github.synnerz.devonian.features.HudManagerRenderer
import com.github.synnerz.devonian.features.diana.BurrowGuesser
import com.github.synnerz.devonian.features.diana.BurrowWaypoint
import com.github.synnerz.devonian.features.diana.DianaDropTracker
import com.github.synnerz.devonian.features.diana.DianaMobTracker
import com.github.synnerz.devonian.features.dungeons.*
import com.github.synnerz.devonian.features.dungeons.map.DungeonMap
import com.github.synnerz.devonian.features.dungeons.solvers.*
import com.github.synnerz.devonian.features.end.*
import com.github.synnerz.devonian.features.garden.GardenDisplay
import com.github.synnerz.devonian.features.garden.PestsDisplay
import com.github.synnerz.devonian.features.inventory.*
import com.github.synnerz.devonian.features.misc.*
import com.github.synnerz.devonian.features.slayers.BossSlainTime
import com.github.synnerz.devonian.features.slayers.BossSpawnTime
import com.github.synnerz.devonian.hud.HudManager
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.Location
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory

object Devonian : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("devonian")
    val minecraft = Minecraft.getInstance()
    val keybindCategory by lazy {
        KeyMapping.Category.register(
            ResourceLocation.fromNamespaceAndPath(
                "devonian",
                "keybinds"
            )
        )
    }
    val features = mutableListOf<Feature>()
    private val featureInstances = listOf(
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
        InventoryHistoryLog,
        HudManagerInstructions,
        DungeonMap,
        SpeedDisplay,
        BoxDoors,
        ScoreDisplay,
        EtherwarpOverlayFailReason,
        DisableChatAutoScroll,
        DisableBlindness,
        DisableAttachedArrows,
        DisableVignette,
        DisableWaterOverlay,
        DisableSuffocatingOverlay,
        BoulderSolver,
        ThreeWeirdosSolver,
        PingDisplay,
        BoxIcedMobs,
        BlazeSolver,
        DisableVanillaArmor,
        AccurateAbsorption,
        ChangeCrouchHeight,
        DisableFog,
        KeyPickup,
        CreeperBeamsSolver,
        // SimonSaysSolver,
        ArrowAlignSolver,
        CurrentRoomName,
        CurrentRoomCleared,
        TeleportMazeSolver,
        TriviaSolver,
        IcePathSolver,
        TicTacToeSolver,
        WaterBoardSolver,
        ScoreAlert,
        GoldorFrenzyTimer,
        QuiverDisplay,
        ChestProfit,
        SlotBinding,
        SlotLocking,
        HideEntityFire,
        ThirdPersonCrosshair,
        BossBarHealth,
        DungeonWaypoints,
        Fullbright,
        SpotifyDisplay,
        RemoveRecipeBook,
        RemoveContainerBackground,
        CustomContainerColor,
        HudManagerHider,
        BoxMimicChest,
        NoAbilityCdSound,
        DisableSwim,
        CenteredCrosshair,
        DisableEnderPearlCooldown,
        HudManagerRenderer,
        DisableWorldLoadingScreen,
        HighlightDroppedItems,
        DisableHungerBar,
        FixRedVignette,
        HideCraftingText,
        HideOffhandSlotBackground,
        AutoArchitectDraft,
    )

    override fun onInitializeClient() {
        featureInstances.forEach(Feature::initialize)
        ConfigGui.initialize()
        HudManager.initialize()
        KeyShortcuts.initialize()
        CommandAliases.initialize()
        Config.onAfterLoad {
            featureInstances.forEach { feature ->
                Config.getConfig<Boolean>(feature.configName)?.let {
                    feature.configSwitch.set(it)
                }
            }
        }
        Config.load()
        SkyblockPrices.initialize()
        TextConfig.initialize()
        Location.initialize()
        Alert.initialize()
        Dungeons.initialize()
        DevonianCommand.initialize()
    }
}