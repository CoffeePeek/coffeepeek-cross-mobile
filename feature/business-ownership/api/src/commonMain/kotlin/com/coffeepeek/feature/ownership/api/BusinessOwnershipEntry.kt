package com.coffeepeek.feature.ownership.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/** Platform input/rendering only. HTTP and domain implementation types never cross this entry. */
data class OwnershipPickedPhoto(val fileName: String, val contentType: String, val bytes: ByteArray)
typealias OwnershipImage = @Composable (url: String?, bytes: ByteArray?, modifier: Modifier, scale: ContentScale) -> Unit
typealias OwnershipPhotoPicker = @Composable (limit: Int, onPicked: (List<OwnershipPickedPhoto>) -> Unit, content: @Composable (onAdd: () -> Unit) -> Unit) -> Unit
typealias OwnershipBackHandler = @Composable (onBack: () -> Unit) -> Unit
interface BusinessOwnershipEntry {
    @Composable
    fun Content(onBack: () -> Unit, onLogin: () -> Unit, initialKind: String? = null, initialSlug: String? = null)
}
