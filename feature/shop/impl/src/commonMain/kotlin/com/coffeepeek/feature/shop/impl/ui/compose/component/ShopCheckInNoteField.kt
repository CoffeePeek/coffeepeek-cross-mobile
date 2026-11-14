package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.component.CoffeePeekCompactOutlinedTextField
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
        Text(stringResource(Res.string.shop_checkin_form_note), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        CoffeePeekCompactOutlinedTextField(value, onChange, enabled = enabled, isError = hasError,
            errorDescription = errorMessage.takeIf { hasError }, singleLine = false,
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                unfocusedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                errorContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                focusedBorderColor = MaterialTheme.colorScheme.outline),
            placeholder = { Text(stringResource(Res.string.shop_checkin_form_note_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) })
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
