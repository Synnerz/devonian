package com.github.synnerz.devonian.api.splits

import com.github.synnerz.devonian.utils.BasicState

class SequentialSplitStage : SplitStage {
    constructor(children: Array<SplitStage>) : super(children)
    constructor(filter: String, children: Array<SplitStage> = arrayOf()) : super(filter, children)
    constructor(filter: Regex, children: Array<SplitStage> = arrayOf()) : super(filter, children)

    var activeChild = children[0]
    val activeChildState = BasicState(activeChild)

    override fun reset() {
        super.reset()
        activeChild = children[0]
        activeChildState.value = children[0]
    }

    override fun start() {
        if (hasStarted()) return
        _start()
        parent?.onChildStart(this)

        activeChild.onParentStart()
    }

    override fun onChildStart(child: SplitStage) {
        super.onChildStart(child)

        children.forEach {
            if (it !== child) it.stop()
        }
        activeChild = child
        activeChildState.value = child
    }
}