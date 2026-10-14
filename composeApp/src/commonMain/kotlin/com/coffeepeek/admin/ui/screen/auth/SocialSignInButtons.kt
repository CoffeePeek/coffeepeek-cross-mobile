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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.ic_google_g
import coffeepeek.composeapp.generated.resources.sign_in_apple
import coffeepeek.composeapp.generated.resources.sign_in_google
import com.adamglin.PhosphorIcons
import com.adamglin.phosphoricons.Fill
import com.adamglin.phosphoricons.fill.AppleLogo
import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.admin.auth.AppleSignInButton
import com.coffeepeek.admin.auth.GoogleSignInButton
import com.coffeepeek.admin.auth.isGoogleSignInConfigured
import com.coffeepeek.admin.theme.CpDimens
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
                imageVector = PhosphorIcons.Fill.AppleLogo,
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

    // Use the card's side insets so both full labels fit on typical phone screens.
    val expandBy = CpDimens.authCardPadding - 16.dp
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().layout { measurable, constraints ->
            val inset = expandBy.roundToPx()
            val width = constraints.maxWidth + inset * 2
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(constraints.maxWidth, placeable.height) {
                placeable.placeRelative(-inset, 0)
            }
        },
    ) {
        if (showApple && showGoogle && maxWidth >= minButtonWidth * 2 + SocialButtonSpacing) {
            Row(horizontalArrangement = Arrangement.spacedBy(SocialButtonSpacing)) {
                AppleSignInButton(
                    onClick = onAppleClick,
                    enabled = appleEnabled,
                    modifier = Modifier.weight(1f),
                )
                GoogleSignInButton(onResult = onGoogleResult, modifier = Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(SocialButtonSpacing)) {
                if (showApple) AppleSignInButton(onClick = onAppleClick, enabled = appleEnabled)
                if (showGoogle) GoogleSignInButton(onResult = onGoogleResult)
            }
        }
    }
}
