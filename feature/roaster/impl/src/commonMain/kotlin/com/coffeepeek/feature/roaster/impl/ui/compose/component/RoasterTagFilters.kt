package com.coffeepeek.feature.roaster.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.roaster.domain.model.RoasterTag
import com.coffeepeek.feature.roaster.impl.resources.Res
import com.coffeepeek.feature.roaster.impl.resources.roaster_tag_all

@Composable
internal fun RoasterTagFilters(
    tags: List<RoasterTag>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (tags.isEmpty()) return
    LazyRow(contentPadding = PaddingValues(horizontal = CoffeePeekDimens.spacing4),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        item { FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text(stringResource(Res.string.roaster_tag_all)) }) }
        items(tags, key = { it.slug }) { tag ->
            FilterChip(selected = tag.slug in selected, onClick = { onToggle(tag.slug) }, label = { Text(tag.name) })
        }
    }
}

@Preview @Composable private fun RoasterTagFiltersLightPreview() = CoffeePeekTheme(darkTheme = false) {
    RoasterTagFilters(listOf(RoasterTag("light", "Светлая", null, 1), RoasterTag("decaf", "Декаф", null, 2)),
        setOf("decaf"), {}, {})
}

@Preview @Composable private fun RoasterTagFiltersDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    RoasterTagFilters(listOf(RoasterTag("light", "Светлая", null, 1), RoasterTag("decaf", "Декаф", null, 2)),
        setOf("decaf"), {}, {})
}
