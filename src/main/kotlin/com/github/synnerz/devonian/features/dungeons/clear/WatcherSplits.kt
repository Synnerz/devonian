package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.ClientThreadServerTickEvent
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.BasicState

object WatcherSplits : TextHudFeature(
    "watcherSplits",
    "Displays Dialog Time, Watcher Move and Blood Clear",
    Categories.DUNGEONS,
    "catacombs",
    subcategory = "HUD",
    searchTags = setOf("dungeons", "timer"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.Clear.isActiveState.zip(SETTING_SHOW_IN_BOSS.state, Boolean::or))
    }

    private val SETTING_FORMAT = addSelection(
        "format",
        0,
        listOf("Real Time", "Server Ticks", "Both"),
        "",
        "Time Format",
    )
    private val SETTING_SHOW_IN_BOSS = addSwitch(
        "showInBoss",
        false,
        "",
        "Show In Boss",
    )

    private var entityId = -1
    private var sent = false
    private var dialogTicks = -1

    override fun initialize() {
        on<NameChangeEvent> { event ->
            if (!event.name.contains(" The Watcher ")) return@on
            entityId = event.entityId
        }

        on<ClientThreadServerTickEvent> {
            if (dialogTicks != -1) return@on
            val stage = Stages.WatcherDialog
            if (!stage.hasFinished()) return@on

            dialogTicks = EventBus.serverTicks()
        }

        on<TickEvent> {
            if (Stages.WatcherClear.hasFinished() && !sent) {
                Stages.WatcherSplit.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()]).forEach {
                    ChatUtils.sendMessage(it)
                }
                sent = true
                return@on
            }

            if (Stages.WatcherDialog.isActive() && !Stages.WatcherMove.isActive()) {
                Stages.WatcherMove.start()
            }
            if (entityId == -1) return@on
            if (!Stages.WatcherDialog.hasFinished() || Stages.WatcherMove.hasFinished() || dialogTicks == -1) return@on
            if (EventBus.serverTicks() - dialogTicks < 45) return@on

            val entity = minecraft.level?.getEntity(entityId - 1) ?: return@on
            if (
                entity.xo == entity.x &&
                entity.yo == entity.y &&
                entity.zo == entity.z
            ) return@on
            Stages.WatcherMove.stop()
        }

        on<RenderOverlayEvent> {
            setLines(Stages.WatcherSplit.getSplits(TimeUnit.Format.entries[SETTING_FORMAT.get()]))
            draw(it.ctx)
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        entityId = -1
        dialogTicks = -1
        sent = false
        Stages.WatcherMove.reset()
    }

    override fun getEditText(): List<String> = Stages.WatcherSplit.getSplits(TimeUnit.Format.Both, TimeUnit.now())
}