package com.coffeepeek.admin.ui.screen.checkins

import com.coffeepeek.admin.analytics.*

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.check_in_add
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.theme.CoffeePeekTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun CheckInCreateButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val analyticsPage = Analytics.service?.state?.collectAsState()?.value?.page ?: "other"
    val description = stringResource(Res.string.check_in_add)
    FloatingActionButton(onClick = onClick, shape = CircleShape,
        modifier = modifier.analyticsExposure("check_in", "global_create", analyticsPage)
            .semantics(mergeDescendants = true) { contentDescription = description },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary) {
        Icon(CpIcons.Add, contentDescription = null, modifier = Modifier.size(28.dp))
    }
}

@Preview @Composable private fun CheckInCreateButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    CheckInCreateButton(Modifier.padding(16.dp), onClick = {})
}

@Preview @Composable private fun CheckInCreateButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    CheckInCreateButton(Modifier.padding(16.dp), onClick = {})
}
