package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.animation.AnimatedVisibility
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_mascot_laptop
import com.coffeepeek.feature.auth.impl.resources.auth_close
import com.coffeepeek.feature.auth.impl.resources.auth_hide_password
import com.coffeepeek.feature.auth.impl.resources.auth_show_password
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.coffeepeek.core.designsystem.component.CoffeePeekCompactOutlinedTextField
import com.coffeepeek.core.designsystem.component.CoffeePeekInputRole
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.CoffeePeekGlassIconButton
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val AuthLightBackground = Color(0xFFFFFCF7)
private val AuthCardDark = Color(0x992D241F)
private val AuthInputDark = Color(0x08FFFFFF)
private val CardRadius = 24.dp
internal val AuthCardPadding = 40.dp
private val MascotSize = 128.dp

/**
 * The shared frame of the sign-in and recovery screens: ambient background, wordmark, the card with an optional mascot, and the
 * glass close button. Without the mascot the card starts a gap below the header so it never touches the logo or the close button.
 */
@Composable
internal fun AuthScaffold(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    mascot: Boolean = true,
    mascotRes: DrawableResource = Res.drawable.auth_mascot_laptop,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val background = if (isDark) CoffeePeekColors.DarkBackground else AuthLightBackground
    val cardBackground = if (isDark) AuthCardDark else CoffeePeekColors.LightSurface
    val cardBorder = if (isDark) CoffeePeekColors.DarkBorder else CoffeePeekColors.LightBorder
    val mascotOverlap = 40.dp
    val mascotTopGap = 12.dp
    val cardTopInset = if (mascot) mascotTopGap + MascotSize - mascotOverlap else CoffeePeekDimens.spacing6
    val haze = rememberHazeState()

    Box(modifier.fillMaxSize().background(background)) {
        Box(Modifier.fillMaxSize().hazeSource(haze)) {
            AmbientBackground(isDark)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing5)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                Wordmark()
                Box(Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(top = cardTopInset)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CardRadius))
                            .background(cardBackground)
                            .border(1.dp, cardBorder, RoundedCornerShape(CardRadius))
                            .padding(start = AuthCardPadding, end = AuthCardPadding, top = if (mascot) 48.dp else AuthCardPadding, bottom = AuthCardPadding),
                        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
                        content = content,
                    )
                    if (mascot) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(mascotRes),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = mascotTopGap).size(MascotSize).zIndex(1f),
                        )
                    }
                }
                Spacer(Modifier.height(CoffeePeekDimens.spacing8))
            }
        }
        CloseButton(onClose, haze, Modifier.align(Alignment.TopStart).statusBarsPadding().padding(top = 20.dp, start = 20.dp).zIndex(2f))
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, haze: HazeState, modifier: Modifier) {
    CoffeePeekGlassIconButton(onClick = onClick, contentDescription = stringResource(Res.string.auth_close), hazeState = haze, modifier = modifier) {
        Icon(CoffeePeekIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun AmbientBackground(isDark: Boolean) {
    Box(Modifier.fillMaxSize()) {
        if (isDark) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(colors = listOf(CoffeePeekColors.DarkSurface.copy(alpha = 0.35f), Color.Transparent)),
                ),
            )
        }
        Box(
            Modifier.size(480.dp).offset(x = (-120).dp, y = (-120).dp).background(
                Brush.radialGradient(colors = listOf(if (isDark) Color(0x29EAB308) else Color(0x14EAB308), Color.Transparent)),
                shape = CircleShape,
            ),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).size(520.dp).offset(x = 160.dp, y = 160.dp).background(
                Brush.radialGradient(colors = listOf(if (isDark) Color(0x1AB48C4B) else Color(0x0FB48C4B), Color.Transparent)),
                shape = CircleShape,
            ),
        )
    }
}

