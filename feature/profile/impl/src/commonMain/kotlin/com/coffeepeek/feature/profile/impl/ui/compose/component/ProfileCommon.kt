package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_loading
import com.coffeepeek.feature.profile.impl.resources.profile_retry
import com.coffeepeek.feature.profile.impl.resources.profile_section_all
import com.coffeepeek.feature.profile.impl.resources.profile_section_all_description
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons

@Composable
internal fun ProfileCard(modifier: Modifier = Modifier.fillMaxWidth(), content: @Composable () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)),
        content = content,
    )
}

@Composable
internal fun ProfileEmptyCard(text: String, icon: ImageVector) {
    ProfileCard {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(23.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ProfileSectionLoader(height: androidx.compose.ui.unit.Dp = 88.dp) {
    Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        CoffeePeekLoader(stringResource(Res.string.profile_loading))
    }
}

@Composable
internal fun ProfileSectionHeading(title: String, onAll: () -> Unit) {
    val description = stringResource(Res.string.profile_section_all_description, title)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        TextButton(
            onClick = onAll,
            modifier = Modifier.semantics { contentDescription = description },
            contentPadding = PaddingValues(start = 12.dp),
        ) {
            Text(stringResource(Res.string.profile_section_all), color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(3.dp))
            Icon(CoffeePeekIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
internal fun InlineProfileError(text: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text(stringResource(Res.string.profile_retry)) }
    }
}
