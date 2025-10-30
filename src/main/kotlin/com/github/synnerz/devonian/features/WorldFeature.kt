package com.github.synnerz.devonian.features

import com.github.synnerz.devonian.api.events.AreaEvent
import com.github.synnerz.devonian.api.events.SubAreaEvent
import com.github.synnerz.devonian.api.events.WorldChangeEvent

open class WorldFeature @JvmOverloads constructor(
    configName: String,
    area: String? = null,
    subarea: String? = null
) : Feature(configName, area, subarea) {
    init {
        on<AreaEvent>(::onAreaChange)
        on<SubAreaEvent>(::onSubAreaChange)
        on<WorldChangeEvent>(::onWorldChange)
    }

    open fun onAreaChange(event: AreaEvent) {}

    open fun onSubAreaChange(event: SubAreaEvent) {}

    open fun onWorldChange(event: WorldChangeEvent) {}
}