@Composable
private fun Wordmark() {
    val onBackground = MaterialTheme.colorScheme.onBackground
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = onBackground, fontWeight = FontWeight.ExtraBold)) { append("Coffee") }
            withStyle(SpanStyle(color = CoffeePeekColors.Primary, fontWeight = FontWeight.ExtraBold)) { append("Peek") }
        },
        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp, letterSpacing = (-0.77).sp, fontWeight = FontWeight.ExtraBold),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    errorText: String? = null,
    next: Boolean = false,
    enabled: Boolean = true,
    /** Plain text (a name) instead of the e-mail keyboard. */
    text: Boolean = false,
    /** The password being created, so the platform suggests a strong one instead of filling a saved one. */
    newPassword: Boolean = false,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val isError = errorText != null
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gold = if (isDark) CoffeePeekColors.Primary else CoffeePeekColors.GoldWarm
    val fieldColor = if (isDark) AuthInputDark else Color.White
    val border = if (isDark) CoffeePeekColors.DarkBorder else Color(0x66A07B36)
    val fieldHeight = CoffeePeekDimens.controlHeight

    Column(modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        CoffeePeekCompactOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(fieldHeight),
            enabled = enabled,
            shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
            isError = isError,
            errorDescription = errorText,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 18.sp),
            contentPadding = PaddingValues(horizontal = CoffeePeekDimens.inputPadding, vertical = 0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = fieldColor,
                unfocusedContainerColor = fieldColor,
                errorContainerColor = fieldColor,
                focusedBorderColor = border,
                unfocusedBorderColor = border,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = gold,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            leadingIcon = leadingIcon?.let { icon ->
                { Icon(icon, contentDescription = null, tint = if (isError) MaterialTheme.colorScheme.error else gold) }
            },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (passwordVisible) CoffeePeekIcons.VisibilityOff else CoffeePeekIcons.Visibility,
                            contentDescription = stringResource(if (passwordVisible) Res.string.auth_hide_password else Res.string.auth_show_password),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else null,
            placeholder = {
                Text(
                    text = placeholder,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 18.sp),
                    color = Color(0xFF9CA3AF),
                )
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else if (text) KeyboardType.Text else KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = if (next) ImeAction.Next else ImeAction.Done,
            ),
            inputRole = if (newPassword) CoffeePeekInputRole.NewPassword else CoffeePeekInputRole.Credential,
        )
        AnimatedVisibility(visible = isError) {
            Text(
                text = errorText.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** The gold pill button; [loading] swaps the label for a spinner and blocks taps. */
@Composable
internal fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val gold = CoffeePeekColors.Primary
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.fillMaxWidth().height(CoffeePeekDimens.controlHeight),
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = gold,
            contentColor = CoffeePeekColors.DarkTextOnPrimary,
            disabledContainerColor = gold.copy(alpha = if (loading) 1f else 0.5f),
            disabledContentColor = CoffeePeekColors.DarkTextOnPrimary.copy(alpha = if (loading) 1f else 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp, disabledElevation = 0.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = CoffeePeekColors.DarkTextOnPrimary, strokeWidth = 2.dp)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

/** The secondary action under the primary button: a text link at the end of a row at least 48 dp high. */
@Composable
internal fun AuthFooterRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(1.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = CoffeePeekColors.Primary,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        )
    }
}

/** The title of a step and, under it, its description: 8 dp apart; the card spaces everything else by 16 dp. */
@Composable
internal fun AuthHeading(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.52).sp, lineHeight = 28.6.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The registration progress: three equal columns inside the card's content width, the labels wrap instead of being cut. */
@Composable
internal fun AuthStepper(activeIndex: Int, steps: List<String>, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, label ->
            Column(
                modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { selected = index == activeIndex },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = if (index == 0) Color.Transparent else MaterialTheme.colorScheme.outline)
                    StepMarker(index, activeIndex)
                    HorizontalDivider(Modifier.weight(1f), color = if (index == steps.lastIndex) Color.Transparent else MaterialTheme.colorScheme.outline)
                }
                Text(
                    text = label,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = if (index <= activeIndex) CoffeePeekColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StepMarker(index: Int, activeIndex: Int) {
    val active = index <= activeIndex
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape)
            .background(if (active) CoffeePeekColors.Primary else Color.Transparent)
            .border(1.dp, if (active) CoffeePeekColors.Primary else MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (index < activeIndex) {
            Icon(CoffeePeekIcons.Check, contentDescription = null, tint = CoffeePeekColors.DarkTextOnPrimary, modifier = Modifier.size(14.dp))
        } else {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (active) CoffeePeekColors.DarkTextOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
