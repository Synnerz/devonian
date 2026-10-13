package com.github.synnerz.devonian.features.dungeons.clear

import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.api.splits.TimeUnit
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.hud.texthud.SplitsFeature
import com.github.synnerz.devonian.utils.BasicState

object WatcherSplits : SplitsFeature(
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

    private val SETTING_SHOW_IN_BOSS = addSwitch(
        "showInBoss",
        false,
        "",
        "Show In Boss",
    )

    override fun getMaxName(): String = "Watcher Dialogue"

    override fun getSplits(style: TimeUnit.Style, force: TimeUnit?): List<String> =
        Stages.WatcherSplit.getSplits(style, force)

    override fun getChatSplits(style: TimeUnit.Style, force: TimeUnit?): List<String> =
        listOf(super.getChatSplits(style, force).joinToString(" &f| "))

    private var entityId = -1
    private var sent = false
    private var dialogTicks = -1

    override fun initialize() {
        super.initialize()

        Stages.WatcherSplit.feature = this

        on<NameChangeEvent> { event ->
            if (!event.name.contains(" The Watcher")) return@on
            entityId = event.entityId
        }

        on<ClientThreadServerTickEvent> {
            if (dialogTicks != -1) return@on
            val stage = Stages.WatcherDialog
            if (!stage.hasFinished()) return@on

            dialogTicks = EventBus.serverTicks()
        }

        on<ClientThreadServerTickEvent> {
            if (Stages.WatcherClear.hasFinished() && !sent) {
                sendMessageAll()
                sent = true
                return@on
            }

            if (Stages.WatcherDialog.isActive() && !Stages.WatcherMove.isActive()) {
                Stages.WatcherMove.start()
            }
            if (Stages.WatcherMove.hasStarted() && Stages.WatcherClear.hasFinished()) {
                Stages.WatcherMove.stop()
                return@on
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
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        entityId = -1
        dialogTicks = -1
        sent = false
        Stages.WatcherMove.reset()
    }
}