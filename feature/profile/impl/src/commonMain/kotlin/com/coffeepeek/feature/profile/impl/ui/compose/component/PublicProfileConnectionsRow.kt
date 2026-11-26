package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekOutlinedButton
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.text.coffeePeekPluralForm
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun PublicProfileConnectionsRow(followers: Int, following: Int?, onFollowers: (() -> Unit)?, onFollowing: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileConnectionButton(connectionLabel(followers, false), CoffeePeekIcons.Community, onFollowers, Modifier.weight(1f))
        ProfileConnectionButton(connectionLabel(following, true), CoffeePeekIcons.User, onFollowing, Modifier.weight(1f))
    }
}

@Composable
private fun ProfileConnectionButton(title: String, icon: ImageVector, action: (() -> Unit)?, modifier: Modifier) {
    CoffeePeekOutlinedButton(onClick = { action?.invoke() }, enabled = action != null,
        modifier = modifier.heightIn(min = 48.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            CoffeePeekIcon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun connectionLabel(count: Int?, following: Boolean): String {
    if (count == null) return stringResource(Res.string.profile_following_title)
    val resource = if (following) coffeePeekPluralForm(count, Res.string.profile_connections_following_one,
        Res.string.profile_connections_following_few, Res.string.profile_connections_following_many)
    else coffeePeekPluralForm(count, Res.string.profile_connections_followers_one,
        Res.string.profile_connections_followers_few, Res.string.profile_connections_followers_many)
    return stringResource(resource, count)
}

@Composable
private fun ProfileConnectionsPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PublicProfileConnectionsRow(9, 1, {}, {})
        PublicProfileConnectionsRow(0, 0, {}, {})
        PublicProfileConnectionsRow(12, null, null, null)
    }
}

@Preview @Composable private fun ProfileConnectionsLightPreview() = ProfileConnectionsPreview(false)
@Preview @Composable private fun ProfileConnectionsDarkPreview() = ProfileConnectionsPreview(true)
