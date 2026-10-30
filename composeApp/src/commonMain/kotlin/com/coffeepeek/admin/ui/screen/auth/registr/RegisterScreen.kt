package com.coffeepeek.admin.ui.screen.auth.registr

import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
import coffeepeek.composeapp.generated.resources.auth_email_placeholder
import coffeepeek.composeapp.generated.resources.auth_edit_email
import coffeepeek.composeapp.generated.resources.auth_analytics_consent
import coffeepeek.composeapp.generated.resources.auth_analytics_description
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import coffeepeek.composeapp.generated.resources.already_have_account
import coffeepeek.composeapp.generated.resources.continue_action
import coffeepeek.composeapp.generated.resources.create_account
import coffeepeek.composeapp.generated.resources.create_account_title
import coffeepeek.composeapp.generated.resources.email_check_subtitle
import coffeepeek.composeapp.generated.resources.email_step_title
import coffeepeek.composeapp.generated.resources.registration_password_placeholder
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as IconButton
import androidx.compose.ui.text.style.TextOverflow
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
import com.coffeepeek.admin.auth.AppleAuth
import com.coffeepeek.admin.auth.handleGoogleSignInResult
import com.coffeepeek.admin.auth.isGoogleSignInConfigured
import com.coffeepeek.admin.legal.LegalUrls
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.ui.screen.auth.AuthFooterRow
import com.coffeepeek.admin.ui.screen.auth.AuthHeading
import com.coffeepeek.admin.ui.screen.auth.AuthMascot
import com.coffeepeek.admin.ui.screen.auth.AuthOrDivider
import com.coffeepeek.admin.ui.screen.auth.AuthPrimaryButton
import com.coffeepeek.admin.ui.screen.auth.AuthScreenScaffold
import com.coffeepeek.admin.ui.screen.auth.AuthSocialSignInButtons
import com.coffeepeek.admin.ui.screen.auth.AuthStepper
import com.coffeepeek.admin.ui.screen.auth.AuthTextField
import com.coffeepeek.admin.utils.MIN_REGISTRATION_PASSWORD_LENGTH
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.admin.di.platformViewModel

private data class RegistrationFormState(
    val step: RegisterStep = RegisterStep.Email,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isTermsAccepted: Boolean = false,
    val isAnalyticsAccepted: Boolean = false,
    val passwordError: String? = null,
    val emailError: String? = null,
)

object RegisterScreen {
    @Composable
    operator fun invoke(vm: RegisterViewModel = platformViewModel()) {
        val step by vm.step.collectAsState()
        val name by vm.name.collectAsState()
        val email by vm.email.collectAsState()
        val password by vm.password.collectAsState()
        val isTermsAccepted by vm.isTermsAccepted.collectAsState()
        val isAnalyticsAccepted by vm.isAnalyticsAccepted.collectAsState()
        val passwordError by vm.passwordError.collectAsState()
        val emailError by vm.emailError.collectAsState()
        val appleSigningIn by vm.appleSigningIn.collectAsState()
        RegisterScreenContent(
            state = RegistrationFormState(step, name, email, password, isTermsAccepted, isAnalyticsAccepted, passwordError, emailError),
            onEmailChange = vm::onEmailChange,
            onNameChange = vm::onNameChange,
            onPasswordChange = vm::onPasswordChange,
            onTermsChange = vm::onTermsCheckedChange,
            onAnalyticsChange = vm::onAnalyticsCheckedChange,
            onContinue = vm::onContinueEmail,
            onRegister = vm::onRegisterClick,
            onBack = vm::onBack,
            onLogin = vm::goToLogin,
            onClose = Navigator::closeAuth,
            socialSignIn = if (AppleAuth.isSupported() || isGoogleSignInConfigured()) {
                {
                    AuthSocialSignInButtons(
                        onAppleClick = vm::onAppleLogin,
                        appleEnabled = !appleSigningIn,
                        onGoogleResult = { result -> handleGoogleSignInResult(result, vm::onGoogleLogin) },
                    )
                }
            } else null,
        )
    }
}

@Composable
private fun RegisterScreenContent(
    state: RegistrationFormState,
    onEmailChange: (String) -> Unit = {},
    onNameChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTermsChange: (Boolean) -> Unit = {},
    onAnalyticsChange: (Boolean) -> Unit = {},
    onContinue: () -> Unit = {},
    onRegister: () -> Unit = {},
    onBack: () -> Unit = {},
    onLogin: () -> Unit = {},
    onClose: () -> Unit = {},
    socialSignIn: (@Composable () -> Unit)? = null,
) {
    AuthScreenScaffold(
        mascot = if (state.step == RegisterStep.Success) AuthMascot.Happy else AuthMascot.Laptop,
        onClose = onClose,
    ) {
        if (state.step != RegisterStep.Success) {
            AuthStepper(
                activeIndex = if (state.step == RegisterStep.Email) 0 else 1,
                steps = listOf(stringResource(Res.string.step_email), stringResource(Res.string.step_register), stringResource(Res.string.step_done)),
            )
        }
        when (state.step) {
            RegisterStep.Email -> RegistrationEmailContent(state, onEmailChange, onContinue, onLogin, socialSignIn)
            RegisterStep.Details -> RegistrationDetailsContent(
                state, onNameChange, onPasswordChange, onTermsChange, onAnalyticsChange, onRegister, onBack, onLogin,
            )
            RegisterStep.Success -> {
                AuthHeading(stringResource(Res.string.register_success_title), stringResource(Res.string.register_success_subtitle, state.email))
                AuthPrimaryButton(stringResource(Res.string.go_to_login), onLogin)
            }
        }
    }
}

