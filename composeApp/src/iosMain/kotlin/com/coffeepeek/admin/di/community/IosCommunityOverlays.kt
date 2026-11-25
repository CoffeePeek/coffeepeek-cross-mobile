package com.coffeepeek.admin.di.community

import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.snackbar_dismiss
import com.coffeepeek.admin.di.media.IosCheckInPhotoPresentation
import com.coffeepeek.feature.community.impl.NativeCommunityPhoto
import com.coffeepeek.feature.community.impl.NativeCommunitySheet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString
import platform.UIKit.UIAccessibilityIsVoiceOverRunning

class IosCommunityOverlayState internal constructor(val sheet: IosCommunitySheet?, val photo: IosCheckInPhotoPresentation?,
    val message: IosCommunityMessage?, val bottomClearance: Double)

/** The Swift view owns modal presentation. This owner only adapts and revokes their existing sources. */
class IosCommunityOverlays internal constructor(private val scope: CoroutineScope, private val isCurrent: () -> Boolean,
    private val createSheet: (String, Boolean, (String, String) -> Unit) -> IosCommunitySheet,
    private val createPhoto: (NativeCommunityPhoto) -> IosCheckInPhotoPresentation,
) {
    private val state = MutableStateFlow(IosCommunityOverlayState(null, null, null, 0.0))
    private val messages = Mutex()
    private var messageCompletion: CompletableDeferred<Unit>? = null
    private var sheetKey: String? = null
    private var photoKey: Long? = null
    private var closed = false

    fun current() = state.value
    fun observe(onState: (IosCommunityOverlayState) -> Unit) = IosCommunityOverlayObservation(scope.launch { state.collect(onState) })
    fun dismissSheet(sheet: IosCommunitySheet) { if (active() && state.value.sheet === sheet) sheet.perform("dismiss", "") }
    fun dismissPhoto(photo: IosCheckInPhotoPresentation) { if (active() && state.value.photo === photo) photo.dismiss() }

    internal fun update(sheet: NativeCommunitySheet?, photo: NativeCommunityPhoto?, bottomClearance: Double) {
        if (!active()) return
        updateSheet(sheet)
        updatePhoto(photo.takeIf { sheet == null })
        state.value = IosCommunityOverlayState(state.value.sheet, state.value.photo, state.value.message, bottomClearance)
    }

    private fun updateSheet(next: NativeCommunitySheet?) {
        if (sheetKey != next?.key) {
            state.value.sheet?.close()
            sheetKey = next?.key
            state.value = IosCommunityOverlayState(null, state.value.photo, state.value.message, state.value.bottomClearance)
        }
        if (next == null) return
        when (next) {
            is NativeCommunitySheet.Editor -> setSheet(next.data.nativeJson(), next.data.busy, next.actions::perform)
            is NativeCommunitySheet.Report -> setSheet(next.data.nativeJson(), next.data.busy, next.actions::perform)
        }
    }

    private fun setSheet(json: String, busy: Boolean, actions: (String, String) -> Unit) {
        val source = state.value.sheet
        if (source != null) source.update(json, busy, actions)
        else state.value = IosCommunityOverlayState(createSheet(json, busy, actions), state.value.photo, state.value.message, state.value.bottomClearance)
    }

    private fun updatePhoto(next: NativeCommunityPhoto?) {
        if (photoKey == next?.key) return
        state.value.photo?.close()
        photoKey = next?.key
        state.value = IosCommunityOverlayState(state.value.sheet, next?.let(createPhoto), state.value.message, state.value.bottomClearance)
    }

    internal suspend fun message(text: String) = messages.withLock {
        if (!active()) return@withLock
        val completion = CompletableDeferred<Unit>(currentCoroutineContext()[Job])
        val dismiss = getString(Res.string.snackbar_dismiss)
        val source = IosCommunityMessage(text, null, dismiss, { active() && messageCompletion === completion },
            { completion.complete(Unit) }, {})
        messageCompletion = completion
        state.value = IosCommunityOverlayState(state.value.sheet, state.value.photo, source, state.value.bottomClearance)
        try {
            if (UIAccessibilityIsVoiceOverRunning()) completion.await() else withTimeoutOrNull(4_000) { completion.await() }
        } finally {
            finishMessage(completion, source)
        }
    }

    private fun finishMessage(completion: CompletableDeferred<Unit>, source: IosCommunityMessage) {
        if (messageCompletion === completion) {
            messageCompletion = null
            state.value = IosCommunityOverlayState(state.value.sheet, state.value.photo, null, state.value.bottomClearance)
        }
        source.close()
        completion.cancel()
    }

    internal fun close() {
        if (closed) return
        closed = true
        messageCompletion?.cancel()
        messageCompletion = null
        state.value.sheet?.close()
        state.value.photo?.close()
        state.value.message?.close()
        state.value = IosCommunityOverlayState(null, null, null, 0.0)
    }

    private fun active() = !closed && isCurrent()
}

class IosCommunityOverlayObservation internal constructor(private val job: Job) { fun cancel() = job.cancel() }
