package com.coffeepeek.feature.shelf.impl.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekSwitch
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.component.CoffeePeekSettingsRow
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.ShelfProfileVisibilityViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.model.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

@Composable
internal fun ShelfProfileVisibilityScreen(model: ShelfProfileVisibilityViewModel, onSignIn: () -> Unit, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val signIn by rememberUpdatedState(onSignIn)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.onAction(ShelfProfileVisibilityAction.Refresh) }
    LaunchedEffect(model) { model.events.collect { if (it == ShelfProfileVisibilityEvent.SignIn) signIn() } }
    ShelfProfileVisibilityContent(state, model::onAction, modifier)
}

@Composable
internal fun ShelfProfileVisibilityContent(state: ShelfProfileVisibilityState,
    action: (ShelfProfileVisibilityAction) -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.shelf_profile_show)
    val enabled = state.signedIn && state.loaded && !state.loading && !state.busy
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CoffeePeekSettingsRow(CoffeePeekIcons.CoffeeBean, label, stringResource(Res.string.shelf_profile_show_hint),
            onClick = { action(ShelfProfileVisibilityAction.SetVisible(!state.visible)) }, enabled = enabled,
            role = Role.Switch, showArrow = false,
            modifier = Modifier.semantics { toggleableState = ToggleableState(state.visible) },
            trailing = { CoffeePeekSwitch(state.visible, null, Modifier.clearAndSetSemantics { }, enabled) })
        ShelfProfileVisibilityStatus(state, action)
    }
}

@Composable
private fun ShelfProfileVisibilityStatus(state: ShelfProfileVisibilityState, action: (ShelfProfileVisibilityAction) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.loading || state.busy) CoffeePeekLoader(stringResource(Res.string.shelf_profile_updating), size = 20.dp)
        if (!state.signedIn) CoffeePeekTextButton({ action(ShelfProfileVisibilityAction.SignIn) }) {
            Text(stringResource(Res.string.shelf_sign_in))
        }
        state.error?.let { error ->
            Text(stringResource(if (error == ShelfProfileVisibilityError.Load) Res.string.shelf_profile_visibility_load_failed
                else Res.string.shelf_profile_visibility_save_failed), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            if (error == ShelfProfileVisibilityError.Load) CoffeePeekTextButton({ action(ShelfProfileVisibilityAction.Refresh) }) {
                Text(stringResource(Res.string.shelf_retry))
            }
        }
    }
}

@Composable
private fun ShelfProfileVisibilityPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        ShelfProfileVisibilityContent(ShelfProfileVisibilityState(signedIn = true, loaded = true), {})
        ShelfProfileVisibilityContent(ShelfProfileVisibilityState(signedIn = true, loaded = true, visible = true), {})
    }
}

@Preview @Composable private fun ShelfProfileVisibilityLightPreview() = ShelfProfileVisibilityPreview(false)
@Preview @Composable private fun ShelfProfileVisibilityDarkPreview() = ShelfProfileVisibilityPreview(true)
