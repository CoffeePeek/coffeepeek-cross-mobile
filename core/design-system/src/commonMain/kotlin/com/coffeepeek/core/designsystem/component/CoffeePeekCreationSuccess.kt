package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.icons.CoffeePeekIllustrations
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIcon
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Shared completion layout; callers own localized copy, feedback and navigation. */
@Composable
fun CoffeePeekCreationSuccess(
    title: String,
    message: String,
    buttonLabel: String,
    closeLabel: String,
    onClose: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
) {
    CoffeePeekScaffold(
        modifier = modifier,
        topBar = { CreationSuccessHeader(title, closeLabel, onClose) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(CoffeePeekDimens.spacing4)) {
                CoffeePeekButton(buttonLabel, onDone)
            }
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight).padding(CoffeePeekDimens.spacing6),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CreationSuccessMessage(message)
                supportingContent()
            }
        }
    }
}

@Composable
private fun CreationSuccessHeader(title: String, closeLabel: String, onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(CoffeePeekDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        CoffeePeekGlassIconButton(onClose, closeLabel, nativeIcon = CoffeePeekGlassIcon.Close) {
            CoffeePeekIcon(CoffeePeekIcons.Close, null, modifier = Modifier.size(20.dp))
        }
        Text(
            title, Modifier.weight(1f).semantics { heading(); liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(CoffeePeekDimens.glassControlSize))
    }
}

@Composable
private fun CreationSuccessMessage(message: String) {
    Image(
        painterResource(CoffeePeekIllustrations.CreationSuccess), contentDescription = null,
        contentScale = ContentScale.Fit, modifier = Modifier.size(180.dp),
    )
    Text(
        message, modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth(),
        style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CreationSuccessPreview(dark: Boolean, fontScale: Float = 1f) = CoffeePeekTheme(darkTheme = dark) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
        CoffeePeekCreationSuccess(
            "Thank you!", "Your contribution has been sent for review. It will appear after moderation.",
            "Done", "Close", {}, {},
        )
    }
}

@Preview @Composable private fun CreationSuccessLightPreview() = CreationSuccessPreview(false)
@Preview @Composable private fun CreationSuccessDarkPreview() = CreationSuccessPreview(true)
@Preview @Composable private fun CreationSuccessLargeTextLightPreview() = CreationSuccessPreview(false, 2f)
@Preview @Composable private fun CreationSuccessLargeTextDarkPreview() = CreationSuccessPreview(true, 2f)
