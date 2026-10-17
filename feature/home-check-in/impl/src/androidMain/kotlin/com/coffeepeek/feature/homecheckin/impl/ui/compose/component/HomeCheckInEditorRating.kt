package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.R

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
            Text(stringResource(R.string.home_check_in_taste_question), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            // Equal weights retain 44dp at normal width and avoid overflow on narrow windows.
            Row(Modifier.fillMaxWidth().widthIn(max = 224.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                (1..5).forEach { score ->
                    val description = stringResource(R.string.home_check_in_choose_rating, score)
                    Box(Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(8.dp))
                        .selectable(selected = rating == score, enabled = enabled, role = Role.Button) { onRate(score) }
                        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
                        Icon(if (score <= (rating ?: 0)) CoffeePeekIcons.StarFilled else CoffeePeekIcons.StarOutline, null,
                            Modifier.size(24.dp), tint = if (score <= (rating ?: 0)) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f))
                    }
                }
            }
        }
    }
}

@PreviewLightDark @Composable private fun HomeCheckInEditorRatingPreview() = CoffeePeekTheme {
    Surface { HomeCheckInEditorRating(5, true, { Icon(CoffeePeekIcons.Coffee, null, it) }, {}) }
}
