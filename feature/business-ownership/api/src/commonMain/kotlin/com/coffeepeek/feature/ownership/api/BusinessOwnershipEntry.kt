package com.coffeepeek.feature.ownership.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

/** Platform input/rendering only. HTTP and domain implementation types never cross this entry. */
data class OwnershipPickedPhoto(val fileName: String, val contentType: String, val bytes: ByteArray)
typealias OwnershipImage = @Composable (url: String?, bytes: ByteArray?, modifier: Modifier, scale: ContentScale) -> Unit
typealias OwnershipPhotoPicker = @Composable (limit: Int, onPicked: (List<OwnershipPickedPhoto>) -> Unit, content: @Composable (onAdd: () -> Unit) -> Unit) -> Unit
typealias OwnershipBackHandler = @Composable (onBack: () -> Unit) -> Unit
interface BusinessOwnershipEntry {
    @Composable
    fun Content(onBack: () -> Unit, onLogin: () -> Unit, initialKind: String? = null, initialSlug: String? = null, initialId: String? = null)
    @Composable fun ProfileChanges(): Long
    @Composable fun Session(): BusinessSessionState
    @Composable fun AccountSwitch()
    @Composable fun Tab(onCheckIns: () -> Unit, checkIns: @Composable () -> Unit)
    @Composable fun ProfileActions(kind: String, id: String, slug: String, onSuggest: () -> Unit, onOwnerEdit: () -> Unit,
        favorite: @Composable () -> Unit, share: @Composable () -> Unit)
}

data class BusinessSessionState(val businessMode: Boolean = false, val hasBusinesses: Boolean = false, val ownedIdentities: Set<String> = emptySet())
val LocalBusinessOwnership = staticCompositionLocalOf<BusinessOwnershipEntry?> { null }
