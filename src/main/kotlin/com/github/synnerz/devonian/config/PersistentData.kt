package com.github.synnerz.devonian.config

interface PersistentData {
    fun onAfterLoad(cb: () -> Unit)
    fun onPreSave(cb: () -> Unit)

    fun getData(): DataObject

    /**
     * - Loads the data from the saved file if it exists
     * - Note: this should run once every other feature is done loading.
     * it's done this way so that it respects the default values.
     */
    fun load()

    /**
     * - Saves all the json into a file
     * - Note: must call during game shutdown
     */
    fun save()
}