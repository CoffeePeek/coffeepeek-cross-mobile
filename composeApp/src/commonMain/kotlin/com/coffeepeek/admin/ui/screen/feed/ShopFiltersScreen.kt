package com.coffeepeek.admin.ui.screen.feed

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.SwipeDismissModalBottomSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.ui.component.CheckmarkSection
import com.coffeepeek.admin.ui.component.GroupSection
import com.coffeepeek.admin.ui.component.PriceBeanSlider
import com.coffeepeek.admin.ui.component.RoasterAvatar
import com.coffeepeek.admin.ui.component.priceLevelHint
import com.coffeepeek.admin.ui.component.brewMethodIcon
import com.coffeepeek.admin.ui.component.shopTagIcon
import org.jetbrains.compose.resources.painterResource

private val RowInset = 16.dp

/** iOS-style filter sheet: slides up, swipe down / back discards, «Готово» applies. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopFiltersScreen(
    state: FeedUiState,
    onDismiss: () -> Unit,
    onApply: (FeedFiltersUi) -> Unit,
) {
    var draft by remember(state.filters) { mutableStateOf(state.filters) }
    SwipeDismissModalBottomSheet(onDismissRequest = onDismiss) {
        SheetNavBar(
            resetEnabled = draft.activeFilterCount > 0,
            onReset = { draft = draft.clearSelections() },
            onDone = {
                onApply(draft)
                onDismiss()
            },
        )

        PlatformScrollableColumn(
            modifier = Modifier
                .weight(1f, fill = false)

                .navigationBarsPadding()
                .padding(horizontal = RowInset)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            GroupSection(
                title = "Цена",
                trailing = priceLevelHint(draft.priceRange) ?: "Любая",
            ) {
                PriceBeanSlider(
                    selected = draft.priceRange,
                    onSelect = { draft = draft.copy(priceRange = it) },
                    showTitle = false,
                    showHint = false,
                    modifier = Modifier.padding(horizontal = RowInset, vertical = 8.dp),
                )
            }
            CheckmarkSection(
                title = "Обжарщики",
                items = state.roasters,
                selectedIds = draft.roasterIds,
                onToggle = { id ->
                    draft = draft.copy(roasterIds = draft.roasterIds.toggle(id))
                },
                leading = { item -> RoasterAvatar(item) },
            )
            CheckmarkSection("Зёрна", state.beans, draft.beanIds, onToggle = {
                draft = draft.copy(beanIds = draft.beanIds.toggle(it))
            })
            CheckmarkSection("Оборудование", state.equipment, draft.equipmentIds, onToggle = {
                draft = draft.copy(equipmentIds = draft.equipmentIds.toggle(it))
            })
            CheckmarkSection(
                title = "Метод заваривания",
                items = state.brewMethods,
                selectedIds = draft.brewMethodIds,
                onToggle = { id ->
                    draft = draft.copy(brewMethodIds = draft.brewMethodIds.toggle(id))
                },
                leading = { item ->
                    Icon(
                        painter = painterResource(brewMethodIcon(item.name)),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
            CheckmarkSection(
                title = "Особенности",
                items = ShopTagGroups.amenityTags(state.shopTags),
                selectedIds = draft.tagIds,
                onToggle = { id ->
                    draft = draft.copy(tagIds = draft.tagIds.toggle(id))
                },
                leading = { item ->
                    Icon(
                        imageVector = shopTagIcon(item.slug),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun SheetGrabber() {
    Box(
        modifier = Modifier
            .padding(top = 6.dp, bottom = 4.dp)
            .size(width = 36.dp, height = 5.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
    )
}

@Composable
private fun SheetNavBar(
    resetEnabled: Boolean,
    onReset: () -> Unit,
    onDone: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp),
    ) {
        TextButton(
            onClick = onReset,
            enabled = resetEnabled,
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Text("Сбросить", fontSize = 17.sp, color = if (resetEnabled) CpColor.Primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
        Text(
            text = "Фильтры",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Center),
        )
        TextButton(
            onClick = onDone,
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Text("Готово", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = CpColor.Primary)
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> =
    if (contains(id)) this - id else this + id
