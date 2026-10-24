package com.coffeepeek.feature.profile.impl.ui.compose

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.profile.impl.resources.Res
import com.coffeepeek.feature.profile.impl.resources.profile_back
import com.coffeepeek.feature.profile.impl.resources.profile_cancel
import com.coffeepeek.feature.profile.impl.resources.profile_edit_about_label
import com.coffeepeek.feature.profile.impl.resources.profile_edit_about_long
import com.coffeepeek.feature.profile.impl.resources.profile_edit_about_placeholder
import com.coffeepeek.feature.profile.impl.resources.profile_edit_avatar_description
import com.coffeepeek.feature.profile.impl.resources.profile_edit_change_photo
import com.coffeepeek.feature.profile.impl.resources.profile_edit_delete_account
import com.coffeepeek.feature.profile.impl.resources.profile_edit_delete_confirm
import com.coffeepeek.feature.profile.impl.resources.profile_edit_delete_message
import com.coffeepeek.feature.profile.impl.resources.profile_edit_delete_title
import com.coffeepeek.feature.profile.impl.resources.profile_edit_save
import com.coffeepeek.feature.profile.impl.resources.profile_edit_saving
import com.coffeepeek.feature.profile.impl.resources.profile_edit_title
import com.coffeepeek.feature.profile.impl.resources.profile_edit_username_empty
import com.coffeepeek.feature.profile.impl.resources.profile_edit_username_label
import com.coffeepeek.feature.profile.impl.resources.profile_edit_username_long
import com.coffeepeek.feature.profile.impl.resources.profile_edit_username_placeholder
import com.coffeepeek.feature.profile.impl.resources.profile_edit_username_short
import com.coffeepeek.feature.profile.impl.resources.profile_loading
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.component.rememberCoffeePeekSyncedText
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.profile.api.EditProfileSlots
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.impl.ui.EditProfileViewModel
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileAction
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileEvent
import com.coffeepeek.feature.profile.impl.ui.model.EditProfileState
import com.coffeepeek.feature.profile.impl.ui.model.UsernameError

@Composable
internal fun EditProfileScreen(
    vm: EditProfileViewModel,
    slots: EditProfileSlots,
    onBack: () -> Unit,
    onOpenAccountDeletion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var photoLoading by remember { mutableStateOf(false) }
    var showSources by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(vm) { vm.events.collect { event -> when (event) { EditProfileEvent.Saved -> onBack() } } }
    LaunchedEffect(state.usernameToConfirm, state.error) {
        if (state.usernameToConfirm != null || state.error != null) keyboard?.hide()
    }
    slots.photoPicker(showSources, { showSources = false }, { photoLoading = it }) { vm.onAction(EditProfileAction.AvatarPicked(it)) }
    EditProfileErrorDialog(state) { vm.onAction(EditProfileAction.DismissError) }
    state.usernameToConfirm?.let { username ->
        UsernameChangeConfirmation(
            username = username,
            onConfirm = { vm.onAction(EditProfileAction.ConfirmUsernameChange) },
            onDismiss = { vm.onAction(EditProfileAction.CancelUsernameChange) },
        )
    }
    if (showDeleteDialog) DeleteAccountDialog(
        onDismiss = { showDeleteDialog = false },
        onConfirm = { showDeleteDialog = false; onOpenAccountDeletion() },
    )
    EditProfileScreenContent(
        state, slots, photoLoading, vm::onAction, onBack,
        onPickAvatar = { showSources = true }, onDeleteAccount = { showDeleteDialog = true }, modifier = modifier,
    )
}

@Composable
internal fun EditProfileScreenContent(
    state: EditProfileState,
    slots: EditProfileSlots,
    photoLoading: Boolean,
    onAction: (EditProfileAction) -> Unit,
    onBack: () -> Unit,
    onPickAvatar: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(stringResource(Res.string.profile_edit_title), stringResource(Res.string.profile_back), onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.profile_loading))
            }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4)) {
                EditProfileIdentity(state, slots, photoLoading, onAction, onPickAvatar)
                Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                EditProfileAbout(state, onAction)
                Spacer(Modifier.height(CoffeePeekDimens.spacing6))
                SaveProfileButton(state) { onAction(EditProfileAction.Save) }
                Spacer(Modifier.height(CoffeePeekDimens.spacing4))
                DeleteAccountButton(!state.isSaving, onDeleteAccount)
                Spacer(Modifier.height(CoffeePeekDimens.spacing8))
            }
        }
    }
}

@Composable
private fun EditProfileIdentity(
    state: EditProfileState, slots: EditProfileSlots, photoLoading: Boolean,
    onAction: (EditProfileAction) -> Unit, onPickAvatar: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4), verticalAlignment = Alignment.CenterVertically) {
        AvatarPicker(state, slots, photoLoading, onPickAvatar)
        Column(Modifier.weight(1f)) { EditProfileUsername(state, onAction) }
    }
}

