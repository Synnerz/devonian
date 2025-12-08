package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.hud.texthud.Marquee
import com.github.synnerz.devonian.hud.texthud.TextHud
import com.github.synnerz.devonian.hud.texthud.TextHudFamily
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import java.util.Scanner
import java.util.concurrent.TimeUnit

object SpotifyDisplay : TextHudFeature(
    "spotifyDisplay",
    "shows current song playing, only Windows with Spotify app"
) {
    private val SETTING_HIDE_NOT_OPEN = addSwitch(
        "hideClosed",
        "do not render the spotify display if spotify is not opened",
        "Hide If Not Opened",
        true
    )
    private val SETTING_MAX_SONG_LENGTH = addSlider(
        "maxSongLength",
        "",
        "Max Song Name Length",
        0.0, 300.0,
        100.0
    )
    private val SETTING_PREFIX = addTextInput(
        "prefix",
        "",
        "Spotify Prefix",
        "&2Spotify &7>&r "
    )
    private val SETTING_FORMAT = addTextInput(
        "format",
        "use %ARTIST% and %SONG% (or dont for some reason idfc)",
        "Spotify Format",
        "&a%ARTIST% &7-&b %SONG%"
    )
    private val SETTING_ALTERNATE_SCROLLING = addSwitch(
        "alternate",
        "",
        "Alternate Marquee Scrolling",
        false
    )
    private val SETTING_SCROLL_SPEED = addSlider(
        "scrollSpeed",
        "",
        "Marquee Scroll Speed",
        0.0, 300.0,
        80.0
    )

    override fun createHud(): TextHud = TextHudFamily("spotifyDisplay", this)

    override fun getEditText(): List<String> = throw UnsupportedOperationException()

    override fun setEditDisplay() {
        setDisplay("Never Gonna Give You Up", "Rick Astley")
    }

    val prefixHud = TextHud("spotifyDisplayPrefix", (hud as TextHudFamily).createChildProvider())
    val marqueeHud = Marquee("spotifyDisplayMarquee", (hud as TextHudFamily).createChildProvider())

    init {
        val hud = hud as TextHudFamily
        hud.children.add(prefixHud)
        hud.children.add(marqueeHud)
    }

    private val isWindows = System.getProperty("os.name").startsWith("Windows")
    
    private val specialNames = mapOf(
        "Spotify Free" to "&cPaused",
        "Spotify Premium" to "&cPaused",
        "AngleHiddenWindow" to "&cPaused",
        "Spotify" to "&aAdvertisement",
        "NOT OPENED" to "&cNot Opened"
    )

    fun setDisplay(song: String, artist: String) {
        prefixHud.setLine(SETTING_PREFIX.get())
        marqueeHud.setLine(
            if (isWindows)
                specialNames[song] ?:
                SETTING_FORMAT.get()
                    .replace("%SONG%", song)
                    .replace("%ARTIST%", artist)
            else "&cNot on Windows"
        )
        marqueeHud.maxLen = SETTING_MAX_SONG_LENGTH.get()
        marqueeHud.alternate = SETTING_ALTERNATE_SCROLLING.get()
        marqueeHud.scrollSpeed = SETTING_SCROLL_SPEED.get()
    }

    private var song = ""
    private var artist = ""
    private var open = false

    override fun initialize() {
        if (isWindows) {
            Scheduler.schedulePool.scheduleWithFixedDelay({
                if (isEditing) return@scheduleWithFixedDelay

                val proc = ProcessBuilder(
                    "cmd.exe", "/s", "/c",
                    "chcp", "65001",
                    "&&",
                    "tasklist.exe",
                    "/fo", "csv",
                    "/nh",
                    "/v",
                    "/fi", "\"IMAGENAME eq Spotify.exe\""
                ).start()

                val sc = Scanner(proc.inputStream, Charsets.UTF_8)
                // Active code page: 65001
                sc.nextLine()

                while (sc.hasNextLine()) {
                    val line = sc.nextLine()
                    if (line == "INFO: No tasks are running which match the specified criteria.") break

                    val parts = line.split("\",\"")
                    var name = parts.drop(8).joinToString("\",\"").dropLast(1)

                    if (name == "N/A") continue
                    name = name.trim()

                    if (name in specialNames) song = name
                    else {
                        val i = name.indexOf(" - ")
                        artist = name.take(i)
                        song = name.drop(i + 3)
                    }
                    open = true
                    return@scheduleWithFixedDelay
                }

                song = "NOT OPENED"
                open = false
                proc.waitFor(2L, TimeUnit.SECONDS)
            }, 0L, 2L, TimeUnit.SECONDS)
        }

        on<RenderOverlayEvent> { event ->
            if (!open && SETTING_HIDE_NOT_OPEN.get()) return@on
            if (!isEditing) setDisplay(song, artist)
            draw(event.ctx)
        }
    }
}