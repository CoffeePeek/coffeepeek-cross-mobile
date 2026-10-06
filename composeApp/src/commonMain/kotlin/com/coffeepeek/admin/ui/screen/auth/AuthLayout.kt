package com.coffeepeek.admin.ui.screen.auth

import com.coffeepeek.admin.ui.component.GlassControlIcon
import com.coffeepeek.admin.ui.component.PlatformGlassIconButton
import com.coffeepeek.admin.ui.component.PlatformGlassBackButton
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.maskot_happy
import coffeepeek.composeapp.generated.resources.maskot_with_laptop
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class AuthMascot {
    Laptop,
    Happy,
}

@Composable
fun AuthScreenScaffold(
    mascot: AuthMascot = AuthMascot.Laptop,
    showMascot: Boolean = true,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bg = if (isDark) CpColor.DarkBackground else CpColor.AuthLightBackground
    val cardBg = if (isDark) CpColor.AuthCardDark else CpColor.LightSurface
    val cardBorder = if (isDark) CpColor.DarkBorder else CpColor.LightBorder

    // How much of the mascot sits above the card edge (rest overlaps the card).
    val mascotOverlap = 40.dp
    val mascotTopGap = 12.dp
    val cardTopInset = if (showMascot) {
        mascotTopGap + CpDimens.authMascotSize - mascotOverlap
    } else {
        0.dp
    }

    val glassHaze = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg),
    ) {
        // Blur source for the glass close button.
        Box(Modifier.fillMaxSize().hazeSource(glassHaze)) {
        AuthAmbientBackground(isDark = isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CpDimens.spacing4, vertical = CpDimens.spacing5),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(CpDimens.spacing4))

            AuthWordmark()

            Box(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = cardTopInset)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CpDimens.authCardRadius))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(CpDimens.authCardRadius))
                        .padding(
                            start = CpDimens.authCardPadding,
                            end = CpDimens.authCardPadding,
                            top = if (showMascot) 48.dp else CpDimens.authCardPadding,
                            bottom = CpDimens.authCardPadding,
                        ),
                    content = content,
                )

                if (showMascot) {
                    val mascotRes: DrawableResource = when (mascot) {
                        AuthMascot.Laptop -> Res.drawable.maskot_with_laptop
                        AuthMascot.Happy -> Res.drawable.maskot_happy
                    }
                    Image(
                        painter = painterResource(mascotRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = mascotTopGap)
                            .size(CpDimens.authMascotSize)
                            .zIndex(1f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(CpDimens.spacing8))
        }
        }

        // Above the scroll column so clicks are not swallowed.
        AuthCloseButton(
            onClick = onClose,
            hazeState = glassHaze,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(top = 20.dp, start = 20.dp)
                .zIndex(2f),
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
            text = "  ИЛИ  ",
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
fun AuthFooterRow(
    onBack: (() -> Unit)?,
    onSecondary: () -> Unit,
    secondaryText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            PlatformGlassBackButton(onClick = onBack)
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }

        Text(
            text = secondaryText,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = CpColor.Primary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSecondary)
                .padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun AuthStepper(
    activeIndex: Int,
    steps: List<String>,
    modifier: Modifier = Modifier,
) {
    // Card uses 40dp horizontal padding; bleed into it so labels fit.
    // Do not use negative Modifier.padding — Compose requires non-negative values.
    val expandBy = CpDimens.authCardPadding - 16.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val expandPx = expandBy.roundToPx()
                val widened = (constraints.maxWidth + expandPx * 2).coerceAtLeast(0)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = widened, maxWidth = widened),
                )
                layout(constraints.maxWidth, placeable.height) {
                    placeable.placeRelative(-expandPx, 0)
                }
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, label ->
            val active = index <= activeIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (active) CpColor.Primary.copy(alpha = 0.12f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (active) CpColor.Primary else Color.Transparent)
                        .then(
                            if (!active) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            else Modifier,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (index < activeIndex) {
                        Icon(
                            imageVector = CpIcons.Check,
                            contentDescription = null,
                            tint = CpColor.DarkTextOnPrimary,
                            modifier = Modifier.size(10.dp),
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = if (active) CpColor.DarkTextOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (active) CpColor.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                )
            }
            if (index < steps.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .weight(1f, fill = true)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline),
                )
            }
        }
    }
}
