package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.Location
import com.github.synnerz.devonian.utils.State
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Zombie
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object Dungeons {
    private val playerInfoRegex = "^\\[\\d+] (\\w+)(?:.+?)? \\((\\w+) ?([IVXLCDM]+)?\\)$".toRegex()
    private val dungeonFloorRegex = "^ * ⏣ The Catacombs \\((\\w+)\\)$".toRegex()
    private val bossMessageRegex = "^\\[BOSS] (.+?): (.+?)$".toRegex()

    private val clearedPercentRegex = "^Cleared: (\\d+)% \\(\\d+\\)$".toRegex()
    private val timeElapsedRegex = "^Time Elapsed: (?:(\\d+)h)? ?(?:(\\d+)m)? ?(\\d+)s$".toRegex()
    private val teamDeathsRegex = "^Team Deaths: (\\d+)$".toRegex()
    private val cryptsRegex = "^ Crypts: (\\d+)$".toRegex()
    private val secretsFoundPercentRegex = "^ Secrets Found: ([\\d.]+)%$".toRegex()
    private val secretsFoundRegex = "^ Secrets Found: (\\d+)$".toRegex()
    private val completedRoomsRegex = "^ Completed Rooms: (\\d+)$".toRegex()
    private val openedRoomsRegex = "^ Opened Rooms: (\\d+)$".toRegex()
    private val discoveriesRegex = "^Discoveries: (\\d+)$".toRegex()
    private val puzzleNameRegex = "^ ([\\w ]+): \\[([✦✔✖])] ?\\(?(\\w+)?\\)?$".toRegex()
    private val puzzlesCountRegex = "^Puzzles: \\((\\d+)\\)$".toRegex()

    val players = linkedMapOf<String, DungeonPlayer>()
    val playerClasses = ConcurrentHashMap<String, DungeonClass>()
    private var needReset = true

    var floor = FloorType.None
    val floorState = BasicState(FloorType.None)

    // [0, 100]
    val clearedPercent = BasicState(0)
    val timeElapsed = BasicState(0)
    val deaths = BasicState(0)
    val hasSpirit = BasicState(true)
    val crypts = BasicState(0)

    // [0, 100]
    val secretsFoundPercent = BasicState(0.0)
    val secretsFound = BasicState(0)
    val completedRooms = BasicState(0)
    val openedRooms = BasicState(0)
    val discoveries = BasicState(0)
    val totalPuzzles = BasicState(0)
    val completedPuzzles = BasicState(0)
    val mimicKilled = BasicState(false)
    val princeKilled = BasicState(false)
    val isPaul = BasicState(false)
    val inBoss = BasicState(false)
    val bloodCleared = BasicState(false)
    val started = BasicState(false)

    private fun fuckEntrance(score: State<Double>) =
        score.zip(floorState) { score, floor -> (score * (if (floor == FloorType.Entrance) 0.7 else 1.0)).toInt() }

    // https://github.com/Skytils/SkytilsMod/blob/2e32484d011000f8d618401fe9675234969ab23e/mod/src/main/kotlin/gg/skytils/skytilsmod/features/impl/dungeons/ScoreCalculation.kt
    val totalRoomHisto = mutableMapOf<Int, Int>()
    val totalRooms = clearedPercent.zip(completedRooms) { clear, rooms ->
        if (clear == 0 || rooms == 0) return@zip 0
        val guess = (100.0 * rooms / clear + 0.5).toInt()
        totalRoomHisto[guess] = (totalRoomHisto[guess] ?: 0) + 1
        totalRoomHisto.toList().maxBy { it.second * 1000 + it.first }.first
    }

    val totalSecrets = secretsFound.zip(secretsFoundPercent) { found, percent ->
        if (found == 0 || percent == 0.0) 0
        else (100.0 / percent * found + 0.5).toInt()
    }
    val totalSecretsRequired = floorState.zip(totalSecrets) { floor, totalSecrets ->
        ceil(floor.requiredPercent * totalSecrets).toInt()
    }
    // [0, 1]
    val actualSecretPercent = secretsFound.zip(totalSecretsRequired) { found, total ->
        if (total == 0) 0.0 else min(found.toDouble() / total, 1.0)
    }
    val secretScore = actualSecretPercent.map { it * 40.0 }

    val actualCompletedRooms = BasicState(0)

    // [0, 1]
    val actualClearPercent = actualCompletedRooms.zip(totalRooms) { completed, total ->
        if (total > 0) min(completed.toDouble() / total, 1.0) else 0.0
    }
    val roomClearScore = actualClearPercent.map { it * 60.0 }

    private val discoveryScore_ = secretScore.zip(roomClearScore) { a, b -> a + b }
    val exploreScore = fuckEntrance(discoveryScore_)

    val deathPenalty = deaths.zip(hasSpirit) { deaths, spirit ->
        if (deaths == 0) 0
        else -2 * deaths + (if (spirit) 1 else 0)
    }
    val puzzlePenalty = completedPuzzles.zip(totalPuzzles) { completed, total ->
        10 * (total - completed)
    }
    val totalPenalty = deathPenalty.zip(puzzlePenalty) { a, b -> a + b }
    private val skillScore_ = actualClearPercent.zip(totalPenalty) { clear, penalty ->
        max(20.0 + clear * 80.0 - penalty, 20.0)
    }
    val skillScore = fuckEntrance(skillScore_)

    private val speedScore_ = timeElapsed.zip(floorState) { time, floor ->
        val overtime = time - floor.requiredSpeed
        when {
            overtime < 12 -> 100.0
            overtime < 120 -> 100.0 - overtime / 12.0
            overtime < 360 -> 91.0 - overtime / 24.0
            overtime < 660 -> 92.0 - overtime / 30.0
            overtime < 3090 -> 86.5 - overtime / 40.0
            else -> 0.0
        }
    }
    val speedScore = fuckEntrance(speedScore_)

    private val bonusScore_ = crypts.zip(mimicKilled) { crypts, mimic ->
        min(crypts, 5) + (if (mimic) 2 else 0)
    }.zip(princeKilled) { score, prince ->
        score + (if (prince) 1 else 0)
    }.zip(isPaul) { score, paul ->
        score + (if (paul) 10 else 0).toDouble()
    }
    val bonusScore = fuckEntrance(bonusScore_)

    val score = exploreScore.zip(skillScore) { a, b -> a + b }
        .zip(speedScore) { a, b -> a + b }
        .zip(bonusScore) { a, b -> a + b }

    val _targetScore = floorState.zip(isPaul) { state, paul -> 40 - (1 + (if (floor.floorNum >= 6) 7 else 5) + (if (paul) 10 else 0)) }
    val minSecrets = _targetScore.zip(totalSecretsRequired) { score, secrets -> ceil(score * secrets / 40.0).toInt() }
    val remainingMinSecrets = minSecrets.zip(secretsFound) { total, found -> max(0, total - found) }

    fun initialize() {
        DungeonScanner.init()
    }

    init {
        EventBus.on<TabUpdateEvent> { event ->
            event.matches(cryptsRegex)?.let {
                crypts.value = it[0].toInt()
                return@on
            }

            event.matches(teamDeathsRegex)?.let {
                deaths.value = it[0].toInt()
                return@on
            }

            event.matches(secretsFoundPercentRegex)?.let {
                secretsFoundPercent.value = it[0].toDouble()
                return@on
            }

            event.matches(completedRoomsRegex)?.let {
                completedRooms.value = it[0].toInt()
                actualCompletedRooms.value = it[0].toInt() +
                    (if (inBoss.value) 0 else 1) +
                    (if (bloodCleared.value) 0 else 1)
                return@on
            }

            event.matches(secretsFoundRegex)?.let {
                secretsFound.value = it[0].toInt()
                return@on
            }

            event.matches(openedRoomsRegex)?.let {
                openedRooms.value = it[0].toInt()
                return@on
            }

            event.matches(discoveriesRegex)?.let {
                discoveries.value = it[0].toInt()
                return@on
            }

            event.matches(puzzlesCountRegex)?.let {
                totalPuzzles.value = it[0].toInt()
                return@on
            }

            event.matches(puzzleNameRegex)?.let {
                if (it[1].isEmpty() || it[1] != "✔") return@on
                completedPuzzles.value = min(completedPuzzles.value + 1, totalPuzzles.value)
                return@on
            }

            val match = event.matches(playerInfoRegex) ?: return@on
            val (name, role) = match

            val player = players.getOrPut(name) {
                playerClasses[name] = DungeonClass.Unknown
                DungeonPlayer(
                    name,
                    DungeonClass.Unknown,
                    0,
                    false
                )
            }

            if (role == "DEAD") player.isDead = true
            else {
                player.isDead = false
                val c = DungeonClass.from(role)
                player.role = c
                playerClasses[name] = c

                val level = match.getOrNull(2)
                if (level != null) player.classLevel = StringUtils.parseRoman(level)
            }
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<TickEvent> {
            val mc = Devonian.minecraft

            // TODO: check when each player is being updated by the server
            // players.forEach { it.value.tick() }
            players.firstEntry()?.value?.tick()

            mc.level?.players()?.forEach {
                val ping = mc.connection?.getPlayerInfo(it.uuid)?.latency ?: return@forEach
                if (ping == -1) return@forEach

                val player = players[it.name.string] ?: return@forEach
                player.entity = it
            }
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<AreaEvent> { event ->
            val area = event.area
            if (area == null || area != "Catacombs") {
                if (!needReset) return@on
                players.clear()
                playerClasses.clear()
                DungeonScanner.reset()
                DungeonMapScanner.reset()
                reset()
                needReset = false
            } else needReset = true
        }

        EventBus.on<WorldChangeEvent> {
            players.clear()
            playerClasses.clear()
            DungeonScanner.reset()
            DungeonMapScanner.reset()
            reset()
            needReset = false
        }

        EventBus.on<ScoreboardEvent> { event ->
            event.matches(dungeonFloorRegex)?.let {
                floor = FloorType.from(it[0])
                floorState.value = floor
                DungeonEvent.FloorEnter(floor).post()
                return@on
            }
            if (Location.area != "catacombs") return@on
            val matchesTime = event.matches(timeElapsedRegex)
            if (matchesTime != null) {
                val hours = matchesTime[0].ifEmpty { "0" }.toInt()
                val minutes = matchesTime[1].ifEmpty { "0" }.toInt()
                val seconds = matchesTime[2].ifEmpty { "0" }.toInt()
                timeElapsed.value = hours * 3600 + minutes * 60 + seconds
                return@on
            }

            val match = event.matches(clearedPercentRegex) ?: return@on
            clearedPercent.value = match[0].toInt()
        }

        EventBus.on<ChatChannelEvent.PartyChatEvent> { event ->
            when (event.message) {
                "Mimic Killed!",
                "\$SKYTILS-DUNGEON-SCORE-MIMIC$"
                    -> mimicKilled.value = true

                "Prince Killed!"
                    -> princeKilled.value = true
            }
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<ChatEvent> { event ->
            if (event.message == "[NPC] Mort: Here, I found this map when I first entered the dungeon.") {
                started.value = true
                DungeonEvent.RunStarted().post()
                return@on
            }

            if (event.message == "A Prince falls. +1 Bonus Score") {
                princeKilled.value = true
                DungeonEvent.PrinceKilled().post()
                return@on
            }

            val (name, message) = event.matches(bossMessageRegex) ?: return@on
            val boss = DungeonBoss.from(name) ?: return@on

            if (boss == DungeonBoss.Scarf) {
                if (message == "How can you move forward when you keep regretting the past?") return@on
                if (message == "If you win, you live. If you lose, you die. If you don't fight, you can't win.") return@on
                if (message == "If I had spent more time studying and less time watching anime, maybe mother would be here with me!") return@on
                if (message == "Wither Ultra!") return@on
            }

            DungeonEvent.BossMessageEvent(boss, message).post()
            if (boss != DungeonBoss.Watcher) inBoss.value = true
            else if (message == "You have proven yourself. You may pass.") bloodCleared.value = true
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<EntityDeathEvent> { event ->
            val entity = event.entity as LivingEntity
            if (entity !is Zombie) return@on
            if (!entity.isBaby || entity.hasItemInSlot(EquipmentSlot.HEAD)) return@on

            mimicKilled.value = true
            DungeonEvent.MimicKilled().post()
        }.setEnabled(Location.stateInArea("catacombs"))
    }

    private fun reset() {
        floor = FloorType.None

        clearedPercent.value = 0
        timeElapsed.value = 0
        deaths.value = 0
        hasSpirit.value = true
        crypts.value = 0
        secretsFoundPercent.value = 0.0
        secretsFound.value = 0
        completedRooms.value = 0
        openedRooms.value = 0
        discoveries.value = 0
        totalPuzzles.value = 0
        completedPuzzles.value = 0
        mimicKilled.value = false
        princeKilled.value = false
        inBoss.value = false
        bloodCleared.value = false
        started.value = false

        totalRoomHisto.clear()
    }

    enum class DungeonBoss(val displayName: String) {
        Watcher("The Watcher"),
        Bonzo("Bonzo"),
        Scarf("Scarf"),
        Professor("The Professor"),
        Thorn("Thorn"),
        Livid("Livid"),
        Sadan("Sadan"),
        Maxor("Maxor"),
        Storm("Storm"),
        Goldor("Goldor"),
        Necron("Necron"),
        WitherKing("Wither King");

        companion object {
            private val map = entries.associateBy { it.displayName }
            fun from(name: String) = map[name]
        }
    }
}