@Composable
private fun RegistrationEmailContent(
    state: RegistrationFormState,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit,
    onLogin: () -> Unit,
    socialSignIn: (@Composable () -> Unit)?,
) {
    AuthHeading(stringResource(Res.string.email_step_title), stringResource(Res.string.email_check_subtitle))
    AuthTextField(
        value = state.email,
        onValueChange = onEmailChange,
        label = stringResource(Res.string.step_email),
        placeholder = stringResource(Res.string.auth_email_placeholder),
        leadingIcon = CpIcons.Email,
        errorText = state.emailError,
    )
    if (socialSignIn != null) {
        AuthOrDivider()
        socialSignIn()
    }
    AuthPrimaryButton(stringResource(Res.string.continue_action), onContinue, enabled = state.email.isNotBlank())
    AuthFooterRow(null, onLogin, stringResource(Res.string.already_have_account))
}

@Composable
private fun RegistrationDetailsContent(
    state: RegistrationFormState,
    onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onAnalyticsChange: (Boolean) -> Unit,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    NewProfileBadge()
    AuthHeading(stringResource(Res.string.create_account_title))
    EmailChip(state.email, onBack)
    RegistrationFields(state, onNameChange, onPasswordChange)
    RegistrationTermsConsent(state.isTermsAccepted, onTermsChange)
    RegistrationAnalyticsConsent(state.isAnalyticsAccepted, onAnalyticsChange)
    AuthPrimaryButton(
        text = stringResource(Res.string.create_account),
        onClick = onRegister,
        enabled = state.name.trim().length >= 2 &&
            state.password.length >= MIN_REGISTRATION_PASSWORD_LENGTH && state.isTermsAccepted,
    )
    AuthFooterRow(null, onLogin, stringResource(Res.string.already_have_account))
}

@Composable
private fun RegistrationFields(
    state: RegistrationFormState,
    onNameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
        AuthTextField(
            label = stringResource(Res.string.name),
            value = state.name,
            onValueChange = onNameChange,
            placeholder = stringResource(Res.string.name_hint),
            leadingIcon = CpIcons.User,
            keyboardType = KeyboardType.Text,
            next = true,
        )
        Column {
            AuthTextField(
                label = stringResource(Res.string.password),
                value = state.password,
                onValueChange = onPasswordChange,
                placeholder = stringResource(Res.string.registration_password_placeholder),
                leadingIcon = CpIcons.Lock,
                isPassword = true,
                newPassword = true,
                errorText = state.passwordError,
            )
            PasswordStrengthBar(state.password)
        }
    }
}

@Composable
private fun RegistrationTermsConsent(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = CpColor.Primary, checkmarkColor = CpColor.DarkTextOnPrimary),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(CpDimens.spacing3))
        TermsText()
    }
}

@Composable
private fun RegistrationAnalyticsConsent(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = checked, role = Role.Checkbox, onValueChange = onCheckedChange,
        ),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked, onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = CpColor.Primary, checkmarkColor = CpColor.DarkTextOnPrimary),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(CpDimens.spacing3))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.auth_analytics_consent), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface)
            Text(stringResource(Res.string.auth_analytics_description),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            .background(
                if (isDark) ColorWhite3 else CpColor.LightSurfaceAlt,
                RoundedCornerShape(12.dp),
            )
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 4.dp),
        )
        EditEmailButton(onChange)
    }
}

@Composable
private fun EditEmailButton(onChange: () -> Unit) {
    IconButton(onClick = onChange, modifier = Modifier.size(44.dp)) {
        Icon(CpIcons.Edit, contentDescription = stringResource(Res.string.auth_edit_email),
            tint = CpColor.Primary, modifier = Modifier.size(20.dp))
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

@Composable
private fun RegisterScreenPreview(dark: Boolean, step: RegisterStep) = CoffeePeekTheme(darkTheme = dark) {
    RegisterScreenContent(RegistrationFormState(step = step, email = stringResource(Res.string.auth_email_placeholder)))
}

@Preview(widthDp = 360, heightDp = 900) @Composable
private fun RegisterEmailLightPreview() = RegisterScreenPreview(false, RegisterStep.Email)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun RegisterEmailDarkPreview() = RegisterScreenPreview(true, RegisterStep.Email)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun RegisterDetailsLightPreview() = RegisterScreenPreview(false, RegisterStep.Details)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun RegisterDetailsDarkPreview() = RegisterScreenPreview(true, RegisterStep.Details)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun RegisterSuccessLightPreview() = RegisterScreenPreview(false, RegisterStep.Success)
@Preview(widthDp = 360, heightDp = 900) @Composable
private fun RegisterSuccessDarkPreview() = RegisterScreenPreview(true, RegisterStep.Success)
