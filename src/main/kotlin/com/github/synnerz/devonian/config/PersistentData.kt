package com.github.synnerz.devonian.config

interface PersistentData : PersistentObject {
    fun getData(): DataObject
}