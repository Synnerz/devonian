package com.github.synnerz.devonian.utils

import com.github.synnerz.devonian.Devonian
import java.nio.file.Files
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.PosixFilePermission
import java.util.*

object OpenEditor {
    private val isWindows = System.getProperty("os.name").startsWith("Windows")
    private val isMac = System.getProperty("os.name").contains("mac", ignoreCase = true)
    private val permSet = EnumSet.of(
        PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
        PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE,
        PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE,
    )
    private val perms = object : FileAttribute<Set<PosixFilePermission>> {
        override fun name(): String = "posix:permissions"
        override fun value(): Set<PosixFilePermission> = permSet
    }

    private val instructions = "This first line will be ignored. Edit the value, save the file, and close the editor."

    fun edit(initial: List<String>, cb: (List<String>) -> Unit) {
        Thread({
            val rootPath = Devonian.minecraft.gameDirectory.toPath().resolve("config/devonian")
            val tmpPath = if (isWindows) Files.createTempFile(rootPath, "edit-", ".txt")
                else Files.createTempFile(rootPath, "edit-", ".txt", perms)

            Files.write(tmpPath, listOf(instructions) + initial, Charsets.UTF_8)

            val editProc = ProcessBuilder(
                if (isWindows) listOf("cmd.exe", "/s", "/c", "start", "/B", "/WAIT", "\"\"", "\"$tmpPath\"")
                else if (isMac) listOf("open", "\"$tmpPath\"")
                else listOf("xdg-open", "\"$tmpPath\"")
            ).start()
            editProc.waitFor()

            val txt = Files.readAllLines(tmpPath, Charsets.UTF_8)
            Files.deleteIfExists(tmpPath)
            txt.removeFirstOrNull()

            cb(txt)
        }, "DevonianOpenEditor").start()
    }
}