package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.api.HypixelModApi
import com.github.synnerz.devonian.api.Party
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.WebRequests
import com.github.synnerz.devonian.api.events.Event
import com.github.synnerz.devonian.api.events.GameUnloadEvent
import com.github.synnerz.devonian.config.Categories
import com.mojang.authlib.exceptions.AuthenticationException
import kotlinx.coroutines.future.await
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

object WebsocketClient : Feature(
    "websocket",
    "Enables websocket server to share and receive data to/from other players",
    Categories.GLOBAL,
    subcategory = "Mod",
    displayName = "Websocket Client"
) {
    private val listener = object : WebSocket.Listener {
        override fun onOpen(webSocket: WebSocket?) {
            if (webSocket != null)
                onOpenSocket(webSocket)
            super.onOpen(webSocket)
        }

        override fun onText(webSocket: WebSocket?, data: CharSequence?, last: Boolean): CompletionStage<*>? {
            if (data != null && webSocket != null)
                onTextSocket(webSocket, data)
            return super.onText(webSocket, data, last)
        }

        override fun onError(webSocket: WebSocket?, error: Throwable?) {
            println("Devonian\$WebSocket error")
            error?.printStackTrace()
            if (retry <= 0)
                retry = 5
            super.onError(webSocket, error)
        }

        override fun onClose(webSocket: WebSocket?, statusCode: Int, reason: String?): CompletionStage<*>? {
            if (webSocket != null)
                onCloseSocket(webSocket, statusCode, reason)
            return super.onClose(webSocket, statusCode, reason)
        }
    }
    private const val KEY_NAME = "devonianwebsocket"
    private val playerName
        get() = minecraft.player?.name?.string ?: ""
    private var retry = 0
    private var oldPartyState = false
    var socket: WebSocket? = null

    class MessageEvent(
        val webSocket: WebSocket,
        val message: String,
    ) : Event {
        fun matches(criteria: Regex): List<String>? {
            val match = criteria.matchEntire(message) ?: return null
            return match.groupValues.drop(1)
        }
    }

    init {
        configSwitch.onChange { v ->
            if (v) {
                connect()
                return@onChange
            }
            disconnect()
        }
    }

    override fun initialize() {
        on<GameUnloadEvent> {
            socket?.abort()
        }

        on<HypixelModApi.HelloPacket> {
            connect()
        }

        on<Party.PartyJoinEvent> {
            onParty()
            oldPartyState = true
        }
        on<Party.PartyLeaveEvent> {
            onParty(false)
            oldPartyState = false
        }

        Scheduler.schedulePool.scheduleWithFixedDelay({
            if (retry <= 0) return@scheduleWithFixedDelay
            println("Devonian\$WebSocket attempting to reconnect x$retry")
            retry--
            socket?.abort()
            socket = null
            connect()
        }, 30L, 30L, TimeUnit.SECONDS)

        configSwitch.set(true)
    }

    fun connect() {
        if (isConnected()) {
            println("Devonian\$WebSocket attempted connecting while an active connection exists")
            return
        }
        if (playerName.isEmpty()) {
            println("Devonian\$WebSocket attempted connecting while a player entity did not exist")
            return
        }

        val r = UUID.randomUUID().toString()
        WebRequests.withName(
            "Websocket",
            {
                authenticate(hash(r)) {
                    _connect(r)
                }
            },
            {
                if (retry >= 1) return@withName
                retry = 5
            }
        )
    }

    fun disconnect() {
        send("Disconnect")
        socket = null
    }

    fun isConnected() = socket != null

    fun onOpenSocket(webSocket: WebSocket) {}

    fun onTextSocket(webSocket: WebSocket, data: CharSequence) {
        // TODO: check if this ever triggers, if so something went very wrong
        if (!isEnabled()) return
        val msg = data.toString()
        Scheduler.scheduleTask {
            MessageEvent(webSocket, msg).post()
            if (msg == "3002" && Party.inParty) {
                onParty()
            }
        }
    }

    fun onCloseSocket(webSocket: WebSocket, statusCode: Int, reason: String?) {
        println("Devonian\$Websocket disconnected $statusCode \"$reason\"")
        socket = null
    }

    fun onParty(inParty: Boolean = true) {
        if (!inParty) {
            send("PartyLeave")
            return
        }
        if (oldPartyState) send("PartyLeave")
        send("Party[${Party.partyHash}]")
    }

    fun send(msg: String) {
        if (!isConnected()) return
        socket?.sendText(msg, true)
    }

    private suspend fun _connect(r: String) {
        val wss = HttpClient
            .newHttpClient()
            .newWebSocketBuilder()
            .buildAsync(URI.create("ws://wss.docilelm.top/"), listener)
            .await()

        retry = 0
        socket = wss
        send("Authenticate[${playerName}, $r]")
    }

    private suspend fun authenticate(serverId: String, onResolve: suspend () -> Unit) {
        try {
            // we let the official mojang services handle authentication for us
            // because we don't need our users to be skeptical of the mod itself
            // our server only receives the "random" uuid created before calling this method (search for its name to see)
            // this way no sensitive data (like your access token in this case) is sent to us at all (because it shouldn't)
            minecraft.services().sessionService.joinServer(
                minecraft.player!!.uuid,
                minecraft.user.accessToken,
                serverId
            )
            onResolve()
        } catch (e: AuthenticationException) {
            println("Devonian\$Websocket Authentication error")
            e.printStackTrace()
        }
    }

    private fun hash(r: String): String {
        val m = MessageDigest.getInstance("sha1")
        return BigInteger(
            1,
            m.digest("$KEY_NAME$r".byteInputStream().readAllBytes())
        ).toString(16)
    }
}