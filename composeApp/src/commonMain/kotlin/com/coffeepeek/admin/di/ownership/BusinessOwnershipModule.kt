package com.coffeepeek.admin.di.ownership

import androidx.compose.runtime.Composable
import com.coffeepeek.admin.ui.component.NativeNavigationBar
import com.coffeepeek.admin.utils.rememberPhotoPicker
import com.coffeepeek.core.network.ApiClients
import com.coffeepeek.core.designsystem.component.CoffeePeekImage
import com.coffeepeek.core.network.requestResult
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.domain.model.PendingPhotoUpload
import com.coffeepeek.domain.repository.PhotoRepository
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.data.repository.createBusinessOwnershipRepository
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.domain.repository.BusinessOwnershipRepository
import com.coffeepeek.feature.ownership.impl.createBusinessOwnershipEntry
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

internal fun businessOwnershipModule() = module {
    single<OwnershipAccount> { SessionOwnershipAccount(get()) }
    single<BusinessPhotoUploader> { OwnershipMediaUploader(get(), get()) }
    single<BusinessOwnershipRepository> { createBusinessOwnershipRepository(get<ApiClients>().client, get(), get()) }
    single<BusinessOwnershipEntry> {
        createBusinessOwnershipEntry(get(), get(), ownershipImage, ownershipPicker,
            backHandler = { back -> NativeNavigationBar("", back, customBack = true, action = null) },
            onEdit = { kind, id -> com.coffeepeek.admin.ui.Navigator.navigate(com.coffeepeek.admin.ui.Navigator.Screen.MyBusiness(kind = kind, businessId = id)) },
            onLogin = { com.coffeepeek.admin.ui.Navigator.navigate(com.coffeepeek.admin.ui.Navigator.Screen.Auth) },
            onOpenBusiness = { kind, slug ->
                com.coffeepeek.admin.ui.Navigator.navigate(if (kind == "CoffeeShop")
                    com.coffeepeek.admin.ui.Navigator.Screen.ShopDetail(slug)
                    else com.coffeepeek.admin.ui.Navigator.Screen.RoasterDetail(slug))
            })
    }
}
private class SessionOwnershipAccount(private val sessions: SessionReader) : OwnershipAccount {
    override fun current(): String? = sessions.peekSession()?.takeIf(sessions::isActiveSession)?.userId
    override fun observe() = sessions.observeSession().map { it?.takeIf(sessions::isActiveSession)?.userId }
}
private class OwnershipMediaUploader(private val photos: PhotoRepository, private val account: OwnershipAccount) : BusinessPhotoUploader {
    override suspend fun upload(kind: BusinessKind, photos: List<BusinessPhoto>): Result<List<BusinessPhoto>> = requestResult {
        val owner = account.current() ?: throw OwnershipFailure(OwnershipProblem.SignedOut)
        val inputs = photos.map { PendingPhotoUpload(it.fileName, it.contentType, checkNotNull(it.bytes)) }
        val uploaded = if (kind == BusinessKind.CoffeeShop) this.photos.uploadShopPhotos(inputs).getOrThrow()
            else this.photos.uploadRoasterPhotos(inputs).getOrThrow()
        if (owner != account.current()) throw OwnershipFailure(OwnershipProblem.SignedOut)
        check(uploaded.size == photos.size)
        photos.zip(uploaded).map { (source, saved) -> source.copy(bytes = null, storageKey = saved.storageKey, size = saved.size) }
    }
}
private val ownershipImage: OwnershipImage = { url, bytes, modifier, scale ->
    CoffeePeekImage(data = bytes ?: url.orEmpty(), modifier = modifier, contentScale = scale)
}
private val ownershipPicker: OwnershipPhotoPicker = { limit, onPicked, content ->
    val controller = rememberPhotoPicker(maxSelection = limit, onPhotosPicked = { photos ->
        onPicked(photos.map { OwnershipPickedPhoto(it.fileName, it.contentType, it.bytes) })
    })
    content(controller.pickFromGallery)
}
