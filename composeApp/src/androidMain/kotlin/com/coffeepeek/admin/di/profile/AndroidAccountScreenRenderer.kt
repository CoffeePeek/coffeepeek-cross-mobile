package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.auth.GoogleAuth
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.PhotoSourceBottomSheet
import com.coffeepeek.admin.ui.screen.profile.AccountScreenRenderer
import com.coffeepeek.admin.utils.CpImage
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.repository.UserRepository
import com.coffeepeek.feature.profile.api.AccountDeletionEntry
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.EditProfileEntry
import com.coffeepeek.feature.profile.api.EditProfileSlots
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileDeletionRequest
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.impl.createAccountDeletionEntry
import com.coffeepeek.feature.profile.impl.createEditProfileEntry
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Android routes the edit and deletion screens to the migrated entries. Navigation, the platform sign-out after a session end and the
 * photo picker stay app-owned.
 */
internal class AndroidAccountScreenRenderer(
    private val edit: EditProfileEntry,
    private val deletion: AccountDeletionEntry,
    /** Outlives the screen: the redirect must still happen after the screen has left composition with the session. */
    private val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val platformSignOut: suspend () -> Unit = { GoogleAuth.signOut() },
    private val openLogin: () -> Unit = { Navigator.openLoginAfterSessionEnd() },
) : AccountScreenRenderer {
    @Composable
    override fun EditProfile() {
        edit.Content(
            onBack = Navigator::popBack,
            onOpenAccountDeletion = { Navigator.navigate(Navigator.Screen.DeleteAccountPending) },
        )
    }

    @Composable
    override fun DeletionPending() {
        deletion.Content(
            onClose = Navigator::popBack,
            onSessionEnded = ::endSession,
        )
    }

    /** Legacy order: platform sign-out first, then the login redirect the root consumes on the next login-state change. */
    internal fun endSession() {
        appScope.launch { platformSignOut(); openLogin() }
    }
}

/** Writes through the shared `UserRepository`, which updates the cached profile the Profile tab observes. */
internal class AndroidProfileEditPort(private val users: UserRepository) : ProfileEditPort {
    override suspend fun updateAvatar(image: ProfileAvatarImage): Result<Unit> =
        users.updateAvatar(PendingPhotoUpload(fileName = image.fileName, contentType = image.contentType, bytes = image.bytes))

    override suspend fun updateUsername(username: String): Result<Unit> =
        users.updateUsername(username).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it.toUsernameChangeFailure()) },
        )
    override suspend fun updateAbout(about: String): Result<Unit> = users.updateAbout(about)
}

internal class AndroidAccountDeletionPort(private val users: UserRepository) : AccountDeletionPort {
    override suspend fun existingRequest(): Result<ProfileDeletionRequest?> =
        users.getAccountDeletionRequest().map { it?.let { request -> ProfileDeletionRequest(request.resendAvailableAtUtc) } }

    override suspend fun request(): Result<ProfileDeletionRequest> =
        users.requestAccountDeletion().map { ProfileDeletionRequest(it.resendAvailableAtUtc) }
}

internal fun androidEditProfileSlots() = EditProfileSlots(
    image = { url, description, modifier -> CpImage(data = url, modifier = modifier, contentScale = ContentScale.Crop, contentDescription = description) },
    pickedAvatar = { image, modifier -> CpImage(data = image.bytes, modifier = modifier, contentScale = ContentScale.Crop) },
    photoPicker = { show, onDismiss, onLoading, onPicked ->
        val picker = rememberPhotoPicker(
            maxSelection = 1,
            isLoading = onLoading,
            onPhotosPicked = { images: List<PickedImage> -> images.firstOrNull()?.let { onPicked(ProfileAvatarImage(it.fileName, it.contentType, it.bytes)) } },
        )
        if (show) PhotoSourceBottomSheet(
            onDismiss = onDismiss,
            onGallery = { onDismiss(); picker.pickFromGallery() },
            onCamera = { onDismiss(); picker.takePhoto() },
            title = "Фото профиля",
        )
    },
)

internal fun androidAccountScreenRenderer(sessions: SessionRepository, users: UserRepository): AndroidAccountScreenRenderer {
    val accounts = AndroidProfileAccountPort(sessions, users)
    return AndroidAccountScreenRenderer(
        edit = createEditProfileEntry(accounts, AndroidProfileEditPort(users), androidEditProfileSlots()),
        deletion = createAccountDeletionEntry(accounts, AndroidAccountDeletionPort(users)),
    )
}
