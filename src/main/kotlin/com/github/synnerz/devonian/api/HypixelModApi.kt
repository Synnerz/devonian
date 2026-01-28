package com.github.synnerz.devonian.api

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.events.Event
import net.hypixel.data.rank.MonthlyPackageRank
import net.hypixel.data.rank.PackageRank
import net.hypixel.data.rank.PlayerRank
import net.hypixel.data.type.ServerType
import net.hypixel.modapi.HypixelModAPI
import net.hypixel.modapi.packet.impl.clientbound.ClientboundHelloPacket
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket.PartyRole
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPingPacket
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPingPacket
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket
import java.util.*
import kotlin.jvm.optionals.getOrNull

object HypixelModApi {
    val modApiIns: HypixelModAPI get() = HypixelModAPI.getInstance()

    class HelloPacket : Event()
    class PartyInfoPacket(
        val inParty: Boolean,
        val members: Map<UUID, PartyRole>,
        val isLeader: Boolean, // whether the current user is the leader
    ) : Event()
    class PingPacket(
        val response: String,
    ) : Event()
    class PlayerInfoPacket(
        val playerRank: PlayerRank,
        val packageRank: PackageRank,
        val monthlyPackageRank: MonthlyPackageRank,
        val prefix: String?,
    ) : Event()
    class LocationPacket(
        val serverName: String,
        val serverType: ServerType?,
        val lobbyName: String?,
        val mode: String?,
        val map: String?,
    ) : Event()

    fun initialize() {
        modApiIns.createHandler(ClientboundHelloPacket::class.java) { HelloPacket().post() }
        modApiIns.createHandler(ClientboundPartyInfoPacket::class.java) {
            it.apply {
                if (!isInParty) {
                    PartyInfoPacket(false, mapOf(), false).post()
                    return@apply
                }

                var isLeader = false
                val mem = buildMap {
                    memberMap.forEach { (k, v) ->
                        if (k == Devonian.minecraft.player!!.uuid && v.role == PartyRole.LEADER)
                            isLeader = true
                        put(k, v.role)
                    }
                }

                PartyInfoPacket(true, mem, isLeader).post()
            }
        }
        modApiIns.createHandler(ClientboundPingPacket::class.java) {
            PingPacket(it.response).post()
        }
        modApiIns.createHandler(ClientboundPlayerInfoPacket::class.java) {
            PlayerInfoPacket(
                it.playerRank,
                it.packageRank,
                it.monthlyPackageRank,
                it.prefix.getOrNull()
            ).post()
        }
        modApiIns.subscribeToEventPacket(ClientboundLocationPacket::class.java)
        modApiIns.createHandler(ClientboundLocationPacket::class.java) {
            LocationPacket(
                it.serverName,
                it.serverType.getOrNull(),
                it.lobbyName.getOrNull(),
                it.mode.getOrNull(),
                it.map.getOrNull(),
            ).post()
        }
    }

    fun requestPing() {
        modApiIns.sendPacket(ServerboundPingPacket())
    }

    fun requestPartyInfo() {
        modApiIns.sendPacket(ServerboundPartyInfoPacket())
    }

    fun requestPlayerInfo() {
        modApiIns.sendPacket(ServerboundPlayerInfoPacket())
    }
}