package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.domain.model.*
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.uuid.Uuid

@Composable
internal fun BusinessGallery(state: OwnershipState, action: (OwnershipAction) -> Unit, image: OwnershipImage, picker: OwnershipPhotoPicker) {
    val photos = state.draft?.photos.orEmpty()
    Text(stringResource(Res.string.ownership_photos), style = MaterialTheme.typography.titleMedium)
    key(state.generation) {
        picker((10 - photos.size).coerceAtLeast(1), { picked ->
            action(OwnershipAction.AddPhotos(state.generation, picked.map { input ->
                BusinessPhoto(Uuid.random().toString(), fileName = input.fileName, contentType = input.contentType,
                    size = input.bytes.size.toLong(), bytes = input.bytes.copyOf())
            }))
        }) { add ->
            CoffeePeekPhotoAttachments(photos, enabled = !state.busy && !state.reloadRequired, isLoading = state.busy, maxPhotos = 10,
                onMove = { from, to -> action(OwnershipAction.MovePhoto(from, to)) },
                onRemove = { action(OwnershipAction.RemovePhoto(it)) }, onAdd = add,
                photo = { photo, modifier, scale -> image(photo.url, photo.bytes, modifier, scale) })
        }
    }
}
@Preview @Composable private fun BusinessGalleryLightPreview() = CoffeePeekTheme(darkTheme = false) {
    BusinessGallery(OwnershipState(draft = ProfileDraft()), {}, { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) }, { _, _, content -> content {} })
}
@Preview @Composable private fun BusinessGalleryDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    BusinessGallery(OwnershipState(draft = ProfileDraft()), {}, { _, _, modifier, _ -> CoffeePeekImagePlaceholder(modifier) }, { _, _, content -> content {} })
}
