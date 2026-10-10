package com.coffeepeek.feature.favorites.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpColor
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.favorites.impl.resources.Res
import com.coffeepeek.feature.favorites.impl.resources.favorites_closed
import com.coffeepeek.feature.favorites.impl.resources.favorites_open
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun OpenStatusBadge(isOpen: Boolean, modifier: Modifier = Modifier) {
    val color = if (isOpen) CpColor.Success else MaterialTheme.colorScheme.error
    Row(
        modifier = modifier.clip(RoundedCornerShape(CpDimens.radiusSm))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = CpDimens.spacing2, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(stringResource(if (isOpen) Res.string.favorites_open else Res.string.favorites_closed),
            style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.Bold)
    }
}

@Preview @Composable private fun OpenStatusBadgeLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OpenStatusBadge(true)
}
@Preview @Composable private fun OpenStatusBadgeDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OpenStatusBadge(false)
}
