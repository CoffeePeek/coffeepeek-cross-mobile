package com.coffeepeek.admin.ui.screen.editprofile

import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.CompactOutlinedTextField
import com.coffeepeek.admin.ui.component.CoffeePeekLoader
import com.coffeepeek.admin.ui.component.CpCircularBackButton
import com.coffeepeek.admin.ui.component.platformTextInputOptions
import com.coffeepeek.admin.ui.component.rememberSyncedTextFieldValue
import com.coffeepeek.admin.ui.component.limitTextLength
import com.coffeepeek.admin.ui.component.PhotoSourceBottomSheet
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.admin.di.platformViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(vm: EditProfileViewModel = platformViewModel()) {
    val state by vm.state.collectAsState()
    var isPhotoLoading by remember { mutableStateOf(false) }
    var showAvatarSourceSheet by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    val aboutField = rememberSyncedTextFieldValue(state.about)
    val photoPicker = rememberPhotoPicker(
        maxSelection = 1,
        isLoading = { isPhotoLoading = it },
        onPhotosPicked = { images -> images.firstOrNull()?.let(vm::onAvatarPicked) },
    )

    if (showAvatarSourceSheet) {
        PhotoSourceBottomSheet(
            onDismiss = { showAvatarSourceSheet = false },
            onGallery = {
                showAvatarSourceSheet = false
                photoPicker.pickFromGallery()
            },
            onCamera = {
                showAvatarSourceSheet = false
                photoPicker.takePhoto()
            },
            title = "Фото профиля",
        )
    }

    // Диалог ошибки
    state.error?.let { err ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Ошибка", style = MaterialTheme.typography.headlineSmall) },
            text = {
                Text(err, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            confirmButton = {
                TextButton(onClick = vm::clearError) {
                    Text("Понятно", style = MaterialTheme.typography.labelLarge)
                }
            },
            shape = RoundedCornerShape(CpDimens.radius2xl),
        )
    }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteAccountDialog = false
                vm.startAccountDeletion()
            },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Редактировать профиль",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = { CpCircularBackButton(onClick = Navigator::popBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.padding(horizontal = CpDimens.spacing4),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CoffeePeekLoader()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(CpDimens.spacing4),
        ) {
            // ── Аватар ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarPickerSection(
                    avatarUrl = state.avatarUrl,
                    pendingAvatar = state.pendingAvatar,
                    initials = state.username.take(2).uppercase().ifEmpty { "?" },
                    isLoading = isPhotoLoading,
                    onClick = { showAvatarSourceSheet = true },
                )

                Column(modifier = Modifier.weight(1f)) {
                    FieldLabel("Имя пользователя")
                    CompactOutlinedTextField(
                        value = state.username,
                        onValueChange = vm::onUsernameChange,
                        maxLength = 64,
                        modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight),
                        placeholder = {
                            Text(
                                "Ваше имя",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        isError = state.username.isNotEmpty() && state.usernameError != null,
                        singleLine = true,
                        contentPadding = CpDimens.singleLineFieldContentPadding,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        shape = RoundedCornerShape(percent = 50),
                        colors = fieldColors(),
                    )
                    FieldFooter(
                        error = if (state.username.isNotEmpty()) state.usernameError else null,
                        counter = "${state.username.length}/64",
                    )
                }
            }

            Spacer(Modifier.height(CpDimens.spacing4))

            // ── О себе ───────────────────────────────────────────────────────
            FieldLabel("О себе")
            OutlinedTextField(
                value = aboutField.value,
                onValueChange = { updated ->
                    val limited = updated.limitTextLength(600)
                    aboutField.value = limited
                    vm.onAboutChange(limited.text)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Расскажите немного о себе…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                isError = state.aboutError != null,
                minLines = 4,
                maxLines = 8,
                keyboardOptions = platformTextInputOptions(KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                )),
                shape = RoundedCornerShape(CpDimens.buttonRadius),
                colors = fieldColors(),
            )
            FieldFooter(
                error = state.aboutError,
                counter = "${state.about.length}/600",
            )

            Spacer(Modifier.height(CpDimens.spacing6))

            // ── Кнопка сохранения ─────────────────────────────────────────────
            Button(
                onClick = vm::save,
                modifier = Modifier.fillMaxWidth().height(CpDimens.buttonHeight),
                enabled = state.canSave,
                shape = RoundedCornerShape(CpDimens.buttonRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                ),
            ) {
                if (state.isSaving) {
                    CoffeePeekLoader(
                        color = MaterialTheme.colorScheme.onPrimary,
                        size = CpDimens.loaderButton,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Сохранить изменения", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(CpDimens.spacing4))

            TextButton(
                onClick = { showDeleteAccountDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = CpIcons.Delete,
                    contentDescription = null,
                    tint = CpColor.Error,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(CpDimens.spacing2))
                Text(
                    text = "Удалить аккаунт",
                    style = MaterialTheme.typography.labelLarge,
                    color = CpColor.Error,
                )
            }

            Spacer(Modifier.height(CpDimens.spacing8))
        }
    }
}

// ── Вспомогательные компоненты ────────────────────────────────────────────────

@Composable
private fun AvatarPickerSection(
    avatarUrl: String?,
    pendingAvatar: com.coffeepeek.admin.utils.PickedImage?,
    initials: String,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(104.dp)
            .clickable(
                enabled = !isLoading,
                onClickLabel = "Изменить фото профиля",
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CpColor.Primary, CpColor.GoldWarm))),
            contentAlignment = Alignment.Center,
        ) {
            when {
                pendingAvatar != null -> {
                    CpImage(
                        data = pendingAvatar.bytes,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                !avatarUrl.isNullOrBlank() -> {
                    CpImage(
                        data = avatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }
                else -> {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium,
                        color = CpColor.DarkTextOnPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CoffeePeekLoader(size = CpDimens.loaderButton, strokeWidth = 2.dp)
                }
            }
        }
        if (!isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = CpIcons.Camera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}


@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
    )
}

@Composable
private fun FieldFooter(error: String?, counter: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        if (counter != null) {
            Text(
                text = counter,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor    = MaterialTheme.colorScheme.outline,
    unfocusedBorderColor  = MaterialTheme.colorScheme.outline,
    errorBorderColor      = MaterialTheme.colorScheme.error,
    focusedContainerColor    = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor  = MaterialTheme.colorScheme.surface,
)

@Composable
private fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Удалить аккаунт?", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Text(
                text = "Мы отправим письмо со ссылкой для подтверждения. Аккаунт останется активным, пока вы не подтвердите удаление в браузере.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.height(CpDimens.buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CpColor.Error,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(percent = 50),
            ) {
                Text("Удалить", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Отмена",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        shape = RoundedCornerShape(CpDimens.radius2xl),
    )
}
