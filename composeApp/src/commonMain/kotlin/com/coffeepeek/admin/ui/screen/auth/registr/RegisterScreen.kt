package com.coffeepeek.admin.ui.screen.auth.registr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coffeepeek.composeapp.generated.resources.Res
import coffeepeek.composeapp.generated.resources.already_have_account
import coffeepeek.composeapp.generated.resources.continue_action
import coffeepeek.composeapp.generated.resources.create_account
import coffeepeek.composeapp.generated.resources.create_account_title
import coffeepeek.composeapp.generated.resources.email_check_subtitle
import coffeepeek.composeapp.generated.resources.email_step_title
import coffeepeek.composeapp.generated.resources.error_enter_password_length
import coffeepeek.composeapp.generated.resources.go_to_login
import coffeepeek.composeapp.generated.resources.name
import coffeepeek.composeapp.generated.resources.name_hint
import coffeepeek.composeapp.generated.resources.new_profile
import coffeepeek.composeapp.generated.resources.password
import coffeepeek.composeapp.generated.resources.privacy_link
import coffeepeek.composeapp.generated.resources.register_success_subtitle
import coffeepeek.composeapp.generated.resources.register_success_title
import coffeepeek.composeapp.generated.resources.step_done
import coffeepeek.composeapp.generated.resources.step_email
import coffeepeek.composeapp.generated.resources.step_register
import coffeepeek.composeapp.generated.resources.terms_accept_prefix
import coffeepeek.composeapp.generated.resources.terms_and
import coffeepeek.composeapp.generated.resources.terms_link
import com.coffeepeek.admin.auth.GoogleSignInButton
import com.coffeepeek.admin.auth.handleGoogleSignInResult
import com.coffeepeek.admin.auth.isGoogleSignInConfigured
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.auth.AuthFooterRow
import com.coffeepeek.admin.ui.screen.auth.AuthMascot
import com.coffeepeek.admin.ui.screen.auth.AuthOrDivider
import com.coffeepeek.admin.ui.screen.auth.AuthPrimaryButton
import com.coffeepeek.admin.ui.screen.auth.AuthScreenScaffold
import com.coffeepeek.admin.ui.screen.auth.AuthStepper
import com.coffeepeek.admin.ui.screen.auth.AuthTextField
import com.coffeepeek.admin.utils.MIN_REGISTRATION_PASSWORD_LENGTH
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.admin.di.platformViewModel

object RegisterScreen {

