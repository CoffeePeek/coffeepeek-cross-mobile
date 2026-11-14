package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton
import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_title
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun GuestProfileScreen(bottomClearance: Dp, accountPrompt: @Composable () -> Unit) {
    CoffeePeekScaffold(contentWindowInsets = WindowInsets(0)) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).statusBarsPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = bottomClearance + CoffeePeekDimens.spacing4),
        ) {
            Text(
                stringResource(Res.string.profile_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = CoffeePeekDimens.spacing4),
            )
            accountPrompt()
        }
    }
}

@Composable
private fun GuestProfilePreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    GuestProfileScreen(bottomClearance = 64.dp) {
        CoffeePeekFilledButton(onClick = {}) { Text("Войти") }
    }
}

@Preview @Composable private fun GuestProfileLightPreview() = GuestProfilePreview(false)
@Preview @Composable private fun GuestProfileDarkPreview() = GuestProfilePreview(true)
