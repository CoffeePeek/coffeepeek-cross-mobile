package com.coffeepeek.feature.profile.impl.ui.compose.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_section_favorites
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ProfileSectionHeading(title: String) {
    Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ProfileSectionHeadingPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface { ProfileSectionHeading(stringResource(Res.string.profile_section_favorites)) }
}
@Preview @Composable private fun ProfileSectionHeadingLightPreview() = ProfileSectionHeadingPreview(false)
@Preview @Composable private fun ProfileSectionHeadingDarkPreview() = ProfileSectionHeadingPreview(true)
