package com.coffeepeek.admin.ui.screen.checkins

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_create_home
import coffeepeek.composeapp.generated.resources.check_in_create_shop
import coffeepeek.composeapp.generated.resources.check_in_create_shelf_title
import coffeepeek.composeapp.generated.resources.check_in_create_title
import com.coffeepeek.admin.analytics.Analytics
import com.coffeepeek.admin.analytics.analyticsExposure
import com.coffeepeek.admin.theme.CoffeePeekTheme
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import com.coffeepeek.core.designsystem.component.CoffeePeekActionList
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInCreateSheet(onDismiss: () -> Unit, onShop: () -> Unit, onHome: () -> Unit) {
    SwipeDismissModalBottomSheet(onDismissRequest = onDismiss) {
        CheckInCreateContent(onShop, onHome)
    }
}

@Composable
internal fun CheckInCreateContent(onShop: () -> Unit, onHome: () -> Unit, onShelfBag: (() -> Unit)? = null) {
    val analyticsPage = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    CheckInCreateActions(onShop, onHome, onShelfBag,
        Modifier.analyticsExposure("home_check_in", "check_in_type_picker", analyticsPage))
}

@Composable
private fun CheckInCreateActions(
    onShop: () -> Unit,
    onHome: () -> Unit,
    onShelfBag: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val checkIns = listOf(
        CoffeePeekMenuAction(stringResource(Res.string.check_in_create_shop), CoffeePeekIcons.Location, onClick = onShop),
        CoffeePeekMenuAction(stringResource(Res.string.check_in_create_home), CoffeePeekIcons.Coffee, onClick = onHome),
    )
    val actions = checkIns + listOfNotNull(onShelfBag?.let {
        CoffeePeekMenuAction(stringResource(Res.string.check_in_create_shelf_title), CoffeePeekIcons.Archive, onClick = it)
    })
    Column(modifier.fillMaxWidth().padding(horizontal = 8.dp).padding(bottom = 16.dp)
        .verticalScroll(rememberScrollState())) {
        Text(stringResource(Res.string.check_in_create_title), style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).semantics { heading() })
        CoffeePeekActionList(actions.map { listOf(it) })
    }
}

@Preview @Composable private fun CheckInCreatePickerLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreateActions({}, {})
}

@Preview @Composable private fun CheckInCreatePickerDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreateActions({}, {})
}

@Preview @Composable private fun CheckInCreateShelfLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreateActions({}, {}, onShelfBag = {})
}

@Preview @Composable private fun CheckInCreateShelfDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreateActions({}, {}, onShelfBag = {})
}
