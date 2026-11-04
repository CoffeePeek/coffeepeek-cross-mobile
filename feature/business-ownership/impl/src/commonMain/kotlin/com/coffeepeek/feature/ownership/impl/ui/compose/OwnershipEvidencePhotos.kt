package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.coffeepeek.core.designsystem.component.*
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.ownership.api.*
import com.coffeepeek.feature.ownership.domain.model.BusinessPhoto
import com.coffeepeek.feature.ownership.impl.resources.*
import com.coffeepeek.feature.ownership.impl.ui.compose.model.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.uuid.Uuid

@Composable
internal fun OwnershipEvidencePhotos(state: OwnershipState, action: (OwnershipAction) -> Unit,
    image: OwnershipImage, picker: OwnershipPhotoPicker) {
    val photos = state.submission.photos
    Text(stringResource(Res.string.ownership_evidence_photos))
    key(state.generation) {
        picker((3 - photos.size).coerceAtLeast(1), { picked ->
            action(OwnershipAction.AddEvidence(state.generation, picked.map {
                BusinessPhoto(Uuid.random().toString(), fileName = it.fileName, contentType = it.contentType,
                    size = it.bytes.size.toLong(), bytes = it.bytes.copyOf())
            }))
        }) { add ->
            CoffeePeekPhotoAttachments(photos, enabled = !state.busy, isLoading = state.busy, maxPhotos = 3,
                onMove = { from, to -> action(OwnershipAction.MoveEvidence(from, to)) }, onRemove = { action(OwnershipAction.RemoveEvidence(it)) }, onAdd = add,
                photo = { photo, modifier, scale -> image(photo.url, photo.bytes, modifier, scale) })
        }
    }
}
@Preview @Composable private fun OwnershipEvidenceLightPreview() = CoffeePeekTheme(darkTheme = false) {
    OwnershipEvidencePhotos(OwnershipState(), {}, { _, _, m, _ -> CoffeePeekImagePlaceholder(m) }, { _, _, c -> c {} })
}
@Preview @Composable private fun OwnershipEvidenceDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    OwnershipEvidencePhotos(OwnershipState(), {}, { _, _, m, _ -> CoffeePeekImagePlaceholder(m) }, { _, _, c -> c {} })
}
