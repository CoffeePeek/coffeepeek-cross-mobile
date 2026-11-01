package com.coffeepeek.admin.ui.screen.auth

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
import com.coffeepeek.admin.ui.component.platformAuthKeyboardOptions
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leadingIcon: ImageVector? = null,
    isPassword: Boolean = false,
    errorText: String? = null,
    newPassword: Boolean = false,
    next: Boolean = false,
    keyboardType: KeyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
) {
    val localizedLegacyAppTextFieldAuthHidePassword = stringResource(Res.string.legacy_app_text_field_auth_hide_password)
    val localizedLegacyAppTextFieldAuthShowPassword = stringResource(Res.string.legacy_app_text_field_auth_show_password)

    var isPasswordVisible by remember { mutableStateOf(false) }
    val isError = errorText != null
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gold = if (isDark) CpColor.Primary else CpColor.GoldWarm

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }

        val nativeInput = com.coffeepeek.admin.ui.component.NativeAuthInput(
            value, onValueChange, label ?: placeholder, placeholder, isPassword, newPassword, next, isError, keyboardType,
            Modifier.fillMaxWidth().heightIn(min = CpDimens.authFieldHeight),
        )
        if (!nativeInput) CompactOutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(CpDimens.authFieldHeight),
            shape = RoundedCornerShape(CpDimens.authFieldRadius),
            isError = isError,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 18.sp,
            ),
            contentPadding = CpDimens.singleLineFieldContentPadding,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) CpColor.AuthInputDark else Color.White,
                unfocusedContainerColor = if (isDark) CpColor.AuthInputDark else Color.White,
                errorContainerColor = if (isDark) CpColor.AuthInputDark else Color.White,
                focusedBorderColor = if (isDark) CpColor.DarkBorder else Color(0x66A07B36),
                unfocusedBorderColor = if (isDark) CpColor.DarkBorder else Color(0x66A07B36),
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = gold,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            leadingIcon = if (leadingIcon != null) {
                {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error else gold,
                    )
                }
            } else null,
            trailingIcon = if (isPassword) {
                {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) CpIcons.VisibilityOff else CpIcons.Visibility,
                            contentDescription = if (isPasswordVisible) localizedLegacyAppTextFieldAuthHidePassword else localizedLegacyAppTextFieldAuthShowPassword,
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
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                    ),
                    color = Color(0xFF9CA3AF),
                )
            },
            visualTransformation = if (isPassword && !isPasswordVisible) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = platformAuthKeyboardOptions(
                options = KeyboardOptions(
                    keyboardType = keyboardType,
                    autoCorrectEnabled = false,
                    imeAction = if (next) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Done,
                ),
                newPassword = newPassword,
            ),
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

@Composable
fun AuthPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val gold = CpColor.Primary
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(CpDimens.authPrimaryBtnHeight),
        shape = RoundedCornerShape(CpDimens.authFieldRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = gold,
            contentColor = CpColor.DarkTextOnPrimary,
            disabledContainerColor = gold.copy(alpha = 0.5f),
            disabledContentColor = CpColor.DarkTextOnPrimary.copy(alpha = 0.7f),
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 2.dp,
            disabledElevation = 0.dp,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}
