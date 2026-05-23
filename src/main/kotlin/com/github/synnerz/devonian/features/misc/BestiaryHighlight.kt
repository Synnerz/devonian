package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.events.EntityJoinEvent
import com.github.synnerz.devonian.api.events.NameChangeEvent
import com.github.synnerz.devonian.api.events.PacketReceivedEvent
import com.github.synnerz.devonian.api.events.RenderWorldEvent
import com.github.synnerz.devonian.api.events.TickEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.features.Feature
import com.github.synnerz.devonian.utils.StringUtils
import com.github.synnerz.devonian.utils.render.Render3DImmediate
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

object BestiaryHighlight : Feature(
    "bestiaryHighlight",
    "highlights the entities you set in the text input",
    Categories.MISC,
    subcategory = "General",
) {
    private val SETTING_INPUT_MATCH = addTextInput(
        "mobMatch",
        "",
        "Use custom format, you can input <name hp:100> or multiple hp values like <name hp:100|1M|1k|1B> \"|\" acts as OR. or don't input any hp and it'll match only based on name.",
        "Mob Match"
    ).also {
        it.onChange { str ->
            entities.clear()
            mobPairs.clear()

            if (!str.contains(":")) {
                nameToMatch = str.trim().lowercase()
                hpToMatch.clear()
                return@onChange
            }

            val match = parserRegex.matchEntire(str)?.groupValues ?: return@onChange
            val name = match.getOrNull(1) ?: return@onChange
            val hpMatch = match.getOrNull(2)
            if (hpMatch != null) {
                hpToMatch.clear()
                if (hpMatch.contains("|")) {
                    hpMatch.split("|").forEach {
                        hpToMatch.add(
                            if (!it.matches(shortenedRegex)) it.toIntOrNull()?.toDouble() ?: -1.0
                            else StringUtils.parseShortenedNumber(it.uppercase()).toDouble()
                        )
                    }
                }
                else {
                    hpToMatch.add(
                        if (!hpMatch.matches(shortenedRegex)) hpMatch.toIntOrNull()?.toDouble() ?: -1.0
                        else StringUtils.parseShortenedNumber(hpMatch.uppercase()).toDouble()
                    )
                }
            }

            nameToMatch = name.trim().lowercase()
        }
    }
    private val SETTING_OUTLINE_COLOR = addColorPicker(
        "outlineColor",
        Color.GREEN.rgb,
        "",
        "Outline Color"
    )
    private val SETTING_OUTLINE_WIDTH = addSlider(
        "outlineWidth",
        2.0,
        0.0, 10.0,
        "",
        "Outline Width"
    )
    // https://regex101.com/r/z0JQMW/1
    // [mob type, mob type2, name, hp, max hp (can be null)]
    private val mobNameRegex = "^(?:\\uFD3E )?(?:\\[Lv\\d+] )?([^\\u0000-\\u007F]+)([^\\u0000-\\u007F]+)?(?: ✯)? ([A-z ]+) ([\\d,.MkB]+)/?([\\d,.MkB]+)?❤(?: \\uFD3F)?$".toRegex()
    // [name, hp(s)]
    private val parserRegex = "^([\\w ]+)(?:hp:([\\d,.kMB|]+))?$".toRegex()
    private val shortenedRegex = "^[\\d,.]+([kMB])$".toRegex()
    private val entities = ConcurrentLinkedQueue<Int>()
    private val mobPairs = mutableListOf<LivingEntity>()
    private var lastStand = 0
    private var nameToMatch = ""
    private var hpToMatch = mutableListOf<Double>()
    var phase = false

    data class MobData(
        val name: String,
        val hp: Double,
        val mobType: String? = null,
        val mobType2: String? = null,
        val id: Int = -1,
    )

    override fun initialize() {
        on<PacketReceivedEvent> { event ->
            val packet = event.packet
            if (packet !is ClientboundAddEntityPacket) return@on
            if (packet.type != EntityType.ARMOR_STAND) return@on

            lastStand = packet.id
        }

        on<NameChangeEvent> { event ->
            if (event.entityId != lastStand) return@on
            val match = mobNameRegex.matchEntire(event.name)?.groupValues ?: return@on
            val mobType = match.getOrNull(1)
            val mobType2 = match.getOrNull(2)
            val name = match.getOrNull(3) ?: return@on
            val hpMatch = match.getOrNull(4) ?: return@on
            val maxHpMatch = match.getOrNull(5)
            val hp =
                if (maxHpMatch != null) {
                    StringUtils.parseShortenedNumber(maxHpMatch.uppercase()).toDouble()
                } else {
                    StringUtils.parseShortenedNumber(hpMatch.uppercase()).toDouble()
                }

            val data = MobData(name, hp, mobType, mobType2, event.entityId)
            if (!matches(data)) return@on

            entities.add(event.entityId - 1)
        }

        on<EntityJoinEvent> { event ->
            val entity = event.entity as? LivingEntity ?: return@on
            val name = entity.name.string
            val data = MobData(name, entity.maxHealth.toDouble(), id = entity.id)
            if (!matches(data)) return@on

            entities.add(entity.id)
        }

        on<TickEvent> {
            val level = minecraft.level ?: return@on
            var length = entities.size

            while (--length >= 0) {
                val id = entities.poll() ?: break
                val entity = level.getEntity(id) as? LivingEntity
                if (entity == null) entities.offer(id)
                else mobPairs.add(entity)
            }
        }

        on<RenderWorldEvent> {
            mobPairs.removeIf { ent ->
                if (ent.isDeadOrDying || ent.isRemoved) return@removeIf true

                val pos = ent.getPosition(minecraft.deltaTracker.getGameTimeDeltaPartialTick(false))
                Render3DImmediate.renderWireframeBox(
                    pos.x,
                    pos.y,
                    pos.z,
                    0.8, 2.0,
                    SETTING_OUTLINE_COLOR.getColor(),
                    phase = phase,
                    lineWidth = SETTING_OUTLINE_WIDTH.get(),
                    centered = true,
                )

                false
            }
        }
    }

    override fun onWorldChange(event: WorldChangeEvent) {
        mobPairs.clear()
        entities.clear()
        lastStand = 0
    }

    private fun matches(mobData: MobData): Boolean {
        val m = if (hpToMatch.isNotEmpty() && mobData.hp != -1.0) mobData.hp in hpToMatch else true
        return nameToMatch.isNotEmpty() && mobData.name.lowercase().contains(nameToMatch) && m
    }
}