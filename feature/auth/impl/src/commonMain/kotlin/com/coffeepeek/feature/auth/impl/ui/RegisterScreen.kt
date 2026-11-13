package com.coffeepeek.feature.auth.impl.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.feature.auth.api.GoogleSignInLauncher
import com.coffeepeek.feature.auth.impl.resources.Res
import com.coffeepeek.feature.auth.impl.resources.auth_already_have_account
import com.coffeepeek.feature.auth.impl.resources.auth_analytics_description
import com.coffeepeek.feature.auth.impl.resources.auth_analytics_title
import com.coffeepeek.feature.auth.impl.resources.auth_check_failed
import com.coffeepeek.feature.auth.impl.resources.auth_email_label
import com.coffeepeek.feature.auth.impl.resources.auth_email_placeholder
import com.coffeepeek.feature.auth.impl.resources.auth_email_taken
import com.coffeepeek.feature.auth.impl.resources.auth_go_to_login
import com.coffeepeek.feature.auth.impl.resources.auth_login
import com.coffeepeek.feature.auth.impl.resources.auth_name_label
import com.coffeepeek.feature.auth.impl.resources.auth_name_placeholder
import com.coffeepeek.feature.auth.impl.resources.auth_name_required
import com.coffeepeek.feature.auth.impl.resources.auth_name_too_short
import com.coffeepeek.feature.auth.impl.resources.auth_password_label
import com.coffeepeek.feature.auth.impl.resources.auth_privacy_link
import com.coffeepeek.feature.auth.impl.resources.auth_register_failed
import com.coffeepeek.feature.auth.impl.resources.auth_register_password_placeholder
import com.coffeepeek.feature.auth.impl.resources.auth_register_submit
import com.coffeepeek.feature.auth.impl.resources.auth_register_success_subtitle
import com.coffeepeek.feature.auth.impl.resources.auth_register_success_title
import com.coffeepeek.feature.auth.impl.resources.auth_register_title
import com.coffeepeek.feature.auth.impl.resources.auth_terms_and
import com.coffeepeek.feature.auth.impl.resources.auth_terms_link
import com.coffeepeek.feature.auth.impl.resources.auth_terms_prefix
import com.coffeepeek.feature.auth.impl.resources.auth_terms_required
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.coffeepeek.feature.auth.impl.register.RegisterEvent
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.coffeePeekAccentForeground
import com.coffeepeek.feature.auth.domain.MIN_REGISTRATION_NAME_LENGTH
import com.coffeepeek.feature.auth.domain.RegistrationIssue
import com.coffeepeek.feature.auth.domain.canSubmitRegistration
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
    google: GoogleSignInLauncher?,
    modifier: Modifier = Modifier,
    appleAvailable: Boolean = false,
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
        onGoogle = google?.rememberSignIn(
            onToken = { vm.onAction(RegisterAction.GoogleToken(it)) },
            onFailure = { vm.onAction(RegisterAction.GoogleFailed(it)) },
        ),
        modifier = modifier,
        onApple = if (appleAvailable) ({ vm.onAction(RegisterAction.AppleSignIn) }) else null,
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
    onApple: (() -> Unit)? = null,
) {
    key(state.step) {
        AuthScaffold(
            title = stringResource(Res.string.auth_register_title),
            onClose = onClose,
            modifier = modifier,
        ) {
            when (state.step) {
                RegisterStep.Form -> RegisterForm(state, onAction, onGoToLogin, onGoogle, onApple)
                RegisterStep.Success -> SuccessStep(state.email, onGoToLogin)
            }
        }
    }
}

