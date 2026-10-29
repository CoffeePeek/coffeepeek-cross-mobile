package com.coffeepeek.feature.profile.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** App-rendered pieces of the edit form: image loading, the preview of a picked file and the gallery/camera flow. */
class EditProfileSlots(
    val image: ProfileImage,
    /** Preview of a picked avatar that is not uploaded yet. */
    val pickedAvatar: @Composable (image: ProfileAvatarImage, modifier: Modifier) -> Unit,
    /**
     * The photo source sheet and pickers. Composed always; the sheet is shown while [show] is true. [onLoading] reports a picker that is
     * still reading the file; [onPicked] receives the single chosen image.
     */
    val photoPicker: @Composable (show: Boolean, onDismiss: () -> Unit, onLoading: (Boolean) -> Unit, onPicked: (ProfileAvatarImage) -> Unit) -> Unit,
)

/** "Редактировать профиль". Caller owns navigation. The form lives in the host's ViewModel store, so it survives rotation. */
interface EditProfileEntry {
    @Composable
    fun Content(onBack: () -> Unit, onOpenAccountDeletion: () -> Unit, modifier: Modifier)
}

/**
 * The account deletion confirmation. Opening it never requests deletion by itself; the user sends the e-mail with a button.
 * [onSessionEnded] is called once when the session that was active ends while the screen is open (the app signs out of the platform and
 * opens the login).
 */
interface AccountDeletionEntry {
    @Composable
    fun Content(onClose: () -> Unit, onSessionEnded: () -> Unit, modifier: Modifier)
}
