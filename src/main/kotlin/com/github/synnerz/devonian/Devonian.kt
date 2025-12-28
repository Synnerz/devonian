package com.github.synnerz.devonian

import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.SkyblockPrices
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.events.ChatEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.TextConfig
import com.github.synnerz.devonian.config.ui.talium.ConfigGui
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.features.HudManagerHider
import com.github.synnerz.devonian.features.HudManagerInstructions
import com.github.synnerz.devonian.features.HudManagerRenderer
import com.github.synnerz.devonian.features.debug.CopyItem
import com.github.synnerz.devonian.features.debug.WAILA
import com.github.synnerz.devonian.features.debug.packetlogger.PacketLogger
import com.github.synnerz.devonian.features.debug.renderers.DungeonRoomComponentRenderer
import com.github.synnerz.devonian.features.debug.renderers.RenderSlotIndex
import com.github.synnerz.devonian.features.diana.BurrowGuesser
import com.github.synnerz.devonian.features.diana.BurrowWaypoint
import com.github.synnerz.devonian.features.diana.DianaDropTracker
import com.github.synnerz.devonian.features.diana.DianaMobTracker
import com.github.synnerz.devonian.features.dungeons.*
import com.github.synnerz.devonian.features.dungeons.clear.AutoArchitectDraft
import com.github.synnerz.devonian.features.dungeons.clear.BoxDoors
import com.github.synnerz.devonian.features.dungeons.clear.BoxIcedMobs
import com.github.synnerz.devonian.features.dungeons.clear.BoxMimicChest
import com.github.synnerz.devonian.features.dungeons.clear.BoxStarMob
import com.github.synnerz.devonian.features.dungeons.clear.CloseChestOnKey
import com.github.synnerz.devonian.features.dungeons.clear.ColorPortal
import com.github.synnerz.devonian.features.dungeons.clear.CreeperBeamsDing
import com.github.synnerz.devonian.features.dungeons.clear.CryptsDisplay
import com.github.synnerz.devonian.features.dungeons.clear.CurrentRoomCleared
import com.github.synnerz.devonian.features.dungeons.clear.CurrentRoomName
import com.github.synnerz.devonian.features.dungeons.clear.DungeonWaypoints
import com.github.synnerz.devonian.features.dungeons.clear.KeyPickup
import com.github.synnerz.devonian.features.dungeons.clear.MimicKilled
import com.github.synnerz.devonian.features.dungeons.clear.PrinceKilled
import com.github.synnerz.devonian.features.dungeons.clear.PuzzlesDisplay
import com.github.synnerz.devonian.features.dungeons.clear.RunSplits
import com.github.synnerz.devonian.features.dungeons.clear.ScoreAlert
import com.github.synnerz.devonian.features.dungeons.clear.SecretsClickedBox
import com.github.synnerz.devonian.features.dungeons.clear.SecretsHud
import com.github.synnerz.devonian.features.dungeons.m7.DragonBoxes
import com.github.synnerz.devonian.features.dungeons.m7.DragonHealth
import com.github.synnerz.devonian.features.dungeons.m7.DragonHitCount
import com.github.synnerz.devonian.features.dungeons.m7.DragonSpawnAlert
import com.github.synnerz.devonian.features.dungeons.m7.DragonSpawnTimer
import com.github.synnerz.devonian.features.dungeons.m7.HideDyingDragons
import com.github.synnerz.devonian.features.dungeons.m7.RecolorDragons
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
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.slf4j.LoggerFactory
import java.util.*

object Devonian : ClientModInitializer {
    private val logger = LoggerFactory.getLogger("devonian")
    val minecraft = Minecraft.getInstance()
    val isDev = setOf(
        UUID.fromString("21c82573-9d28-4d7b-957f-adf20938cd38"),
        UUID.fromString("819d8402-51eb-4c0c-bcf2-d070dcb82a93"),
    ).contains(minecraft.gameProfile.id)
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
        SimonSaysSolver,
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
        DoubleAnimationFix,
        SelectedItemName,
        PurplePadTimer,
        CancelF7BossSounds,
        ProtectStarredItems,
        TpsDisplay,
        LagDisplay,
        HideFairy,
        HideSoulweaverSkulls,
        HideArcherPassive,
        HideWitherKing,
        HideHealerOrbs,
        HideHypeHearts,
        CloseChestOnKey,
        IceFillSolver,
        SpiritLeapKeys,
        LividInvulnerable,
        ItemRarityBackground,
        PuzzleTimers,
        HideCheapCoins,
        DisableNametagBackground,
        DisableTextShadow,
        HideGroundedArrows,
        BonzoMask,
        SpiritMask,
        PhoenixTimer,
        CreeperBeamsDing,
        FixObfuscatedText,
        CustomMageBeam,
        CustomSidebarColor,
        SecretsHud,
        CustomDefenseHud,
        CustomManaUseHud,
        CroesusProfit,
        WardrobeKeybinds,
        FireFreezeTimer,
        ItemAnimations,
        ScrollableTooltip,
        CustomLeapGui,
        NametagShadow,
        CustomHypeSound,
        Deployables,
        TerminalSolvers,
        RelicTimer,
        CustomTerminalScale,
        ItemValue,
        CroesusHighlightUnopened,
        CancelIncorrectSound,
        PetDisplay,
        ShowSelectedPet,
        HighlightSellableItems,
        InventoryScale,
        FPSDisplay,
        SimonSaysProgressDisplay,
        DragonBoxes,
        DragonSpawnTimer,
        DragonSpawnAlert,
        RecolorDragons,
        HideDyingDragons,
        DragonHealth,
        DragonHitCount,
        ColorPortal,
        RemoveGlowEffect,
        PartyFinderHighlight,
        ArmorDisplay,
        SidebarTextShadow,
        SharpShooterSolver,

        // Debug
        CopyItem,
        RenderSlotIndex,
        PacketLogger,
        DungeonRoomComponentRenderer,
        WAILA,
    )

    override fun onInitializeClient() {
        featureInstances.forEach(Feature::preinitialize)
        featureInstances.forEach(Feature::initialize)
        ConfigGui.initialize()
        HudManager.initialize()
        KeyShortcuts.initialize()
        CommandAliases.initialize()
        RefillGFSCommands.initialize()
        CancelMessages.initialize()
        TitleMessages.initialize()
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
        PreventItem.initialize()
        Dungeons.initialize()

        DevonianCommand.command.subcommand("sim") { _, args ->
            val msg = args.joinToString(" ") { it.toString() }
            ChatEvent(msg, Component.literal(msg)).post()
            return@subcommand 1
        }.greedyString("message")

        DevonianCommand.initialize()
    }
}