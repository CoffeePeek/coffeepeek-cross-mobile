package com.coffeepeek.feature.homecheckin.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.coffeepeek.core.designsystem.component.CoffeePeekCreationSuccess
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationVisibility
import com.coffeepeek.feature.homecheckin.impl.resources.Res
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_close
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_done
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_saved_description
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_saved_private
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_saved_unknown
import com.coffeepeek.feature.homecheckin.impl.resources.home_check_in_saved_visibility_changed
import com.coffeepeek.feature.homecheckin.impl.ui.compose.previewHomePreparation
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun HomeCheckInSavedContent(entry: HomePreparation, visibilityUnconfirmed: Boolean,
    onDone: () -> Unit, modifier: Modifier = Modifier) {
    CoffeePeekCreationSuccess(
        title = stringResource(Res.string.home_check_in_saved_unknown),
        message = stringResource(if (entry.visibility == HomePreparationVisibility.Private)
            Res.string.home_check_in_saved_private else Res.string.home_check_in_saved_description),
        buttonLabel = stringResource(Res.string.home_check_in_done),
        closeLabel = stringResource(Res.string.home_check_in_close),
        onClose = onDone,
        onDone = onDone,
        modifier = modifier,
    ) {
        if (visibilityUnconfirmed) Text(
            stringResource(Res.string.home_check_in_saved_visibility_changed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable private fun HomeCheckInSavedPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    HomeCheckInSavedContent(previewHomePreparation(), dark, {})
}

@Preview @Composable private fun HomeCheckInSavedLightPreview() = HomeCheckInSavedPreview(false)
@Preview @Composable private fun HomeCheckInSavedDarkPreview() = HomeCheckInSavedPreview(true)