    @Composable
    operator fun invoke(
        vm: RegisterViewModel = platformViewModel(),
    ) {
        val step by vm.step.collectAsState()
        val name by vm.name.collectAsState()
        val email by vm.email.collectAsState()
        val password by vm.password.collectAsState()
        val isTermsAccepted by vm.isTermsAccepted.collectAsState()
        val passwordError by vm.passwordError.collectAsState()
        val emailError by vm.emailError.collectAsState()

        val steps = listOf(
            stringResource(Res.string.step_email),
            stringResource(Res.string.step_register),
            stringResource(Res.string.step_done),
        )
        val activeIndex = when (step) {
            RegisterStep.Email -> 0
            RegisterStep.Details -> 1
            RegisterStep.Success -> 2
        }

        AuthScreenScaffold(
            mascot = if (step == RegisterStep.Success) AuthMascot.Happy else AuthMascot.Laptop,
            showMascot = true,
            onClose = Navigator::closeAuth,
        ) {
            when (step) {
                RegisterStep.Success -> {
                    Text(
                        text = stringResource(Res.string.register_success_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.52).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(Res.string.register_success_subtitle, email),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 28.dp),
                    )
                    AuthPrimaryButton(
                        text = stringResource(Res.string.go_to_login),
                        onClick = { vm.goToLogin() },
                    )
                }

                RegisterStep.Email -> {
                    AuthStepper(activeIndex = activeIndex, steps = steps)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(Res.string.email_step_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.52).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = stringResource(Res.string.email_check_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 24.dp),
                    )

                    AuthTextField(
                        value = email,
                        onValueChange = vm::onEmailChange,
                        placeholder = "name@example.com",
                        leadingIcon = CpIcons.Email,
                        errorText = emailError,
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    AuthOrDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    if (isGoogleSignInConfigured()) {
                        GoogleSignInButton(
                            onResult = { result ->
                                handleGoogleSignInResult(result, vm::onGoogleLogin)
                            },
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    AuthPrimaryButton(
                        text = stringResource(Res.string.continue_action),
                        onClick = { vm.onContinueEmail() },
                        enabled = email.isNotBlank(),
                    )

                    AuthFooterRow(
                        onBack = null,
                        onSecondary = { vm.goToLogin() },
                        secondaryText = stringResource(Res.string.already_have_account),
                    )
                }

                RegisterStep.Details -> {
                    AuthStepper(activeIndex = activeIndex, steps = steps)
                    Spacer(modifier = Modifier.height(16.dp))

                    NewProfileBadge()

                    Text(
                        text = stringResource(Res.string.create_account_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.52).sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                    )

                    EmailChip(
                        email = email,
                        onChange = { vm.onBack() },
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AuthTextField(
                        label = stringResource(Res.string.name),
                        value = name,
                        onValueChange = vm::onNameChange,
                        placeholder = stringResource(Res.string.name_hint),
                        leadingIcon = CpIcons.User,
                        keyboardType = KeyboardType.Text,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    AuthTextField(
                        label = stringResource(Res.string.password),
                        value = password,
                        onValueChange = vm::onPasswordChange,
                        placeholder = stringResource(Res.string.error_enter_password_length),
                        leadingIcon = CpIcons.Lock,
                        isPassword = true,
                        errorText = passwordError,
                    )

                    PasswordStrengthBar(password = password)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = isTermsAccepted,
                            onCheckedChange = vm::onTermsCheckedChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = CpColor.Primary,
                                checkmarkColor = CpColor.DarkTextOnPrimary,
                            ),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(CpDimens.spacing3))
                        TermsText()
                    }

                    Spacer(modifier = Modifier.height(CpDimens.spacing5))

                    AuthPrimaryButton(
                        text = stringResource(Res.string.create_account),
                        onClick = { vm.onRegisterClick() },
                        enabled = name.trim().length >= 2 &&
                            password.length >= MIN_REGISTRATION_PASSWORD_LENGTH &&
                            isTermsAccepted,
                    )

                    AuthFooterRow(
                        onBack = null,
                        onSecondary = { vm.goToLogin() },
                        secondaryText = stringResource(Res.string.already_have_account),
                    )
                }
            }
        }
    }
}

@Composable
private fun NewProfileBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(CpColor.GoldWarm.copy(alpha = 0.18f), RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "✦  ${stringResource(Res.string.new_profile)}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            ),
            color = CpColor.Primary,
        )
    }
}

@Composable
private fun EmailChip(
    email: String,
    onChange: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
            .background(
                if (isDark) ColorWhite3 else CpColor.LightSurfaceAlt,
                RoundedCornerShape(12.dp),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector = CpIcons.Email,
            contentDescription = null,
            tint = CpColor.GoldWarm,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = "Изменить",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = CpColor.Primary,
            modifier = Modifier.clickable(onClick = onChange),
        )
    }
}

private val ColorWhite3 = Color(0x08FFFFFF)

@Composable
private fun PasswordStrengthBar(password: String) {
    if (password.isEmpty()) return
    val score = (if (password.length >= 8) 1 else 0) +
        (if (password.any { it.isDigit() }) 1 else 0) +
        (if (password.any { it.isUpperCase() }) 1 else 0)
    val colors = listOf(CpColor.Error, CpColor.Primary, CpColor.Success)
    val labels = listOf("слабый", "средний", "надёжный")
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(
                        if (i < score) colors[(score - 1).coerceIn(0, 2)]
                        else if (isDark) CpColor.DarkBorder else CpColor.LightBorder,
                        RoundedCornerShape(99.dp),
                    ),
            )
        }
        if (score > 0) {
            Text(
                text = labels[score - 1],
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun TermsText() {
    val primary = CpColor.Primary
    val linkStyles = TextLinkStyles(style = SpanStyle(color = primary, fontWeight = FontWeight.SemiBold))

    val annotatedString = buildAnnotatedString {
        append(stringResource(Res.string.terms_accept_prefix))
        withLink(
            LinkAnnotation.Url(url = LegalUrls.TERMS, styles = linkStyles),
        ) { append(stringResource(Res.string.terms_link)) }
        append(stringResource(Res.string.terms_and))
        withLink(
            LinkAnnotation.Url(url = LegalUrls.PRIVACY, styles = linkStyles),
        ) { append(stringResource(Res.string.privacy_link)) }
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
