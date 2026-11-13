package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_forgot_password
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** The touch area extends above the caption so the text can sit directly over the field. */
@Composable
internal fun AuthFieldLabelLink(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    CoffeePeekTextButton(onClick, enabled = enabled, contentPadding = PaddingValues(0.dp)) {
        Box(Modifier.heightIn(min = CoffeePeekDimens.glassControlSize), contentAlignment = Alignment.BottomEnd) {
            Text(text, style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End, textDecoration = TextDecoration.Underline)
        }
    }
}

@Preview
@Composable
private fun AuthFieldLabelLinkLightPreview() = CoffeePeekTheme(darkTheme = false) {
    AuthFieldLabelLink(stringResource(Res.string.auth_forgot_password), {})
}

@Preview
@Composable
private fun AuthFieldLabelLinkDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    AuthFieldLabelLink(stringResource(Res.string.auth_forgot_password), {})
}
