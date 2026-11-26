@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.feature.ownership.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.uikit.LocalUIViewController
import com.coffeepeek.feature.ownership.api.OwnershipPhotoPicker
import com.coffeepeek.feature.ownership.domain.model.BusinessPhoto
import com.coffeepeek.feature.ownership.domain.model.OwnershipAccount
import com.coffeepeek.feature.ownership.impl.ui.OwnershipViewModel
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import platform.UIKit.UIViewController
import kotlin.uuid.Uuid

val LocalIosOwnershipContent = staticCompositionLocalOf<@Composable (IosOwnershipSession) -> UIViewController> {
    error("The application must supply native business ownership UI")
}

@Composable
internal actual fun PlatformOwnershipContent(model: OwnershipViewModel, account: OwnershipAccount,
    picker: OwnershipPhotoPicker, tab: Boolean, claim: Boolean, checkInLink: (String) -> String?,
    shareQr: (ByteArray, String) -> Unit, openBusiness: (String, String) -> Unit): Boolean {
    val factory = LocalIosOwnershipContent.current
    val state by model.state.collectAsState()
    var choosePhotos by remember { mutableStateOf<() -> Unit>({}) }
    val open by rememberUpdatedState(openBusiness)
    val session = remember(model) { IosOwnershipSession(model, account, tab, claim,
        { choosePhotos() }, checkInLink, shareQr, { kind, id -> open(kind, id) }) }
    val controller = factory(session)
    CompositionLocalProvider(LocalUIViewController provides controller) {
        OwnershipNativePhotoInput(state, model::onAction, picker) { choosePhotos = it }
    }
    if (claim) OwnershipNativeClaimPresentation(controller, model)
    else UIKitViewController(factory = { controller }, modifier = Modifier.fillMaxSize(),
        properties = UIKitInteropProperties(interactionMode = UIKitInteropInteractionMode.NonCooperative, isNativeAccessibilityEnabled = true))
    return true
}

@Composable
private fun OwnershipNativePhotoInput(state: OwnershipState, action: (OwnershipAction) -> Unit,
    picker: OwnershipPhotoPicker, setPicker: (() -> Unit) -> Unit) {
    val evidence = state.destination == OwnershipDestination.Submit
    val maximum = if (evidence) 3 else 10
    val count = if (evidence) state.submission.photos.size else state.draft?.photos.orEmpty().size
    key(state.generation, evidence) {
        picker((maximum - count).coerceAtLeast(1), { picked ->
            val photos = picked.map { BusinessPhoto(Uuid.random().toString(), fileName = it.fileName,
                contentType = it.contentType, size = it.bytes.size.toLong(), bytes = it.bytes.copyOf()) }
            action(if (evidence) OwnershipAction.AddEvidence(state.generation, photos)
                else OwnershipAction.AddPhotos(state.generation, photos))
        }) { add -> SideEffect { setPicker(add) } }
    }
}
