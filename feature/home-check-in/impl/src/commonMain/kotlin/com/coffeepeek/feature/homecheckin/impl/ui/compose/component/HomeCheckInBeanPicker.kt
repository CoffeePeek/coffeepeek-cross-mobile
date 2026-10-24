package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.foundation.clickable
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_back_to_editor
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_bean
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_bean_search
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_beans_empty
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_beans_empty_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_beans_failed
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_beans_loading
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_clear_search
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_custom_bean
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_custom_bean_hint
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_custom_bean_name
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_retry
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_use_custom_bean
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.impl.ui.compose.model.*

@Composable
internal fun HomeCheckInBeanPicker(
    state: HomeCheckInEditorState, onAction: (HomeCheckInEditorAction) -> Unit,
    emptyArtwork: @Composable (Modifier) -> Unit, modifier: Modifier = Modifier,
) {
    var customName by rememberSaveable { mutableStateOf(state.draft.customBeanName.orEmpty()) }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onAction(HomeCheckInEditorAction.CloseBeanPicker) }, Modifier.size(44.dp)) {
                Icon(CoffeePeekIcons.Back, stringResource(Res.string.home_check_in_back_to_editor))
            }
            Text(stringResource(Res.string.home_check_in_bean), style = MaterialTheme.typography.headlineSmall)
        }
        CoffeePeekSearchField(state.beanQuery, { onAction(HomeCheckInEditorAction.BeanQueryChanged(it)) },
            stringResource(Res.string.home_check_in_bean_search), stringResource(Res.string.home_check_in_clear_search),
            modifier = Modifier.fillMaxWidth(), onSearch = { onAction(HomeCheckInEditorAction.SearchBeans) })
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when {
                state.beansLoading -> CoffeePeekLoader(stringResource(Res.string.home_check_in_beans_loading))
                state.beansIssue != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(Res.string.home_check_in_beans_failed), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { onAction(HomeCheckInEditorAction.SearchBeans) }) { Text(stringResource(Res.string.home_check_in_retry)) }
                }
                state.beans.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    emptyArtwork(Modifier.size(120.dp))
                    Text(stringResource(Res.string.home_check_in_beans_empty), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(stringResource(Res.string.home_check_in_beans_empty_hint), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                else -> LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.beans, key = { it.slug }) { bean ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button) { onAction(HomeCheckInEditorAction.BeanSelected(bean.slug)) }
                            .padding(horizontal = 8.dp, vertical = 12.dp)) {
                            Text(bean.name, style = MaterialTheme.typography.bodyLarge)
                            Text(bean.roasterName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
        Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeCheckInEditorField(stringResource(Res.string.home_check_in_custom_bean)) {
                HomeCheckInEditorInput(customName, { customName = it }, stringResource(Res.string.home_check_in_custom_bean_name),
                    stringResource(Res.string.home_check_in_custom_bean_hint), true, maxLength = 250)
            }
            CoffeePeekButton(stringResource(Res.string.home_check_in_use_custom_bean),
                { onAction(HomeCheckInEditorAction.CustomBeanChosen(customName.trim())) }, enabled = customName.isNotBlank())
        }
    }
}

@Preview @Composable private fun HomeCheckInBeanPickerPreview() = CoffeePeekTheme {
    Surface { HomeCheckInBeanPicker(HomeCheckInEditorState(isBeanPickerOpen = true), {},
        { Icon(CoffeePeekIcons.Search, null, it) }) }
}
