package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coffeepeek.admin.auth.GoogleAuth
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.PhotoSourceBottomSheet
import com.coffeepeek.admin.ui.screen.profile.AccountScreenRenderer
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.feature.profile.api.AccountDeletionEntry
import com.coffeepeek.feature.profile.api.AccountDeletionPort
import com.coffeepeek.feature.profile.api.EditProfileEntry
import com.coffeepeek.feature.profile.api.EditProfileSlots
import com.coffeepeek.feature.profile.api.ProfileAvatarImage
import com.coffeepeek.feature.profile.api.ProfileEditPort
import com.coffeepeek.feature.profile.impl.createAccountDeletionEntry
import com.coffeepeek.feature.profile.impl.createEditProfileEntry
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.feature.profile.api.ProfileAccountPort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Both platforms route edit and deletion screens to feature entries. Navigation, platform sign-out and the
 * photo picker stay app-owned.
 */
internal class FeatureAccountScreenRenderer(
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
            modifier = Modifier,
        )
    }

    @Composable
    override fun DeletionPending() {
        deletion.Content(
            onClose = Navigator::popBack,
            onSessionEnded = ::endSession,
            modifier = Modifier,
        )
    }

    /** Legacy order: platform sign-out first, then the login redirect the root consumes on the next login-state change. */
    internal fun endSession() {
        appScope.launch { platformSignOut(); openLogin() }
    }
}

internal fun editProfileSlots() = EditProfileSlots(
    image = { url, description, modifier -> CoffeePeekImage(data = url, modifier = modifier, contentScale = ContentScale.Crop, contentDescription = description) },
    pickedAvatar = { image, modifier -> CoffeePeekImage(data = image.bytes, modifier = modifier, contentScale = ContentScale.Crop) },
    photoPicker = { title, show, onDismiss, onLoading, onPicked ->
        val picker = rememberPhotoPicker(
            maxSelection = 1,
            isLoading = onLoading,
            onPhotosPicked = { images: List<PickedImage> -> images.firstOrNull()?.let { onPicked(ProfileAvatarImage(it.fileName, it.contentType, it.bytes)) } },
        )
        if (show) PhotoSourceBottomSheet(
            onDismiss = onDismiss,
            onGallery = { onDismiss(); picker.pickFromGallery() },
            onCamera = { onDismiss(); picker.takePhoto() },
            title = title,
        )
    },
)

internal fun featureAccountScreenRenderer(accounts: ProfileAccountPort, edit: ProfileEditPort, deletion: AccountDeletionPort): FeatureAccountScreenRenderer {
    return FeatureAccountScreenRenderer(
        edit = createEditProfileEntry(accounts, edit, editProfileSlots()),
        deletion = createAccountDeletionEntry(accounts, deletion),
    )
}
