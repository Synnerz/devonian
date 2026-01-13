package com.github.synnerz.devonian.utils

import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStream
import java.io.OutputStream

open class PersistentJsonClass<T : Any> : PersistentJson {
    private val clazz: Class<T>?
    private val type: TypeToken<T>?
    var data: T? = null

    constructor(configFile: File, clazz: Class<T>) : super(configFile) {
        this.clazz = clazz
        type = null
    }
    constructor(configFile: File, type: TypeToken<T>) : super(configFile) {
        clazz = null
        this.type = type
    }

    override fun onLoad(reader: InputStream): Boolean {
        reader.bufferedReader(Charsets.UTF_8).use {
            data = if (clazz != null) gson.fromJson(it, clazz) else gson.fromJson(it, type)
        }
        return data != null
    }

    override fun onSave(writer: OutputStream) {
        val obj = data ?: return
        writer.bufferedWriter().use { gson.toJson(obj, it) }
    }
}