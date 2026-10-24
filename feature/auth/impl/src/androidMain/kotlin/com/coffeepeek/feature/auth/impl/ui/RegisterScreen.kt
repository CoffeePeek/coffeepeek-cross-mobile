package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.feature.auth.impl.register.RegisterEvent
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.auth.domain.PasswordStrength
import com.coffeepeek.feature.auth.domain.RegistrationIssue
import com.coffeepeek.feature.auth.domain.canContinueWithEmail
import com.coffeepeek.feature.auth.domain.canSubmitRegistration
import com.coffeepeek.feature.auth.domain.passwordStrength
import com.coffeepeek.feature.auth.impl.R
import com.coffeepeek.feature.auth.impl.register.RegisterAction
import com.coffeepeek.feature.auth.impl.register.RegisterState
import com.coffeepeek.feature.auth.impl.register.RegisterStep
import com.coffeepeek.feature.auth.impl.register.RegisterViewModel

private const val TermsUrl = "https://coffeepeek.by/terms"
private const val PrivacyUrl = "https://coffeepeek.by/privacy"

@Composable
internal fun RegisterScreen(
    vm: RegisterViewModel,
    onClose: () -> Unit,
    onGoToLogin: () -> Unit,
    onSignedIn: () -> Unit,
    googleWebClientId: String,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnSignedIn by rememberUpdatedState(onSignedIn)
    LaunchedEffect(vm, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.events.collect { event ->
                when (event) {
                    RegisterEvent.SignedIn -> currentOnSignedIn()
                }
            }
        }
    }
    RegisterContent(
        state, vm::onAction, onClose, onGoToLogin,
        onGoogle = if (googleWebClientId.isBlank()) null else rememberGoogleSignIn(
            googleWebClientId,
            onToken = { vm.onAction(RegisterAction.GoogleToken(it)) },
            onFailure = { vm.onAction(RegisterAction.GoogleFailed(it)) },
        ),
        modifier = modifier,
    )
}

@Composable
internal fun RegisterContent(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
    onClose: () -> Unit,
    onGoToLogin: () -> Unit,
    onGoogle: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val done = state.step == RegisterStep.Success
    AuthScaffold(
        onClose = onClose,
        modifier = modifier,
        mascotRes = if (done) R.drawable.auth_mascot_happy else R.drawable.auth_mascot_laptop,
    ) {
        when (state.step) {
            RegisterStep.Email -> EmailStep(state, onAction, onGoToLogin, onGoogle)
            RegisterStep.Details -> DetailsStep(state, onAction, onGoToLogin)
            RegisterStep.Success -> SuccessStep(state.email, onGoToLogin)
        }
    }
}

@Composable
private fun steps() = listOf(stringResource(R.string.auth_step_email), stringResource(R.string.auth_step_register), stringResource(R.string.auth_step_done))

@Composable
private fun EmailStep(state: RegisterState, onAction: (RegisterAction) -> Unit, onGoToLogin: () -> Unit, onGoogle: (() -> Unit)?) {
    AuthStepper(activeIndex = 0, steps = steps())
    AuthHeading(stringResource(R.string.auth_register_email_title), stringResource(R.string.auth_register_email_subtitle))
    AuthTextField(
        value = state.email,
        onValueChange = { onAction(RegisterAction.EmailChanged(it)) },
        label = stringResource(R.string.auth_email_label),
        placeholder = stringResource(R.string.auth_email_placeholder),
        leadingIcon = CoffeePeekIcons.Email,
        errorText = when {
            state.emailTaken -> stringResource(R.string.auth_email_taken)
            state.emailIssue != null -> emailError(state.emailIssue)
            else -> null
        },
        enabled = !state.busy,
    )
    FailureText(state, R.string.auth_check_failed)
    if (onGoogle != null) {
        AuthOrDivider()
        GoogleSignInButton(onClick = onGoogle, enabled = !state.busy)
        SocialFailureText(state.socialFailure)
    }
    AuthPrimaryButton(
        text = stringResource(R.string.auth_continue),
        onClick = { onAction(RegisterAction.ContinueWithEmail) },
        enabled = canContinueWithEmail(state.email),
        loading = state.busy,
    )
    AuthFooterRow(text = stringResource(R.string.auth_already_have_account), onClick = onGoToLogin)
}