@Composable
private fun RegisterForm(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
    onGoToLogin: () -> Unit,
    onGoogle: (() -> Unit)?,
    onApple: (() -> Unit)?,
) {
    val canSubmit = state.email.isNotBlank() && canSubmitRegistration(state.name, state.password, state.termsAccepted)
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val email = remember { BringIntoViewRequester() }
    val terms = remember { BringIntoViewRequester() }
    val submit = {
        focus.clearFocus(force = true)
        keyboard?.hide()
        onAction(RegisterAction.Submit)
    }
    LaunchedEffect(state.reported, state.emailTaken) {
        when {
            state.emailTaken || state.reported == RegistrationIssue.EmailRequired ||
                state.reported == RegistrationIssue.EmailInvalid -> email.bringIntoView()
            state.reported == RegistrationIssue.TermsRequired -> terms.bringIntoView()
        }
    }
    RegisterFields(state, onAction, Modifier.bringIntoViewRequester(email), onDone = submit)
    RegisterConsent(state, onAction, Modifier.bringIntoViewRequester(terms))
    RegisterFailure(state)
    AuthPrimaryButton(
        text = stringResource(Res.string.auth_register_submit),
        onClick = submit,
        enabled = canSubmit,
        loading = state.busy,
    )
    RegisterLoginLink(onGoToLogin, enabled = !state.busy)
    if (onGoogle != null || onApple != null) {
        AuthOrDivider()
        SocialSignInButtons(onGoogle, onApple, !state.busy)
        SocialFailureText(state.socialFailure)
    }
}

@Composable
private fun RegisterFields(
    state: RegisterState, onAction: (RegisterAction) -> Unit, emailModifier: Modifier, onDone: () -> Unit,
) {
    val nameFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    RegisterEmailField(state, onAction, emailModifier, onNext = { nameFocus.requestFocus() })
    RegisterNameField(state, onAction, Modifier.focusRequester(nameFocus), onNext = { passwordFocus.requestFocus() })
    RegisterPasswordField(state, onAction, Modifier.focusRequester(passwordFocus), onDone)
}

@Composable
private fun RegisterEmailField(
    state: RegisterState, onAction: (RegisterAction) -> Unit, modifier: Modifier, onNext: () -> Unit,
) {
    AuthTextField(
        value = state.email,
        onValueChange = { onAction(RegisterAction.EmailChanged(it)) },
        label = stringResource(Res.string.auth_email_label),
        placeholder = stringResource(Res.string.auth_email_placeholder),
        modifier = modifier,
        errorText = when {
            state.emailTaken -> stringResource(Res.string.auth_email_taken)
            state.emailIssue != null -> emailError(state.emailIssue)
            else -> null
        },
        enabled = !state.busy,
        next = true,
        onNext = onNext,
    )
}

@Composable
private fun RegisterNameField(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
    fieldModifier: Modifier,
    onNext: () -> Unit,
) {
    val nameLength = state.name.trim().length
    AuthTextField(
        label = stringResource(Res.string.auth_name_label),
        value = state.name,
        onValueChange = { onAction(RegisterAction.NameChanged(it)) },
        placeholder = stringResource(Res.string.auth_name_placeholder),
        text = true,
        next = true,
        enabled = !state.busy,
        onNext = onNext,
        fieldModifier = fieldModifier,
        errorText = if (nameLength in 1 until MIN_REGISTRATION_NAME_LENGTH) {
            stringResource(Res.string.auth_name_too_short, MIN_REGISTRATION_NAME_LENGTH)
        } else if (state.reported == RegistrationIssue.NameRequired) stringResource(Res.string.auth_name_required) else null,
    )
}

