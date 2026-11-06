package com.coffeepeek.core.media

import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coffeepeek.core.media.image.*
import io.kamel.core.Resource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Bounded, one-shot warming through the same renderer/cache as visible images. */
@Composable
fun PrefetchPhotos(urls: List<String>, inPhotoViewer: Boolean = false) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (!lifecycle.isAtLeast(Lifecycle.State.STARTED) || FullScreenPhotoPresence.active && !inPhotoViewer) return
    var started by remember(urls) { mutableStateOf(false) }
    // Visible rows claim transfer slots first; fast scrolls cancel speculative work.
    LaunchedEffect(urls) { delay(150); started = true }
    if (!started) return
    val candidates = urls.asSequence().filter(String::isNotBlank).distinct()
        .filter { !isProtectedCheckInPhoto(it) || isImmutablePhotoVariant(it) }
        .take(6).toList()
    var completed by remember(candidates) { mutableStateOf(emptySet<String>()) }
    // A global limit also covers simultaneous list/gallery prefetch windows.
    candidates.filterNot { it in completed }.take(2).forEach { url ->
        key(url) {
            var acquired by remember { mutableStateOf(false) }
            val finished = remember { CompletableDeferred<Unit>() }
            LaunchedEffect(url) {
                photoPrefetchRequests.withPermit {
                    acquired = true
                    finished.await()
                }
            }
            if (acquired) {
                val result = rememberImageResource(url, renewPermission = false)
                if (result is Resource.Success || result is Resource.Failure) {
                    SideEffect { completed = completed + url; finished.complete(Unit) }
                }
            }
        }
    }
}

private val photoPrefetchRequests = Semaphore(2)
