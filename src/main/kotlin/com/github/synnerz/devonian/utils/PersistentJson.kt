package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import com.github.synnerz.devonian.api.Scheduler
import com.github.synnerz.devonian.api.events.EventBus
import com.github.synnerz.devonian.api.events.GameUnloadEvent
import com.github.synnerz.devonian.config.PersistentObject
import com.google.gson.GsonBuilder
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

abstract class PersistentJson(fileName: String, private var saveBackups: Boolean = true) : PersistentObject {
    private val root = Devonian.minecraft.gameDirectory.toPath().resolve("config")
    private val mainPath = root.resolve(fileName)
    private val backupFolder = root.resolve("devonian/backups")
    private val fileName = mainPath.fileName.toString()
    private val backupReg = "^(.+?)-(\\d+)\\.bak$".toRegex()

    private val afterLoadListeners = mutableListOf<() -> Unit>()
    private val preSaveListeners = mutableListOf<() -> Unit>()

    private var shouldSave = false

    override fun onAfterLoad(cb: () -> Unit) {
        afterLoadListeners.add(cb)
    }

    override fun onPreSave(cb: () -> Unit) {
        preSaveListeners.add(cb)
    }

    init {
        EventBus.on<GameUnloadEvent> {
            saveBackups = false
            save()
        }

        Scheduler.schedulePool.scheduleWithFixedDelay(::save, 5L, 5L, TimeUnit.MINUTES)
    }

    abstract fun onLoad(reader: InputStream): Boolean
    open fun onLoadDefault() {}

    private fun tryLoad(p: Path): Boolean {
        return try {
            if (p.exists()) {
                Files.newInputStream(p).use {
                    onLoad(it)
                }
            } else false
        } catch (_: Exception) {
            false
        }
    }

    private fun extractBackupTime(p: Path): Long? {
        if (p.isDirectory()) return null

        val file = p.fileName.toString()
        val m = backupReg.matchEntire(file) ?: return null

        val name = m.groupValues.getOrNull(1) ?: return null
        if (name != fileName) return null

        val time = m.groupValues.getOrNull(2) ?: return null

        return time.toLongOrNull()
    }

    override fun load() {
        0.let {
            if (tryLoad(mainPath)) return@let

            println("PersistentJson: $fileName failed to load from main file.")

            if (backupFolder.exists()) {
                Files.walk(backupFolder).use { files ->
                    val old = files
                        .map { p -> extractBackupTime(p)?.let { p to it } }
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingLong { -it!!.second })
                        .toList()

                    old.forEach {
                        println("PersistentJson: $fileName attempting to load from backup ${it.first.fileName}")
                        if (tryLoad(it!!.first)) return@let
                    }
                }
            }

            println("PersistentJson: $fileName failed to load from backups.")
            onLoadDefault()
        }

        shouldSave = true
        afterLoadListeners.forEach { it() }
    }

    abstract fun onSave(writer: OutputStream)

    private fun save(p: Path) {
        val latch = CountDownLatch(1)
        Devonian.minecraft.execute {
            preSaveListeners.forEach { it() }
            latch.countDown()
        }
       if (!latch.await(1000L, TimeUnit.MILLISECONDS)) return

        Files.createDirectories(p.parent)
        Files.newOutputStream(p).use { onSave(it) }
    }

    /**
     * - Saves all the json into a file
     * - Note: must call during game shutdown
     */
    override fun save() {
        if (!shouldSave) return

        if (saveBackups && mainPath.exists()) {
            if (backupFolder.exists()) {
                Files.walk(backupFolder).use { files ->
                    val old = files
                        .map { p -> extractBackupTime(p)?.let { p to it } }
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingLong { -it!!.second })
                        .toList()

                    for (i in MAX_BACKUPS - 1 until old.size) {
                        old[i].first.deleteIfExists()
                    }
                }
            } else backupFolder.createDirectories()

            Files.copy(mainPath, backupFolder.resolve("$fileName-${System.currentTimeMillis()}.bak"))
        }

        save(mainPath)
    }

    companion object {
        val gson = GsonBuilder().setPrettyPrinting().create()!!
        private const val MAX_BACKUPS = 5
    }
}