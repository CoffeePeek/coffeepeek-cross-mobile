package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Keeps native content mounted, but pauses it and omits placement while hidden. */
@Composable
internal fun RetainedContent(visible: Boolean, content: @Composable () -> Unit) {
    val parent = LocalLifecycleOwner.current.lifecycle
    val owner = remember {
        object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
    }
    DisposableEffect(parent, visible) {
        fun sync() {
            owner.lifecycle.currentState = if (visible) parent.currentState
                else minOf(parent.currentState, Lifecycle.State.CREATED)
        }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        parent.addObserver(observer)
        sync()
        onDispose { parent.removeObserver(observer) }
    }
    DisposableEffect(owner) {
        onDispose { owner.lifecycle.currentState = Lifecycle.State.DESTROYED }
    }
    Layout(
        content = { CompositionLocalProvider(LocalLifecycleOwner provides owner, content = content) },
        modifier = Modifier.fillMaxSize().then(if (visible) Modifier else Modifier.clearAndSetSemantics {}),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (visible) placeables.forEach { it.placeRelative(0, 0) }
        }
    }
}
