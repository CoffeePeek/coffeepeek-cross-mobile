package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.domain.SocialProvider
import com.coffeepeek.feature.auth.impl.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

/** Provider branding is auth-owned; both buttons keep the production pill geometry and palette. */
@Composable
internal fun SocialSignInButton(provider: SocialProvider, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(50)
    OutlinedButton(
        onClick = onClick, enabled = enabled, shape = shape,
        modifier = modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.controlHeight)
            .shadow(if (enabled && !dark) 3.dp else 0.dp, shape,
                ambientColor = Color.Black.copy(alpha = .06f), spotColor = Color.Black.copy(alpha = .06f)),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        border = BorderStroke(1.dp, if (dark) MaterialTheme.colorScheme.outline else Color(0xFFE9E9EC)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (dark) MaterialTheme.colorScheme.surface else Color.White,
            contentColor = if (dark) Color.White else Color(0xFF15171C),
            disabledContainerColor = if (dark) MaterialTheme.colorScheme.surface else Color.White,
        ),
    ) { SocialSignInLabel(provider) }
}

@Composable
private fun SocialSignInLabel(provider: SocialProvider) {
    if (provider == SocialProvider.Apple) Icon(painterResource(Res.drawable.auth_ic_apple), null, Modifier.size(18.dp))
    else Image(painterResource(Res.drawable.auth_ic_google_g), null, Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(socialSignInLabel(provider), style = socialSignInTextStyle())
}

@Composable
internal fun socialSignInLabel(provider: SocialProvider): String = stringResource(
    if (provider == SocialProvider.Apple) Res.string.auth_sign_in_apple else Res.string.auth_sign_in_google,
)

@Composable
internal fun socialSignInTextStyle() = MaterialTheme.typography.labelLarge.copy(
    fontSize = 12.sp, lineHeight = 18.sp, letterSpacing = 0.sp, fontWeight = FontWeight.Normal,
)

@Composable
private fun SocialSignInButtonPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    Surface {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SocialProvider.entries.forEach { SocialSignInButton(it, {}, true) }
        }
    }
}

@Preview @Composable private fun SocialSignInButtonLightPreview() = SocialSignInButtonPreview(false)
@Preview @Composable private fun SocialSignInButtonDarkPreview() = SocialSignInButtonPreview(true)
