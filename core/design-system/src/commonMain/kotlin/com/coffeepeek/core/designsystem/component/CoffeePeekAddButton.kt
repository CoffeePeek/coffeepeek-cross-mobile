package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.modifier.LocalCoffeePeekNativeGlassPresenter
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekAddButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    loadingDescription: String? = null,
) {
    val description = if (isLoading) requireNotNull(loadingDescription) else contentDescription
    val buttonModifier = modifier.size(56.dp)
    if (LocalCoffeePeekNativeGlassPresenter.current?.IconButton(CoffeePeekGlassIcon.Add,
            onClick, description, buttonModifier, enabled && !isLoading, isLoading = isLoading) == true) return
    CoffeePeekFloatingActionButton(onClick = onClick, enabled = enabled && !isLoading,
        modifier = buttonModifier.semantics(mergeDescendants = true) {
            this.contentDescription = description
            if (isLoading) progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        if (isLoading) CoffeePeekLoader(description, modifier = Modifier.size(24.dp).clearAndSetSemantics {}, size = 24.dp,
            color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        else Icon(CoffeePeekIcons.Add, null, Modifier.size(28.dp))
    }
}

@Composable
private fun CoffeePeekAddButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Row {
        CoffeePeekAddButton({}, "Add")
        CoffeePeekAddButton({}, "Add", enabled = false)
        CoffeePeekAddButton({}, "Add", isLoading = true, loadingDescription = "Loading")
    }
}

@Preview @Composable private fun CoffeePeekAddButtonLightPreview() = CoffeePeekAddButtonPreview(false)
@Preview @Composable private fun CoffeePeekAddButtonDarkPreview() = CoffeePeekAddButtonPreview(true)
