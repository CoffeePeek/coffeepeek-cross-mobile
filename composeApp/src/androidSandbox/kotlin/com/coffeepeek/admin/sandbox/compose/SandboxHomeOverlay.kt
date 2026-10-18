package com.coffeepeek.admin.sandbox.compose

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.coffeepeek.R
import com.coffeepeek.admin.sandbox.SandboxDestination
import com.coffeepeek.admin.sandbox.SandboxViewModel
import com.coffeepeek.core.designsystem.component.CoffeePeekSwipeDismissModalBottomSheet

/** Testing host around the real feature entries; no duplicate Home screen or business ViewModel. */
@Composable internal fun SandboxHomeOverlay(model: SandboxViewModel, destination: SandboxDestination,
    dismissAllowed: Boolean, generation: Int) {
    key(destination) {
        CoffeePeekSwipeDismissModalBottomSheet(
            onDismissRequest = { model.dismiss(destination) }, dismissDescription = stringResource(R.string.sandbox_close),
            dismissEnabled = dismissAllowed, maxSheetHeightFraction = 0.9f,
        ) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides destination) {
                val modifier = Modifier.weight(1f).fillMaxWidth()
                if (destination.editing) model.environment.editor.Content(destination.id, generation, true,
                    { model.dismiss(destination) }, { id, _ -> model.saved(destination, id) },
                    { model.busy(destination, it) }, modifier)
                else model.environment.detail.Content(requireNotNull(destination.id), generation, true,
                    { date, _ -> date }, { it }, { model.dismiss(destination) },
                    { id -> model.open(id, true) }, { model.deleted(destination) }, model::photos,
                    { model.busy(destination, it) }, modifier)
            }
        }
    }
}
