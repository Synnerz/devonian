package com.github.synnerz.devonian.api.dungeon

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.ItemUtils
import com.github.synnerz.devonian.api.Location
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.*
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.features.dungeons.SecretsSound
import com.github.synnerz.devonian.features.dungeons.m7.M7Events
import com.github.synnerz.devonian.utils.BasicState
import com.github.synnerz.devonian.utils.State
import com.github.synnerz.devonian.utils.StringUtils
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ambient.Bat
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.item.alchemy.Potions
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.SkullBlockEntity
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object Dungeons {
    private val playerInfoRegex = "^\\[\\d+] (\\w+)(?:.+?)? \\((\\w+) ?([IVXLCDM]+)?\\)$".toRegex()
    private val dungeonFloorRegex = "^ * ⏣ The Catacombs \\((\\w+)\\)$".toRegex()
    private val bossMessageRegex = "^\\[BOSS] (.+?): (.+?)$".toRegex()
    private val disconnectRegex = "^ ☠ (\\w+) disconnected and became a ghost\\.$".toRegex()
    private val reconnectRegex = "^ ☠ (\\w+) reconnected\\.$".toRegex()

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

    private var wasInDungeons = false
    private val attemptGuessState = BasicState(false)

    var selfPlayer = Devonian.minecraft.let { mc ->
        val prof = mc.gameProfile
        return@let DungeonPlayer(
            prof.name,
            PlayerInfo(prof, false),
            DungeonClass.Unknown,
            0,
            false,
        )
    }
    private var scannedSelf = false
    val players = linkedMapOf(selfPlayer.name to selfPlayer)
    val playerClasses = ConcurrentHashMap(mapOf(selfPlayer.name to selfPlayer.role))
    val selfClass = BasicState(selfPlayer.role)
    private var needReset = true
    private var worldId = 0

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
    val totalRoomSecrets = BasicState(0)

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

    val exploreScore = fuckEntrance(secretScore).zip(fuckEntrance(roomClearScore), Int::plus)

    val deathPenalty = deaths.zip(hasSpirit) { deaths, spirit ->
        if (deaths == 0) 0
        else 2 * deaths - (if (spirit) 1 else 0)
    }
    val puzzlePenalty = completedPuzzles.zip(totalPuzzles) { completed, total ->
        10 * (total - completed)
    }
    val totalPenalty = deathPenalty.zip(puzzlePenalty, Int::plus)
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

    val score = exploreScore.zip(skillScore, Int::plus)
        .zip(speedScore, Int::plus)
        .zip(bonusScore, Int::plus)

    val _targetScore = bonusScore_.map { 40 - it }
    val minSecrets = _targetScore.zip(totalSecretsRequired) { score, secrets -> ceil(score * secrets / 40.0).toInt() }
    val remainingMinSecrets = minSecrets.zip(secretsFound) { total, found -> max(0, total - found) }

    fun initialize() {
        DungeonScanner.init()
        M7Events.init()

        DevonianCommand.command.subcommand("setfloor") { _, args ->
            val f = FloorType.entries.find { it.shortName == args[0] } ?: return@subcommand 1
            floor = f
            floorState.value = f
            return@subcommand 0
        }.string("floor").suggest("floor", "E", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "M1", "M2", "M3", "M4", "M5", "M6", "M7")
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

            val id = worldId
            Scheduler.scheduleTask {
                if (id != worldId) return@scheduleTask
                val player = players.getOrPut(name) {
                    playerClasses[name] = DungeonClass.Unknown
                    DungeonPlayer(
                        name,
                        Devonian.minecraft.connection?.getPlayerInfo(name),
                        DungeonClass.Unknown,
                        0,
                        false
                    )
                }
                // nick
                if (players.size > 1 && !scannedSelf) {
                    players.remove(selfPlayer.name)
                    playerClasses.remove(selfPlayer.name)
                    player.profileInfo = selfPlayer.profileInfo
                    selfPlayer = player
                }
                scannedSelf = true

                if (role == "DEAD") player.isDead = true
                else {
                    player.isDead = false
                    player.isDisconnected = false
                    val c = DungeonClass.from(role)
                    player.role = c
                    playerClasses[name] = c
                    if (players.firstEntry().key == player.name) selfClass.value = c

                    val level = match.getOrNull(2)
                    if (level != null) player.classLevel = StringUtils.parseRoman(level)
                }
            }
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<TickEvent> {
            val mc = Devonian.minecraft

            // TODO: check when each player is being updated by the server
            // players.forEach { it.value.tick() }
            selfPlayer.tick()

            mc.level?.players()?.forEach {
                val ping = mc.connection?.getPlayerInfo(it.uuid)?.latency ?: return@forEach
                if (ping == -1) return@forEach

                val player = players[it.name.string] ?: return@forEach
                player.entity = it
            }
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<AreaEvent> { event ->
            val area = event.area
            if (area == "kuudra") wasInDungeons = false
            if (area == null || area != "catacombs") {
                if (!needReset) return@on
                DungeonScanner.reset()
                DungeonMapScanner.reset()
                reset()
                needReset = false
            } else {
                needReset = true
                wasInDungeons = true
            }
        }

        EventBus.on<WorldChangeEvent> {
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
            when (event.userMessage) {
                "Mimic Killed!",
                "\$SKYTILS-DUNGEON-SCORE-MIMIC$"
                    -> mimicKilled.value = true

                "Prince Killed!"
                    -> princeKilled.value = true
            }
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<ChatEvent> { event ->
            Stages.Root.onChat(event.message)

            if (event.message == "[NPC] Mort: Here, I found this map when I first entered the dungeon.") {
                started.value = true
                wasInDungeons = false
                DungeonEvent.RunStarted().post()
                return@on
            }

            if (event.message == "A Prince falls. +1 Bonus Score") {
                princeKilled.value = true
                DungeonEvent.PrinceKilled().post()
                return@on
            }

            event.matches(disconnectRegex)?.let {
                if (it[0] == Devonian.minecraft.gameProfile.name) wasInDungeons = true
                else players.remove(it[0])?.let { p ->
                    players[it[0]] = p
                    p.isDisconnected = true
                }
                return@on
            }

            val (name, message) = event.matches(bossMessageRegex) ?: return@on
            val boss = DungeonBoss.from(name) ?: return@on
            if (boss == DungeonBoss.Scarf && floor.floorNum != 2) return@on

            DungeonEvent.BossMessageEvent(boss, message).post()
            if (boss != DungeonBoss.Watcher) {
                inBoss.value = true
                DungeonEvent.BossRoomEnter(boss, floor).post()
            }
            else if (message == "You have proven yourself. You may pass.") bloodCleared.value = true
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<EntityDeathEvent> { event ->
            val entity = event.entity
            if (entity is Bat) {
                if (entity.maxHealth != 100f) return@on
                DungeonEvent.SecretBat(entity.x, entity.y, entity.z).post()
                return@on
            }
            if (entity !is Zombie) return@on
            // surely nothing can go wrong here
            if (!entity.isBaby) return@on

            mimicKilled.value = true
            DungeonEvent.MimicKilled().post()
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<ItemPickupEvent> { event ->
            val item = event.entity.item
            val id = ItemUtils.skyblockId(item) ?: return@on

            if (!DungeonEvent.SecretPickup.SECRET_ITEMS.contains(id)) return@on
            if (id == "POTION") {
                val type = item.get(DataComponents.POTION_CONTENTS)
                if (type?.potion?.get() != Potions.HEALING) return@on
            }

            val entity = event.entity

            DungeonEvent.SecretPickup(entity.xo, entity.yo, entity.zo).post()
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<PrePacketSentEvent> { event ->
            val packet = event.packet
            if (packet !is ServerboundUseItemOnPacket) return@on

            val minecraft = Devonian.minecraft
            val result = packet.hitResult
            val pos = result.blockPos
            val blockState = minecraft.level?.getBlockState(pos) ?: return@on
            val registryName = BuiltInRegistries.BLOCK.getKey(blockState.block)

            if (registryName.path == "player_head" && blockState.hasBlockEntity()) {
                val entityBlock = SecretsSound.minecraft.level?.getBlockEntity(pos) ?: return@on
                if (entityBlock.type != BlockEntityType.SKULL) return@on
                val skullBlock = entityBlock as SkullBlockEntity
                val owner = skullBlock.ownerProfile ?: return@on
                val id = owner.partialProfile().id ?: return@on

                if (!DungeonEvent.SecretClicked.SECRET_SKULLS.contains("$id")) return@on

                DungeonEvent.SecretClicked(
                    pos.x.toDouble(),
                    pos.y.toDouble(),
                    pos.z.toDouble(),
                    true,
                    DungeonEvent.SecretClicked.isRedstonekey("$id")
                ).post()
                return@on
            }
            if (!DungeonEvent.SecretClicked.SECRET_BLOCKS.contains("${registryName.namespace}:${registryName.path}"))
                return@on
            DungeonEvent.SecretClicked(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), isLever = registryName.path == "lever").post()
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<SoundPlayEvent> { event ->
            if (event.volume != 0.1f) return@on
            if (!DungeonEvent.SecretBatSound.SECRET_BATS.contains(event.sound)) return@on
            val cancel = DungeonEvent.SecretBatSound(event.x, event.y, event.z).post()
            if (!cancel) return@on
            event.cancel()
        }.setEnabled(Location.stateInArea("catacombs"))

        EventBus.on<ChatEvent> { event ->
            val (name) = event.matches(reconnectRegex) ?: return@on
            if (name != Devonian.minecraft.gameProfile.name) return@on

            // kuudra uses the same message
            // this also means if you crash and rejoin this will fail
            if (!wasInDungeons) return@on

            attemptGuessState.value = true
            started.value = true
            needReset = true
            Stages.Clear.start()
            Location.changeArea("catacombs")
        }

        EventBus.on<DungeonEvent.BossMessageEvent> { event ->
            inBoss.value = event.boss != DungeonBoss.Watcher
            bloodCleared.value = event.boss != DungeonBoss.Watcher
            if (event.boss != DungeonBoss.Watcher) attemptGuessState.value = false
            when (event.boss) {
                DungeonBoss.Watcher -> Stages.WatcherClear.start()
                DungeonBoss.Bonzo -> Stages.F1.start()
                DungeonBoss.Scarf -> Stages.F2.start()
                DungeonBoss.Professor -> Stages.F3.start()
                DungeonBoss.Thorn -> Stages.F4.start()
                DungeonBoss.Livid -> Stages.F5.start()
                DungeonBoss.Sadan -> Stages.F6.start()
                DungeonBoss.Maxor -> Stages.Maxor.start()
                DungeonBoss.Storm -> Stages.Storm.start()
                DungeonBoss.Goldor -> Stages.Goldor.start()
                DungeonBoss.Necron -> Stages.Necron.start()
                DungeonBoss.WitherKing -> Stages.WitherKing.start()
            }
        }.setEnabled(attemptGuessState)
    }

    private fun reset() {
        attemptGuessState.value = false

        selfPlayer = Devonian.minecraft.let { mc ->
            val prof = mc.gameProfile
            return@let DungeonPlayer(
                prof.name,
                PlayerInfo(prof, false),
                DungeonClass.Unknown,
                0,
                false,
            )
        }
        scannedSelf = false
        players.clear()
        players[selfPlayer.name] = selfPlayer
        playerClasses.clear()
        playerClasses[selfPlayer.name] = selfPlayer.role
        selfClass.value = selfPlayer.role
        worldId++

        floor = FloorType.None
        Stages.Root.reset()

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
        totalRoomSecrets.value = 0

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