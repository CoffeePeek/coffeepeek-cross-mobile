package com.coffeepeek.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

internal class CoffeePeekPhotoDragState {
    val bounds = mutableMapOf<Int, Rect>()
    private var origin: Rect? = null
    var index by mutableIntStateOf(-1)
        private set
    var target by mutableIntStateOf(-1)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    fun start(index: Int) {
        origin = bounds[index]
        this.index = index
        target = index
        offset = Offset.Zero
    }

    fun move(delta: Offset) {
        val origin = origin ?: return
        offset += delta
        target = bounds.entries.firstOrNull {
            it.key != index && it.value.contains(origin.center + offset)
        }?.key ?: index
    }

    fun finish(onMove: (Int, Int) -> Unit) {
        val from = index
        val to = target
        cancel()
        if (from >= 0 && to >= 0 && from != to) onMove(from, to)
    }

    fun cancel() {
        origin = null
        index = -1
        target = -1
        offset = Offset.Zero
    }
}
