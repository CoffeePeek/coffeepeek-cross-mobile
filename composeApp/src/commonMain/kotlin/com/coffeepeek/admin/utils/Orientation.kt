package com.coffeepeek.admin.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Orientation {
    PORTRAIT,
    SQUARE,
    LANDSCAPE;

    companion object {

        fun getOrientation(size: IntSize): Orientation {
            return when (size.width / size.height.toFloat()) {
                in 0f..0.95f -> PORTRAIT
                in 1.25f..Float.MAX_VALUE -> LANDSCAPE
                else -> SQUARE
            }
        }

    }

}

object OrientationObserver {

    private val flow = MutableStateFlow(Orientation.SQUARE)
    val orientation = flow.asStateFlow()

    private val _size = MutableStateFlow(IntSize(0, 0))
    val size = _size.asStateFlow()

    @Composable
    fun StartObserver() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    flow.value = Orientation.getOrientation(it)
                    _size.value = it
                }
        )
    }


}