@Composable
private fun RegisterPasswordField(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
    fieldModifier: Modifier,
    onDone: (() -> Unit)?,
) {
    val label = stringResource(Res.string.auth_password_label)
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        AuthTextField(
            label = label,
            value = state.password,
            onValueChange = { onAction(RegisterAction.PasswordChanged(it)) },
            placeholder = label,
            isPassword = true,
            newPassword = true,
            errorText = state.passwordIssue?.let { passwordError(it) },
            enabled = !state.busy,
            fieldModifier = fieldModifier,
            onDone = onDone,
        )
        if (state.passwordIssue == null) {
            Text(
                text = stringResource(Res.string.auth_register_password_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RegisterConsent(state: RegisterState, onAction: (RegisterAction) -> Unit, termsModifier: Modifier) {
    CheckRow(state.termsAccepted, { onAction(RegisterAction.TermsChanged(it)) }, enabled = !state.busy,
        modifier = termsModifier) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            TermsText()
            if (!state.termsAccepted && canSubmitRegistration(state.name, state.password, termsAccepted = true)) {
                Text(
                    text = stringResource(Res.string.auth_terms_required),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    CheckRow(state.analyticsAccepted, { onAction(RegisterAction.AnalyticsChanged(it)) }, enabled = !state.busy) {
        Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Text(
                text = stringResource(Res.string.auth_analytics_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.auth_analytics_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RegisterLoginLink(onGoToLogin: () -> Unit, enabled: Boolean) {
    AuthFooterRow(
        text = stringResource(Res.string.auth_login),
        prompt = stringResource(Res.string.auth_already_have_account),
        onClick = onGoToLogin,
        enabled = enabled,
    )
}

@Composable
private fun SuccessStep(email: String, onGoToLogin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(
            text = stringResource(Res.string.auth_register_success_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(Res.string.auth_register_success_subtitle, email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    AuthPrimaryButton(text = stringResource(Res.string.auth_go_to_login), onClick = onGoToLogin)
}

/** The request error: the server's own message as the previous dialog showed it, or the generic text of the step. */
@Composable
private fun RegisterFailure(state: RegisterState) {
    val failure = state.failure ?: return
    val generic = if (failure.checkingEmail) Res.string.auth_check_failed else Res.string.auth_register_failed
    Text(
        text = failure.message ?: stringResource(generic),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun CheckRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    val showFocus = enabled && focused && LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Row(
        modifier = modifier.fillMaxWidth()
            .then(if (showFocus) Modifier.border(1.dp, MaterialTheme.colorScheme.coffeePeekAccentForeground,
                RoundedCornerShape(CoffeePeekDimens.radiusMd)) else Modifier)
            .toggleable(
                value = checked,
                interactionSource = interactions,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
            .heightIn(min = CoffeePeekDimens.glassControlSize)
            .padding(vertical = CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = CheckboxDefaults.colors(checkedColor = CoffeePeekColors.Primary, checkmarkColor = CoffeePeekColors.DarkTextOnPrimary),
            modifier = Modifier.size(24.dp),
        )
        Column(Modifier.padding(start = CoffeePeekDimens.spacing3), content = { content() })
    }
}

@Composable
private fun TermsText() {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.coffeePeekAccentForeground, fontWeight = FontWeight.SemiBold),
    )
    val text = buildAnnotatedString {
        append(stringResource(Res.string.auth_terms_prefix))
        append(' ')
        withLink(LinkAnnotation.Url(TermsUrl, linkStyles)) { append(stringResource(Res.string.auth_terms_link)) }
        append(' ')
        append(stringResource(Res.string.auth_terms_and))
        append(' ')
        withLink(LinkAnnotation.Url(PrivacyUrl, linkStyles)) { append(stringResource(Res.string.auth_privacy_link)) }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RegisterPreview(state: RegisterState, dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    RegisterContent(state, {}, {}, {}, onGoogle = {}, onApple = {})
}

@Preview @Composable private fun RegisterFormLightPreview() = RegisterPreview(RegisterState(), false)
@Preview @Composable private fun RegisterFormDarkPreview() = RegisterPreview(RegisterState(), true)
@Preview @Composable private fun RegisterTakenEmailLightPreview() = RegisterPreview(RegisterState(email = "taken@example.com", emailTaken = true), false)
@Preview @Composable private fun RegisterTakenEmailDarkPreview() = RegisterPreview(RegisterState(email = "taken@example.com", emailTaken = true), true)
@Preview @Composable private fun RegisterSuccessLightPreview() = RegisterPreview(RegisterState(step = RegisterStep.Success, email = "new@example.com"), false)
@Preview @Composable private fun RegisterSuccessDarkPreview() = RegisterPreview(RegisterState(step = RegisterStep.Success, email = "new@example.com"), true)
