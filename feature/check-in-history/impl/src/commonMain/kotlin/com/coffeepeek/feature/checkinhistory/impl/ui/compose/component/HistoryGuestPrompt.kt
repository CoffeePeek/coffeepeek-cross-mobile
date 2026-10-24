package com.coffeepeek.feature.checkinhistory.impl.ui.compose.component

import androidx.compose.foundation.layout.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.checkinhistory.impl.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HistoryGuestPrompt(onSignIn: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(CoffeePeekDimens.spacing6), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Text(stringResource(Res.string.history_sign_in_prompt))
        CoffeePeekButton(stringResource(Res.string.history_sign_in), onSignIn)
    }
}

@Preview @Composable private fun HistoryGuestPromptPreview() = CoffeePeekTheme {
    HistoryGuestPrompt({})
}
