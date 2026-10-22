package com.github.synnerz.devonian.features.dungeons.f7

import com.github.synnerz.devonian.api.dungeon.DungeonStages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Items
import java.util.UUID

object GoldorFrenzyTimer : TextHudFeature(
    "goldorFrenzyTimer",
    "timer until goldor damage tick",
    Categories.F7,
    "catacombs",
    subcategory = "HUD",
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(
            DungeonStages.F7.isActiveState, DungeonStages.Terminals.hasFinishedState.map(
            Boolean::not))
    }

    private val SETTING_SHOW_TOTAL = addSwitch(
        "showTotal",
        false,
        "",
        "Show Cumulative Time",
    )
    private val SETTING_USE_ALTERNATIVE_SCANNER = addSwitch(
        "useAlternativeScanner",
        false,
        "Uses an alternative scanner for storm dying, which might be inaccurate",
        "Alternative Scanner"
    )

    private val BUNK_UUID = UUID.fromString("f8e5fbc7-af95-3faa-93ab-1604bdd7e3cb")
    private var preGoldor = false
    private var inGoldor = false
    private var until = 0
    private var preGoldorTicks = 0
    private var crushes = 0

    override fun initialize() {
        on<ChatEvent> { event ->
            when (event.message) {
                "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                    inGoldor = true
                    preGoldor = false
                    until = 60
                    preGoldorTicks = 0
                }
                "[BOSS] Storm: I should have known that I stood no chance." -> {
                    if (preGoldor && SETTING_USE_ALTERNATIVE_SCANNER.get()) return@on
                    preGoldor = true
                    preGoldorTicks = 100
                }

                "The Core entrance is opening!" -> inGoldor = false
            }
        }

        on<ClientThreadServerTickEvent> {
            if (preGoldor) {
                val time = preGoldorTicks-- * 0.05
                setLine(
                    "%s%.2f".format(
                        StringUtils.colorForNumber(preGoldorTicks, 100),
                        time
                    )
                )
                if (time < 0) {
                    preGoldor = false
                    preGoldorTicks = 0
                }
                return@on
            }
            if (!inGoldor) return@on
            until = if (until > 1) until - 1 else 60
            setLine(
                "%s%.2f".format(
                    StringUtils.colorForNumber(until, 60),
                    if (SETTING_SHOW_TOTAL.get()) (EventBus.serverTicks() - DungeonStages.Terminals.startTime.tick) * 0.05
                    else until * 0.05
                )
            )
        }

        on<EntityEquipmentEvent> { event ->
            if (event.type != EntityTypes.ARMOR_STAND) return@on
            if (event.slots.size != 1) return@on
            val entry = event.slots.firstOrNull() ?: return@on
            if (entry.first !== EquipmentSlot.HEAD) return@on

            val item = entry.second ?: return@on
            if (item.item !== Items.PLAYER_HEAD) return@on

            val profile = item.get(DataComponents.PROFILE) ?: return@on
            val id = profile.partialProfile().id ?: return@on

            if (id != BUNK_UUID) return@on

            crushes++
            if (crushes != 2) return@on

            preGoldor = true
            preGoldorTicks = 100
        }.setEnabled(SETTING_USE_ALTERNATIVE_SCANNER.state)

        on<RenderOverlayEvent> { event ->
            if (preGoldor || inGoldor) draw(event.ctx)
        }
    }

    override fun getEditText(): List<String> = listOf("${StringUtils.colorForNumber(1.95, 3.0)}1.95")

    override fun onWorldChange(event: WorldChangeEvent) {
        inGoldor = false
        preGoldor = false
        until = 0
        preGoldorTicks = 0
        crushes = 0
    }
}