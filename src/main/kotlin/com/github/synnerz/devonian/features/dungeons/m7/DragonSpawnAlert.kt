package com.github.synnerz.devonian.features.dungeons.m7

import com.github.synnerz.devonian.api.ChatUtils
import com.github.synnerz.devonian.api.dungeon.Dungeons
import com.github.synnerz.devonian.api.dungeon.Stages
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.hud.texthud.Alert
import com.github.synnerz.devonian.utils.BasicState

object DragonSpawnAlert : Feature(
    "dragonSpawnAlert",
    "alerts with dragon spawning",
    Categories.M7,
    "catacombs",
    searchTags = setOf("priority", "dragprio"),
) {
    override fun createRequirements(): List<BasicState<Boolean>?> {
        return super.createRequirements() + listOf(Stages.WitherKing.isActiveState)
    }

    private val SETTING_ONLY_SPLIT = addSwitch(
        "onlySplit",
        false,
        "Only sends an alert for the first dragon.",
        "Only Alert Split",
    )
    private val SETTING_ALERT_TIME = addSlider(
        "duration",
        1000.0,
        0.0, 5000.0,
        "",
        "Alert Duration",
    )
    private val SETTING_SOUND = addSwitch(
        "sound",
        true,
        "",
        "Alert Sound",
    )
    private val SETTING_DO_SPLIT = addSwitch(
        "split",
        true,
        "do you go for split or not",
        "Do Split",
    )
    private val SETTING_SPLIT_PRIO = addTextInput(
        "splitPrio",
        "ogrbp",
        "bers team -> ogrbp <- arch team. only include rgpbo exactly once",
        "Split Priority",
    ).also {
        it.onChange {
            if (isValidPrio(it)) return@onChange
            ChatUtils.sendMessage("&4Invalid Prio; will produce undefined behavior.", true)
        }
    }
    private val SETTING_NO_SPLIT_PRIO = addTextInput(
        "noSplitPrio",
        "robpg",
        "bers team -> robpg <- arch team. only include rgpbo exactly once",
        "No Split Priority",
    ).also {
        it.onChange {
            if (isValidPrio(it)) return@onChange
            ChatUtils.sendMessage("&4Invalid Prio; will produce undefined behavior.", true)
        }
    }
    private val SETTING_BERS_TEAM = addTextInput(
        "bersTeam",
        "bmh",
        "classes that split with bers. only include abhmt",
        "Bers Team",
    )

    private fun isValidPrio(str: String) = str.toCharArray().sorted().joinToString("") == "bgopr"

    private var count = 0
    private var other: M7Dragon? = null

    override fun initialize() {
        EventBus.on<M7Events.DragonSpawned> { event ->
            count++
            if (count == 1) {
                other = event.dragon
                return@on
            }

            var drag = event.dragon
            if (count == 2) {
                other?.let { drag2 ->
                    val role = Dungeons.players.firstEntry()?.value?.role ?: return@let
                    val prio = if (SETTING_DO_SPLIT.get()) SETTING_SPLIT_PRIO.get() else SETTING_NO_SPLIT_PRIO.get()
                    val team = if (SETTING_DO_SPLIT.get()) SETTING_BERS_TEAM.get() else "bmhat"
                    val i1 = prio.indexOf(drag.singleLetter)
                    val i2 = prio.indexOf(drag2.singleLetter)
                    if (team.contains(role.singleLetter) == i2 < i1) drag = drag2
                }
            }

            M7Events.DragonSpawned2(drag, event.isHigh).post()

            if (!isEnabled()) return@on
            if (count != 2 && SETTING_ONLY_SPLIT.get()) return@on

            Alert.show(
                "&l${drag.textColor}${drag.name}${if (event.isHigh) " HIGH" else ""}",
                SETTING_ALERT_TIME.get().toInt(),
                SETTING_SOUND.get()
            )
        }.setEnabled(enabledState ?: BasicState(true))

        EventBus.on<WorldChangeEvent> {
            count = 0
            other = null
        }
    }
}