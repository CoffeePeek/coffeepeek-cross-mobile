package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekMultilineTextField
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_note
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_note_hint
import com.coffeepeek.feature.shop.impl.resources.shop_checkin_form_invalid_note
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCheckInNoteField(value: String, enabled: Boolean, hasError: Boolean, onChange: (String) -> Unit) {
    val errorMessage = stringResource(Res.string.shop_checkin_form_invalid_note)
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        val label = stringResource(Res.string.shop_checkin_form_note)
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoffeePeekMultilineTextField(value, onChange, label,
            stringResource(Res.string.shop_checkin_form_note_hint), 1000,
            enabled = enabled, errorDescription = errorMessage.takeIf { hasError })
        Text("${value.length}/1000", Modifier.align(Alignment.End), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hasError) Text(errorMessage, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall)
    }
}

@Preview @Composable private fun ShopCheckInNoteFieldLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCheckInNoteField("Хороший фильтр и приятная атмосфера", true, false, {})
}
@Preview @Composable private fun ShopCheckInNoteFieldDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCheckInNoteField("", true, true, {})
}
