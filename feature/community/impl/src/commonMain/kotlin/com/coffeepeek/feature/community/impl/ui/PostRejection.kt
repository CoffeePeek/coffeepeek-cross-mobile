package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.impl.resources.*
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun PostRejection(reason: String?, onEdit: (() -> Unit)?, enabled: Boolean) {
    Column {
        Text(stringResource(Res.string.community_label_rejected), style = MaterialTheme.typography.titleSmall)
        Text(reason?.takeIf(String::isNotBlank) ?: stringResource(Res.string.community_rejection_fallback),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        onEdit?.let { CoffeePeekTextButton(it, enabled = enabled) { Text(stringResource(Res.string.community_rejection_edit)) } }
    }
}

@Composable private fun RejectionPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { PostRejection(null, {}, true) }
}
@Preview @Composable private fun RejectionLightPreview() = RejectionPreview(false)
@Preview @Composable private fun RejectionDarkPreview() = RejectionPreview(true)
