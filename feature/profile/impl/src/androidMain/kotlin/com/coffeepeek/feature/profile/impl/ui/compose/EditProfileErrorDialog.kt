package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekErrorDialog
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.profile.impl.R
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileError
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileState
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun EditProfileErrorDialog(state: EditProfileState, onDismiss: () -> Unit) {
    val error = state.error ?: return
    val locale = LocalConfiguration.current.locales[0]
    val retryAt = formatUsernameRetryAt(state.usernameRetryAtUtc, locale, ZoneId.systemDefault())
    CoffeePeekErrorDialog(
        show = true,
        title = stringResource(error.titleResource()),
        message = if (error == EditProfileError.UsernameLimit && retryAt != null) {
            stringResource(R.string.profile_edit_error_username_retry_at, retryAt)
        } else stringResource(error.messageResource()),
        dismissLabel = stringResource(R.string.profile_edit_error_dismiss),
        onDismiss = onDismiss,
    )
}

private fun EditProfileError.titleResource(): Int = when (this) {
    EditProfileError.UsernameLimit -> R.string.profile_edit_error_limit_title
    EditProfileError.UsernameTaken -> R.string.profile_edit_error_taken_title
    EditProfileError.Load -> R.string.profile_edit_error_load_title
    else -> R.string.profile_edit_error_title
}

private fun EditProfileError.messageResource(): Int = when (this) {
    EditProfileError.Load -> R.string.profile_edit_error_load
    EditProfileError.AvatarTooLarge -> R.string.profile_edit_error_avatar_size
    EditProfileError.Avatar -> R.string.profile_edit_error_avatar
    EditProfileError.Username -> R.string.profile_edit_error_username
    EditProfileError.About -> R.string.profile_edit_error_about
    EditProfileError.UsernameLimit -> R.string.profile_edit_error_username_limit
    EditProfileError.UsernameTaken -> R.string.profile_edit_error_username_taken
    EditProfileError.InvalidUsername -> R.string.profile_edit_error_username_invalid
    EditProfileError.SignInRequired -> R.string.profile_edit_error_sign_in
}

internal fun formatUsernameRetryAt(value: String?, locale: Locale, zone: ZoneId): String? {
    if (value.isNullOrBlank()) return null
    return try {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale).withZone(zone).format(Instant.parse(value))
    } catch (_: DateTimeException) {
        null
    }
}

@PreviewLightDark
@Composable
private fun EditProfileErrorDialogPreview() {
    CoffeePeekTheme {
        EditProfileErrorDialog(
            EditProfileState(error = EditProfileError.UsernameLimit, usernameRetryAtUtc = "2026-10-29T17:12:31Z"), {},
        )
    }
}
