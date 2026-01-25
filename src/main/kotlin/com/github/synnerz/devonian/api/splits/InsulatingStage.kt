package com.github.synnerz.devonian.api.splits

class InsulatingStage(children: Array<SplitStage>) : SplitStage(children) {
    override fun onChildStart(child: SplitStage) {
        if (!hasStarted()) _start()
    }
}