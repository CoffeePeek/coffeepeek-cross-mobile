package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_note
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_note_hint
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_note_count
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_note
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInNoteField(value: String, enabled: Boolean, hasError: Boolean, onChange: (String) -> Unit) {
    val errorMessage = stringResource(Res.string.shop_checkin_form_invalid_note)
    OutlinedTextField(
        value = value, onValueChange = onChange, enabled = enabled, isError = hasError,
        label = { Text(stringResource(Res.string.shop_checkin_form_note)) },
        placeholder = { Text(stringResource(Res.string.shop_checkin_form_note_hint)) },
        supportingText = {
            Text(if (hasError) errorMessage
                else stringResource(Res.string.shop_checkin_form_note_count, value.length))
        },
        minLines = 4, modifier = Modifier.fillMaxWidth().semantics {
            if (hasError) error(errorMessage)
        },
    )
}

@Preview @Composable private fun ShopCheckInNoteFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInNoteField("Хороший фильтр и приятная атмосфера", true, false, {})
}
@Preview @Composable private fun ShopCheckInNoteFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInNoteField("", true, true, {})
}
