package com.coffeepeek.admin.ui.screen.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.ic_apple_sign_in
import coffeepeek.composeapp.generated.resources.ic_google_g
import coffeepeek.composeapp.generated.resources.sign_in_apple
import coffeepeek.composeapp.generated.resources.sign_in_google
import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.admin.auth.AppleSignInButton
import com.coffeepeek.admin.auth.GoogleSignInButton
import com.coffeepeek.admin.auth.isGoogleSignInConfigured
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val SocialButtonIconSize = 18.dp
private val SocialButtonIconGap = 8.dp
private val SocialButtonPadding = 6.dp
private val SocialButtonSpacing = 8.dp

internal enum class SocialSignInBrand { Apple, Google }

@Composable
private fun socialButtonTextStyle(): TextStyle = MaterialTheme.typography.labelLarge.copy(
    fontSize = 12.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.sp,
    fontWeight = FontWeight.Normal,
)

@Composable
internal fun SocialSignInButton(
    brand: SocialSignInBrand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shape = RoundedCornerShape(50)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CpDimens.authPrimaryBtnHeight)
            .shadow(
                elevation = if (enabled && !isDark) 3.dp else 0.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f),
            ),
        shape = shape,
        border = BorderStroke(
            1.dp,
            if (isDark) MaterialTheme.colorScheme.outline else Color(0xFFE9E9EC),
        ),
        contentPadding = PaddingValues(horizontal = SocialButtonPadding, vertical = 8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            contentColor = if (isDark) Color.White else Color(0xFF15171C),
            disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
        ),
    ) {
        when (brand) {
            SocialSignInBrand.Apple -> Icon(
                painter = painterResource(Res.drawable.ic_apple_sign_in),
                contentDescription = null,
                modifier = Modifier.size(SocialButtonIconSize),
            )
            SocialSignInBrand.Google -> Image(
                painter = painterResource(Res.drawable.ic_google_g),
                contentDescription = null,
                modifier = Modifier.size(SocialButtonIconSize),
            )
        }
        Spacer(Modifier.width(SocialButtonIconGap))
        Text(
            text = stringResource(
                when (brand) {
                    SocialSignInBrand.Apple -> Res.string.sign_in_apple
                    SocialSignInBrand.Google -> Res.string.sign_in_google
                },
            ),
            style = socialButtonTextStyle(),
        )
    }
}

@Preview
@Composable
private fun AppleSignInButtonLightPreview() = CoffeePeekTheme(darkTheme = false) {
    SocialSignInButton(brand = SocialSignInBrand.Apple, onClick = {})
}

@Preview
@Composable
private fun AppleSignInButtonDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    SocialSignInButton(brand = SocialSignInBrand.Apple, onClick = {})
}

@Composable
internal fun AuthSocialSignInButtons(
    onAppleClick: () -> Unit,
    appleEnabled: Boolean,
    onGoogleResult: (Result<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showApple = AppleAuth.isSupported()
    val showGoogle = isGoogleSignInConfigured()
    if (!showApple && !showGoogle) return

    val textMeasurer = rememberTextMeasurer()
    val textStyle = socialButtonTextStyle()
    val labelWidth = maxOf(
        textMeasurer.measure(stringResource(Res.string.sign_in_apple), textStyle).size.width,
        textMeasurer.measure(stringResource(Res.string.sign_in_google), textStyle).size.width,
    )
    val minButtonWidth = with(LocalDensity.current) { labelWidth.toDp() } +
        SocialButtonIconSize + SocialButtonIconGap + SocialButtonPadding * 2

    AuthSocialSignInLayout(
        showApple, showGoogle, minButtonWidth, modifier,
        appleButton = { AppleSignInButton(onClick = onAppleClick, enabled = appleEnabled, modifier = it) },
        googleButton = { GoogleSignInButton(onResult = onGoogleResult, modifier = it) },
    )
}

@Composable
private fun AuthSocialSignInLayout(
    showApple: Boolean,
    showGoogle: Boolean,
    minButtonWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    appleButton: @Composable (Modifier) -> Unit,
    googleButton: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (showApple && showGoogle && maxWidth >= minButtonWidth * 2 + SocialButtonSpacing) {
            Row(horizontalArrangement = Arrangement.spacedBy(SocialButtonSpacing)) {
                appleButton(Modifier.weight(1f))
                googleButton(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SocialButtonSpacing)) {
                if (showApple) appleButton(Modifier.fillMaxWidth())
                if (showGoogle) googleButton(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AuthSocialSignInPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AuthSocialSignInLayout(
        true, true, 180.dp,
        appleButton = { SocialSignInButton(SocialSignInBrand.Apple, {}, modifier = it) },
        googleButton = { SocialSignInButton(SocialSignInBrand.Google, {}, modifier = it) },
    )
}

@Preview(widthDp = 280) @Composable
private fun AuthSocialSignInLightPreview() = AuthSocialSignInPreview(false)
@Preview(widthDp = 280) @Composable
private fun AuthSocialSignInDarkPreview() = AuthSocialSignInPreview(true)