@Composable
private fun DetailsStep(state: RegisterState, onAction: (RegisterAction) -> Unit, onGoToLogin: () -> Unit) {
    AuthStepper(activeIndex = 1, steps = steps())
    NewProfileBadge()
    AuthHeading(stringResource(R.string.auth_register_title))
    EmailChip(state.email, enabled = !state.busy, onChange = { onAction(RegisterAction.ChangeEmail) })
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4)) {
        AuthTextField(
            label = stringResource(R.string.auth_name_label),
            value = state.name,
            onValueChange = { onAction(RegisterAction.NameChanged(it)) },
            placeholder = stringResource(R.string.auth_name_placeholder),
            leadingIcon = CoffeePeekIcons.User,
            text = true,
            next = true,
            enabled = !state.busy,
        )
        Column {
            AuthTextField(
                label = stringResource(R.string.auth_password_label),
                value = state.password,
                onValueChange = { onAction(RegisterAction.PasswordChanged(it)) },
                placeholder = stringResource(R.string.auth_register_password_placeholder),
                leadingIcon = CoffeePeekIcons.Lock,
                isPassword = true,
                errorText = state.passwordIssue?.let { passwordError(it) },
                enabled = !state.busy,
            )
            PasswordStrengthBar(state.password)
        }
    }
    CheckRow(state.termsAccepted, { onAction(RegisterAction.TermsChanged(it)) }, enabled = !state.busy) { TermsText() }
    CheckRow(state.analyticsAccepted, { onAction(RegisterAction.AnalyticsChanged(it)) }, enabled = !state.busy) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.auth_analytics_title), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                stringResource(R.string.auth_analytics_description),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    state.reported?.let { issue ->
        Text(text = stringResource(reportedText(issue)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    FailureText(state, R.string.auth_register_failed)
    AuthPrimaryButton(
        text = stringResource(R.string.auth_register_submit),
        onClick = { onAction(RegisterAction.Submit) },
        enabled = canSubmitRegistration(state.name, state.password, state.termsAccepted),
        loading = state.busy,
    )
    AuthFooterRow(text = stringResource(R.string.auth_already_have_account), onClick = onGoToLogin)
}

@Composable
private fun SuccessStep(email: String, onGoToLogin: () -> Unit) {
    AuthHeading(stringResource(R.string.auth_register_success_title), stringResource(R.string.auth_register_success_subtitle, email))
    AuthPrimaryButton(text = stringResource(R.string.auth_go_to_login), onClick = onGoToLogin)
}

/** The request error: the server's own message as the legacy dialog showed it, or the generic text of the step. */
@Composable
private fun FailureText(state: RegisterState, generic: Int) {
    val failure = state.failure ?: return
    Text(
        text = failure.message ?: stringResource(generic),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

private fun reportedText(issue: RegistrationIssue): Int = when (issue) {
    RegistrationIssue.NameRequired -> R.string.auth_name_required
    RegistrationIssue.EmailRequired -> R.string.auth_email_required
    RegistrationIssue.EmailInvalid -> R.string.auth_email_invalid
    RegistrationIssue.PasswordTooShort -> R.string.auth_password_too_short_registration
    RegistrationIssue.TermsRequired -> R.string.auth_terms_required
}

@Composable
private fun CheckRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = CoffeePeekColors.Primary, checkmarkColor = CoffeePeekColors.DarkTextOnPrimary),
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(CoffeePeekDimens.spacing3))
        content()
    }
}

@Composable
private fun TermsText() {
    val linkStyles = TextLinkStyles(style = SpanStyle(color = CoffeePeekColors.Primary, fontWeight = FontWeight.SemiBold))
    val text = buildAnnotatedString {
        append(stringResource(R.string.auth_terms_prefix))
        withLink(LinkAnnotation.Url(TermsUrl, linkStyles)) { append(stringResource(R.string.auth_terms_link)) }
        append(stringResource(R.string.auth_terms_and))
        withLink(LinkAnnotation.Url(PrivacyUrl, linkStyles)) { append(stringResource(R.string.auth_privacy_link)) }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NewProfileBadge() {
    Text(
        text = "✦  ${stringResource(R.string.auth_new_profile)}",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
        color = CoffeePeekColors.Primary,
        modifier = Modifier
            .background(CoffeePeekColors.GoldWarm.copy(alpha = 0.18f), RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun EmailChip(email: String, enabled: Boolean = true, onChange: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDark) Color(0x08FFFFFF) else CoffeePeekColors.LightSurfaceAlt, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Icon(CoffeePeekIcons.Email, contentDescription = null, tint = CoffeePeekColors.GoldWarm, modifier = Modifier.size(16.dp))
        Text(
            text = email,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 8.dp, end = 4.dp),
        )
        IconButton(onClick = onChange, enabled = enabled, modifier = Modifier.size(44.dp)) {
            Icon(CoffeePeekIcons.Edit, contentDescription = stringResource(R.string.auth_change_email), tint = CoffeePeekColors.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PasswordStrengthBar(password: String) {
    if (password.isEmpty()) return
    val strength = passwordStrength(password)
    val score = strength.score
    val color = listOf(CoffeePeekColors.Error, CoffeePeekColors.Primary, CoffeePeekColors.Success)[(score - 1).coerceIn(0, 2)]
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val track = if (isDark) CoffeePeekColors.DarkBorder else CoffeePeekColors.LightBorder
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(3) { i ->
            Box(Modifier.weight(1f).height(4.dp).background(if (i < score) color else track, RoundedCornerShape(99.dp)))
        }
        if (score > 0) {
            Text(
                text = stringResource(
                    when (strength) {
                        PasswordStrength.Weak -> R.string.auth_strength_weak
                        PasswordStrength.Medium -> R.string.auth_strength_medium
                        else -> R.string.auth_strength_strong
                    },
                ),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RegisterDetailsPreview() = CoffeePeekTheme {
    RegisterContent(RegisterState(step = RegisterStep.Details, email = "name@example.com", name = "Анна", password = "password1"), {}, {}, {}, onGoogle = {})
}

@PreviewLightDark
@Composable
private fun RegisterEmailPreview() = CoffeePeekTheme {
    RegisterContent(RegisterState(email = "taken@example.com", emailTaken = true), {}, {}, {}, onGoogle = {})
}

@PreviewLightDark
@Composable
private fun RegisterSuccessPreview() = CoffeePeekTheme {
    RegisterContent(RegisterState(step = RegisterStep.Success, email = "new@example.com"), {}, {}, {}, onGoogle = {})
}
