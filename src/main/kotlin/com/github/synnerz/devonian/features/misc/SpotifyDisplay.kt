package com.github.synnerz.devonian.features.misc

import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.RenderOverlayEvent
import com.github.synnerz.devonian.commands.DevonianCommand
import com.github.synnerz.devonian.config.Categories
import com.github.synnerz.devonian.config.Config
import com.github.synnerz.devonian.config.ConfigData
import com.github.synnerz.devonian.config.json.JsonDataObject
import com.github.synnerz.devonian.hud.texthud.Marquee
import com.github.synnerz.devonian.hud.texthud.StylizedTextHud
import com.github.synnerz.devonian.hud.texthud.TextHudFamily
import com.github.synnerz.devonian.hud.texthud.TextHudFeature
import com.github.synnerz.devonian.utils.DebugLogger
import com.github.synnerz.devonian.utils.StringUtils.replaceCodes
import com.google.gson.JsonArray
import java.util.*
import java.util.concurrent.TimeUnit

object SpotifyDisplay : TextHudFeature(
    "spotifyDisplay",
    "shows current song playing, Windows with Spotify app and Linux with Spotify + playerctl package",
) {
    override var dirty = false
    private val SETTING_HIDE_NOT_OPEN = addSwitch(
        "hideClosed",
        true,
        "do not render the spotify display if spotify is not opened",
        "Hide If Not Opened",
    )
    private val SETTING_MAX_SONG_LENGTH = addSlider(
        "maxSongLength",
        100.0,
        0.0, 300.0,
        "",
        "Max Song Name Length",
    )
    private val SETTING_PREFIX = addTextInput(
        "prefix",
        "&2Spotify &7>&r ",
        "",
        "Spotify Prefix",
    )
    private val SETTING_FORMAT = addTextInput(
        "format",
        "&a%ARTIST% &7-&b %SONG%",
        "use %ARTIST% and %SONG% (or dont for some reason idfc)",
        "Spotify Format",
    )
    private val SETTING_ALTERNATE_SCROLLING = addSwitch(
        "alternate",
        false,
        "",
        "Alternate Marquee Scrolling",
    )
    private val SETTING_SCROLL_SPEED = addSlider(
        "scrollSpeed",
        80.0,
        0.0, 300.0,
        "",
        "Marquee Scroll Speed",
    )
    private val SETTING_MEDIA_PLAYER = addTextInput(
        "mediaPlayer",
        "spotify",
        "&4LINUX ONLY&r name of the media player it will scan for (default \"spotify\")".replaceCodes(),
        "Media Player",
    )
    private val SETTING_LOGGER = ConfigData.Switch(
        "${configName}\$logger",
        false,
        null,
        "requires restart",
        "Spotify Logger",
        "Misc",
        emptySet(),
        isHidden,
    ).also {
        Config.registerCategory(it, Categories.DEBUG, "Misc")
    }

    override fun createHud(): StylizedTextHud = TextHudFamily(configName, this)

    override fun getEditText(): List<String> = throw UnsupportedOperationException()

    override fun setEditDisplay() {
        setDisplay("Never Gonna Give You Up", "Rick Astley")
    }

    val prefixHud = StylizedTextHud("spotifyDisplayPrefix", (hud as TextHudFamily).createChildProvider())
    val marqueeHud = Marquee("spotifyDisplayMarquee", (hud as TextHudFamily).createChildProvider())

    init {
        val hud = hud as TextHudFamily
        hud.children.add(prefixHud)
        hud.children.add(marqueeHud)
    }

    private val isWindows = System.getProperty("os.name").startsWith("Windows")
    private val isLinux = System.getProperty("os.name").startsWith("Linux")
    private val linuxSongRegex = " status: (Paused|Playing) artist: (.*), title: (.*)$".toRegex()

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
            if (isWindows || isLinux)
                specialNames[song] ?:
                SETTING_FORMAT.get()
                    .replace("%SONG%", song)
                    .replace("%ARTIST%", artist)
            else "&cNot on Windows/Linux"
        )
        marqueeHud.maxLen = SETTING_MAX_SONG_LENGTH.get()
        marqueeHud.alternate = SETTING_ALTERNATE_SCROLLING.get()
        marqueeHud.scrollSpeed = SETTING_SCROLL_SPEED.get()
    }

    private var song = ""
    private var artist = ""
    private var open = false
    private var cmdReg = false

    private val logger = DebugLogger("SpotifyLogger")

    override fun initialize() {
        if (SETTING_LOGGER.get()) {
            logger.startLogger()

            DevonianCommand.command.subcommand("dumpspotify") { _, _ ->
                logger.stopAndPrint()
                return@subcommand 1
            }
            cmdReg = true
        }
        SETTING_LOGGER.onChange {
            if (it && !cmdReg) {
                logger.startLogger()

                DevonianCommand.command.subcommand("dumpspotify") { _, _ ->
                    logger.stopAndPrint()
                    return@subcommand 1
                }
                cmdReg = true
            }
        }

        if (isWindows) {
            Scheduler.schedulePool.scheduleWithFixedDelay({
                if (!isEnabled()) return@scheduleWithFixedDelay
                if (isEditing) return@scheduleWithFixedDelay

                val obj = JsonDataObject().also {
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

                    it.set("time", System.currentTimeMillis())

                    val sc = Scanner(proc.inputStream, Charsets.UTF_8)
                    // Active code page: 65001
                    it.set("first", sc.nextLine())

                    val arr = JsonArray()
                    it.set("lines", arr)

                    while (sc.hasNextLine()) {
                        val line = sc.nextLine()
                        arr.add(line)
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
                        return@also
                    }

                    song = "NOT OPENED"
                    open = false
                    proc.waitFor(2L, TimeUnit.SECONDS)
                }

                logger.offer(obj)
            }, 0L, 2L, TimeUnit.SECONDS)
        }
        if (isLinux) {
            Scheduler.schedulePool.scheduleWithFixedDelay({
                if (!isEnabled()) return@scheduleWithFixedDelay
                if (isEditing) return@scheduleWithFixedDelay

                val mediaPlayer = SETTING_MEDIA_PLAYER.get()
                val proc = ProcessBuilder(
                    "bash",
                    "-c",
                    // force this into one line with echo (can't separate the cmd)
                    "echo $(" +
                        // some distros require you to install the "which" command package
                        // "command -v" _should_ be everywhere though
                        "command -v playerctl" +
                        "&& playerctl -p $mediaPlayer status -f \"status: {{ status }}\"" +
                        "&& playerctl -p $mediaPlayer metadata -f \"artist: {{ artist }}, title: {{ title }}\"" +
                    ")"
                ).start().also { proc ->
                    val scanner = Scanner(proc.inputStream, Charsets.UTF_8)
                    if (!scanner.hasNextLine()) return@also
                    val line = scanner.nextLine()
                    val match = linuxSongRegex.find(line)?.groupValues ?: return@also

                    val ( _, status, artistName, songName ) = match
                    if (status == "Paused") {
                        artist = ""
                        song = "&cPaused"
                        open = true

                        return@scheduleWithFixedDelay
                    }

                    artist = artistName
                    song = songName
                    open = true

                    return@scheduleWithFixedDelay
                }

                song = "&cNot Opened &7(is playerctl installed?)"
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