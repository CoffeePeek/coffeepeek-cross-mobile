package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekRatingSize
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.resources.*

@Composable
internal fun HomeCheckInEditorRating(
    rating: Int?, enabled: Boolean, artwork: @Composable (Modifier) -> Unit, onRate: (Int) -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
        .border(1.dp, MaterialTheme.colorScheme.outline, shape).padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        artwork(Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.home_check_in_taste_question), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            CoffeePeekRating((rating ?: 0).toDouble(), size = CoffeePeekRatingSize.Large,
                onRatingChange = onRate, enabled = enabled,
                inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                starDescription = { stringResource(Res.string.home_check_in_choose_rating, it) })
        }
    }
}

@Composable private fun HomeCheckInEditorRatingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { HomeCheckInEditorRating(5, true, { Icon(CoffeePeekIcons.Coffee, null, it) }, {}) }
}
@Preview @Composable private fun HomeCheckInEditorRatingLightPreview() = HomeCheckInEditorRatingPreview(false)
@Preview @Composable private fun HomeCheckInEditorRatingDarkPreview() = HomeCheckInEditorRatingPreview(true)