@Composable
private fun EditProfileUsername(state: EditProfileState, onAction: (EditProfileAction) -> Unit) {
    FieldLabel(stringResource(Res.string.profile_edit_username_label))
    val username = rememberCoffeePeekSyncedText(state.username)
    val usernameError = state.usernameError.takeIf { state.username.isNotEmpty() }?.let { it.message() }
    OutlinedTextField(
        value = username.value,
        onValueChange = { updated ->
            val limited = if (updated.text.length > EditProfileState.MAX_USERNAME) updated.copy(text = updated.text.take(EditProfileState.MAX_USERNAME)) else updated
            username.value = limited
            onAction(EditProfileAction.UsernameChanged(limited.text))
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = CoffeePeekDimens.buttonHeight),
        enabled = !state.isSaving,
        placeholder = { Placeholder(stringResource(Res.string.profile_edit_username_placeholder)) },
        textStyle = MaterialTheme.typography.bodyLarge,
        isError = usernameError != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
        shape = RoundedCornerShape(percent = 50),
        colors = fieldColors(),
    )
    FieldFooter(usernameError, "${state.username.length}/${EditProfileState.MAX_USERNAME}")
}

@Composable
private fun EditProfileAbout(state: EditProfileState, onAction: (EditProfileAction) -> Unit) {
    FieldLabel(stringResource(Res.string.profile_edit_about_label))
    val about = rememberCoffeePeekSyncedText(state.about)
    OutlinedTextField(
        value = about.value,
        onValueChange = { updated ->
            val limited = if (updated.text.length > EditProfileState.MAX_ABOUT) updated.copy(text = updated.text.take(EditProfileState.MAX_ABOUT)) else updated
            about.value = limited
            onAction(EditProfileAction.AboutChanged(limited.text))
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.isSaving,
        placeholder = { Placeholder(stringResource(Res.string.profile_edit_about_placeholder)) },
        textStyle = MaterialTheme.typography.bodyLarge,
        isError = state.aboutTooLong,
        minLines = 4,
        maxLines = 8,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        colors = fieldColors(),
    )
    FieldFooter(if (state.aboutTooLong) stringResource(Res.string.profile_edit_about_long) else null, "${state.about.length}/${EditProfileState.MAX_ABOUT}")
}

@Composable
private fun SaveProfileButton(state: EditProfileState, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight),
        enabled = state.canSave,
        shape = RoundedCornerShape(CoffeePeekDimens.buttonRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
        ),
    ) {
        if (state.isSaving) CoffeePeekLoader(stringResource(Res.string.profile_edit_saving), size = 20.dp, color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
        else Text(stringResource(Res.string.profile_edit_save), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun DeleteAccountButton(enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = enabled) {
        Icon(CoffeePeekIcons.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(CoffeePeekDimens.spacing2))
        Text(stringResource(Res.string.profile_edit_delete_account), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.profile_edit_delete_title)) },
        text = { Text(stringResource(Res.string.profile_edit_delete_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.profile_edit_delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.profile_cancel)) } },
    )
}



@Preview
@Composable
private fun EditProfileScreenPreview() {
    CoffeePeekTheme {
        EditProfileScreenContent(
            state = EditProfileState(username = "coffee_lover", originalUsername = "coffee_lover", isLoading = false),
            slots = EditProfileSlots(image = { _, _, _ -> }, pickedAvatar = { _, _ -> }, photoPicker = { _, _, _, _ -> }),
            photoLoading = false, onAction = {}, onBack = {}, onPickAvatar = {}, onDeleteAccount = {},
        )
    }
}

@Composable
private fun UsernameError.message(): String = stringResource(when (this) {
    UsernameError.Empty -> Res.string.profile_edit_username_empty
    UsernameError.TooShort -> Res.string.profile_edit_username_short
    UsernameError.TooLong -> Res.string.profile_edit_username_long
})

@Composable
private fun AvatarPicker(state: EditProfileState, slots: EditProfileSlots, loading: Boolean, onClick: () -> Unit) {
    val changePhoto = stringResource(Res.string.profile_edit_change_photo)
    val avatarDescription = stringResource(Res.string.profile_edit_avatar_description)
    Box(Modifier.size(104.dp).clickable(enabled = !loading && !state.isSaving, onClickLabel = changePhoto, onClick = onClick)) {
        Box(
            Modifier.align(Alignment.Center).size(96.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(CoffeePeekColors.Primary, CoffeePeekColors.GoldWarm))),
            contentAlignment = Alignment.Center,
        ) {
            val pending: ProfileAvatarImage? = state.pendingAvatar
            when {
                pending != null -> slots.pickedAvatar(pending, Modifier.fillMaxSize().semantics { contentDescription = avatarDescription })
                !state.avatarUrl.isNullOrBlank() -> slots.image(state.avatarUrl, avatarDescription, Modifier.fillMaxSize())
                else -> Text(
                    state.username.take(2).uppercase().ifEmpty { "?" },
                    style = MaterialTheme.typography.headlineMedium, color = CoffeePeekColors.DarkTextOnPrimary, fontWeight = FontWeight.Bold,
                )
            }
            if (loading) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.profile_loading), size = 20.dp, strokeWidth = 2.dp)
            }
        }
        if (!loading) Box(
            Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(CoffeePeekIcons.Camera, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, Modifier.padding(bottom = 6.dp, start = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Placeholder(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FieldFooter(error: String?, counter: String) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (error != null) Text(error, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        else Spacer(Modifier.weight(1f))
        Text(counter, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.outline, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)
