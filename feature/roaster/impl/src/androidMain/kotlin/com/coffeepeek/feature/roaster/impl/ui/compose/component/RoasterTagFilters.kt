package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.domain.model.RoasterTag
import com.coffeepeek.feature.roaster.impl.R

@Composable
internal fun RoasterTagFilters(
    tags: List<RoasterTag>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (tags.isEmpty()) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        item { FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text(stringResource(R.string.roaster_tag_all)) }) }
        items(tags, key = { it.slug }) { tag ->
            FilterChip(selected = tag.slug in selected, onClick = { onToggle(tag.slug) }, label = { Text(tag.name) })
        }
    }
}

@PreviewLightDark @Composable private fun RoasterTagFiltersPreview() = CoffeePeekTheme {
    RoasterTagFilters(listOf(RoasterTag("light", "Светлая", null, 1), RoasterTag("decaf", "Декаф", null, 2)),
        setOf("decaf"), {}, {})
}
