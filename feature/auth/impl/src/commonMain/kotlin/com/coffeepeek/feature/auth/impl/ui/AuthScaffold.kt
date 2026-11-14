package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekPlainIconButton
import com.coffeepeek.core.designsystem.component.CoffeePeekScrollableColumn
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_back
import com.coffeepeek.feature.auth.impl.resources.auth_close
import com.coffeepeek.feature.auth.impl.resources.auth_login_title
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_info
import com.coffeepeek.feature.auth.impl.resources.auth_recovery_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun AuthScaffold(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeIcon: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AuthTopBar(title, onClose, closeIcon)
        CoffeePeekScrollableColumn(
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing6)
                    .padding(top = CoffeePeekDimens.spacing6, bottom = CoffeePeekDimens.spacing8),
                verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
                content = content,
            )
        }
    }
}

@Composable
private fun AuthTopBar(title: String, onClose: () -> Unit, closeIcon: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing2),
        verticalAlignment = Alignment.CenterVertically) {
        AuthBackButton(onClose, closeIcon)
        Text(title, Modifier.weight(1f).padding(vertical = CoffeePeekDimens.spacing2).semantics { heading() },
            style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.width(CoffeePeekDimens.glassControlSize))
    }
}

@Composable
private fun AuthBackButton(onClose: () -> Unit, closeIcon: Boolean) {
    val mirror = !closeIcon && LocalLayoutDirection.current == LayoutDirection.Rtl
    CoffeePeekPlainIconButton(onClose) {
        Icon(
            imageVector = if (closeIcon) CoffeePeekIcons.Close else CoffeePeekIcons.ChevronLeft,
            contentDescription = stringResource(if (closeIcon) Res.string.auth_close else Res.string.auth_back),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp).scale(if (mirror) -1f else 1f, 1f),
        )
    }
}

@Preview
@Composable
private fun AuthScaffoldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    AuthScaffold(stringResource(Res.string.auth_login_title), {}) {}
}

@Preview
@Composable
private fun AuthScaffoldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    AuthScaffold(stringResource(Res.string.auth_recovery_title), {}, closeIcon = true) {
        Text(stringResource(Res.string.auth_recovery_info))
    }
}
