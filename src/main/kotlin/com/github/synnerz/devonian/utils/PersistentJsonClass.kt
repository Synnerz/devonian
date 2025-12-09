package com.github.synnerz.devonian.utils

import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

open class PersistentJsonClass<T : Any>(configFile: File, val clazz: Class<T>) : PersistentJson(configFile) {
    var data: T? = null

    override fun onLoad(reader: InputStream) {
        data = gson.fromJson(reader.bufferedReader(Charsets.UTF_8).use { it.readText() }, clazz)
    }

    override fun onSave(writer: OutputStream) {
        val obj = data ?: return
        writer.bufferedWriter().use { gson.toJson(obj, it) }
    }
}