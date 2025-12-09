package com.github.synnerz.devonian.config.json

import com.github.synnerz.devonian.config.DataObject
import com.github.synnerz.devonian.config.PersistentData
import com.github.synnerz.devonian.utils.PersistentJson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.InputStream
import java.io.OutputStream

// TODO: add auto backup type system

class PersistentJsonData(configFile: File) : PersistentJson(configFile), PersistentData {
    private var jsonRoot = JsonObject()
    private var root = JsonDataObject(jsonRoot)

    override fun onLoad(reader: InputStream) {
        val savedData = reader.bufferedReader().use { JsonParser.parseReader(it).asJsonObject }
        if (savedData.isEmpty) return

        savedData.entrySet().forEach { (k, v) ->
            jsonRoot.add(k, v)
        }
    }

    override fun onSave(writer: OutputStream) {
        writer.bufferedWriter().use { gson.toJson(jsonRoot, it) }
    }

    override fun getData(): DataObject = root
}