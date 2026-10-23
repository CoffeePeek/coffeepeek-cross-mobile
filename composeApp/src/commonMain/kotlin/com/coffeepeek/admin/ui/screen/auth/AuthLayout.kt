package com.coffeepeek.admin.ui.screen.auth

import com.coffeepeek.admin.ui.component.PlatformScrollableColumn
import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.auth_or
import coffeepeek.composeapp.generated.resources.login
import coffeepeek.composeapp.generated.resources.login_subtitle
import coffeepeek.composeapp.generated.resources.login_title
import coffeepeek.composeapp.generated.resources.maskot_happy
import coffeepeek.composeapp.generated.resources.maskot_with_laptop
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

enum class AuthMascot {
    Laptop,
    Happy,
}

@Composable
fun AuthScreenScaffold(
    mascot: AuthMascot = AuthMascot.Laptop,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg = if (isDark) CpColor.DarkBackground else CpColor.AuthLightBackground
    val glassHaze = rememberHazeState()
    Box(Modifier.fillMaxSize().background(bg)) {
        Box(Modifier.fillMaxSize().hazeSource(glassHaze)) {
            AuthAmbientBackground(isDark)
            AuthScrollContent(mascot, content)
        }
        AuthCloseButton(
            onClick = onClose,
            hazeState = glassHaze,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding()
                .padding(top = 20.dp, start = 20.dp).zIndex(2f),
        )
    }
}

@Composable
private fun AuthScrollContent(mascot: AuthMascot, content: @Composable ColumnScope.() -> Unit) {
    PlatformScrollableColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(CpDimens.spacing4))
        AuthWordmark()
        AuthMascotCard(mascot, content)
        Spacer(Modifier.height(CpDimens.spacing8))
    }
}

@Composable
private fun AuthMascotCard(mascot: AuthMascot, content: @Composable ColumnScope.() -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val cardBg = if (isDark) CpColor.AuthCardDark else CpColor.LightSurface
    val cardBorder = if (isDark) CpColor.DarkBorder else CpColor.LightBorder
    val shape = RoundedCornerShape(CpDimens.authCardRadius)
    Box(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(top = 12.dp + CpDimens.authMascotSize - 40.dp)
                .fillMaxWidth().clip(shape).background(cardBg).border(1.dp, cardBorder, shape)
                .padding(start = CpDimens.authCardPadding, end = CpDimens.authCardPadding,
                    top = 48.dp, bottom = CpDimens.authCardPadding),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
            content = content,
        )
        Image(
            painter = painterResource(when (mascot) {
                AuthMascot.Laptop -> Res.drawable.maskot_with_laptop
                AuthMascot.Happy -> Res.drawable.maskot_happy
            }),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                .size(CpDimens.authMascotSize).zIndex(1f),
        )
    }
}

@Composable
private fun AuthCloseButton(
    onClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    PlatformGlassIconButton(
        icon = GlassControlIcon.Close,
        onClick = onClick,
        contentDescription = "Закрыть",
        hazeState = hazeState,
        modifier = modifier,
    ) {
        Icon(
            imageVector = CpIcons.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AuthAmbientBackground(isDark: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CpColor.DarkSurface.copy(alpha = 0.35f), Color.Transparent),
                        ),
                    ),
            )
        }
        Box(
            modifier = Modifier
                .size(480.dp)
                .offset(x = (-120).dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0x29EAB308) else Color(0x14EAB308),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(520.dp)
                .offset(x = 160.dp, y = 160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isDark) Color(0x1AB48C4B) else Color(0x0FB48C4B),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
    }
}

@Composable
fun AuthWordmark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.onBackground
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = primary, fontWeight = FontWeight.ExtraBold)) {
                append("Coffee")
            }
            withStyle(SpanStyle(color = CpColor.Primary, fontWeight = FontWeight.ExtraBold)) {
                append("Peek")
            }
        },
        style = MaterialTheme.typography.headlineSmall.copy(
            fontSize = 22.sp,
            letterSpacing = (-0.77).sp,
            fontWeight = FontWeight.ExtraBold,
        ),
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun AuthOrDivider(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = stringResource(Res.string.auth_or),
            modifier = Modifier.padding(horizontal = CpDimens.spacing2),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun AuthLayoutPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    AuthScreenScaffold(onClose = {}) {
        AuthHeading(stringResource(Res.string.login_title), stringResource(Res.string.login_subtitle))
        AuthOrDivider()
        AuthPrimaryButton(stringResource(Res.string.login), {})
    }
}

@Preview(widthDp = 360, heightDp = 900) @Composable
private fun AuthLayoutLightPreview() = AuthLayoutPreview(false)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun AuthLayoutDarkPreview() = AuthLayoutPreview(true)
