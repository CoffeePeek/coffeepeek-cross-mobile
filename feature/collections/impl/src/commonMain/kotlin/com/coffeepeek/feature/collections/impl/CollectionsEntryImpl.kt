package com.coffeepeek.feature.collections.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.collections.api.CollectionsEntry
import com.coffeepeek.feature.collections.api.CollectionPhotoDecoder
import com.coffeepeek.feature.collections.domain.repository.CollectionsAccount
import com.coffeepeek.feature.collections.domain.repository.CollectionsRepository
import com.coffeepeek.feature.collections.impl.ui.CollectionsViewModel
import com.coffeepeek.feature.collections.impl.ui.compose.CollectionsCallbacks
import com.coffeepeek.feature.collections.impl.ui.compose.CollectionsScreen
import kotlin.uuid.Uuid

fun createCollectionsEntry(repository: CollectionsRepository, account: CollectionsAccount, decodePhoto: CollectionPhotoDecoder): CollectionsEntry =
    CollectionsEntryImpl(repository, account, decodePhoto)

private class CollectionsEntryImpl(private val repository: CollectionsRepository, private val account: CollectionsAccount, private val decodePhoto: CollectionPhotoDecoder) : CollectionsEntry {
    private val instance = Uuid.random().toString()


    @Composable
    override fun Composer(onBack: () -> Unit, onSignIn: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit, onShareText: (String) -> Unit,
        onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier) {
        Content(null, CollectionsCallbacks(onBack, onSignIn, onOpenShop, onOpenRoaster, onShareText, onShareImages), modifier)
    }

    @Composable
    override fun Public(id: String, onBack: () -> Unit, onOpenShop: (String) -> Unit, onOpenRoaster: (String) -> Unit,
        onShareText: (String) -> Unit, onShareImages: (List<ByteArray>, String) -> Unit, modifier: Modifier) {
        Content(id, CollectionsCallbacks(onBack, {}, onOpenShop, onOpenRoaster, onShareText, onShareImages), modifier)
    }

    @Composable
    private fun Content(id: String?, callbacks: CollectionsCallbacks, modifier: Modifier) {
        val model = viewModel(key = "collections:$instance:${id ?: "compose"}") { CollectionsViewModel(repository, account, id) }
        CollectionsScreen(model, repository, decodePhoto, callbacks, modifier)
    }
}
