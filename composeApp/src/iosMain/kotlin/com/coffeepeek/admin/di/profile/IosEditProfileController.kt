package com.coffeepeek.admin.di.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.utils.IosNativePhotoPicker
import com.coffeepeek.admin.utils.PhotoPickerController
import com.coffeepeek.admin.utils.PickedImage
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.feature.profile.api.*
import com.coffeepeek.feature.profile.impl.NativeEditProfileSession
import com.coffeepeek.feature.profile.impl.createNativeEditProfileSession
import com.coffeepeek.feature.settings.api.LanguagePort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

private lateinit var editProfileControllerFactory: (IosEditProfile) -> UIViewController
internal fun configureNativeEditProfile(factory: (IosEditProfile) -> UIViewController) { editProfileControllerFactory = factory }

/** The feature owns the draft/store; the native controller owns its picker and observations. */
internal class IosNativeEditProfileFactory(private val accounts: ProfileAccountPort, private val edit: ProfileEditPort,
    private val language: LanguagePort, private val media: IosCatalogueMedia,
) {
    fun controller(): UIViewController {
        val session = createNativeEditProfileSession(accounts, edit)
        var photos: PhotoPickerController? = null
        val source = IosEditProfile(session, language, media, { photos }, Navigator::popBack,
            { Navigator.navigate(Navigator.Screen.DeleteAccountPending) })
        val picker = IosNativePhotoPicker(source.scope, source::photoLoading, source::presentationHost)
        photos = photoController(session, picker)
        source.bindDisposal {
            photos = null
            picker.close()
            session.close()
        }
        observeAccount(source, session)
        observeSaved(source, session)
        return editProfileControllerFactory(source)
    }

    private fun photoController(session: NativeEditProfileSession, picker: IosNativePhotoPicker) = PhotoPickerController(
        { picker.show(false, 1) { pickedAvatar(session, it) } }, { picker.show(true, 1) { pickedAvatar(session, it) } },
    )
    private fun pickedAvatar(session: NativeEditProfileSession, images: List<PickedImage>) {
        images.firstOrNull()?.let { session.avatarPicked(ProfileAvatarImage(it.fileName, it.contentType, it.bytes)) }
    }
    private fun observeAccount(source: IosEditProfile, session: NativeEditProfileSession) {
        source.scope.launch { combine(accounts.observeActiveAccount(), accounts.observeCacheScope()) { _, _ -> session.isCurrent() }
            .distinctUntilChanged().catch {
                if (it is CancellationException) throw it
                source.revoke()
            }.collect { if (!it) source.revoke() } }
    }
    private fun observeSaved(source: IosEditProfile, session: NativeEditProfileSession) {
        source.scope.launch { source.screenVisible.collectLatest { shown ->
            if (shown) session.saved.collect { if (session.isCurrent()) Navigator.popBack() }
        } }
    }
}

@Composable
internal fun iosEditProfileContent(): () -> UIViewController {
    val accounts = koinInject<ProfileAccountPort>()
    val edit = koinInject<ProfileEditPort>()
    val language = koinInject<LanguagePort>()
    val fetcher = koinInject<ImageUrlFetcher>()
    val factory = remember(accounts, edit, language, fetcher) {
        IosNativeEditProfileFactory(accounts, edit, language, IosCatalogueMedia(fetcher, Constants.BASE_URL))
    }
    return factory::controller
}
