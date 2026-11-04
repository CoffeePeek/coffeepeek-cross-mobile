package com.coffeepeek.feature.shelf.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.shelf.api.ShelfProfileEntry
import com.coffeepeek.feature.shelf.domain.repository.ShelfProfileRepository
import com.coffeepeek.feature.shelf.impl.ui.PublicShelfProfileViewModel
import com.coffeepeek.feature.shelf.impl.ui.ShelfProfileVisibilityViewModel
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfProfilePreviewScreen
import com.coffeepeek.feature.shelf.impl.ui.compose.ShelfProfileVisibilityScreen
import kotlin.uuid.Uuid

fun createShelfProfileEntry(repository: ShelfProfileRepository): ShelfProfileEntry = ShelfProfileEntryImpl(repository)

private class ShelfProfileEntryImpl(private val repository: ShelfProfileRepository) : ShelfProfileEntry {
    private val instanceKey = Uuid.random().toString()

    @Composable
    override fun Visibility(onSignIn: () -> Unit, modifier: Modifier) {
        key(this) {
            val model = viewModel(key = "shelf-profile-visibility:$instanceKey") { ShelfProfileVisibilityViewModel(repository) }
            ShelfProfileVisibilityScreen(model, onSignIn, modifier)
        }
    }

    @Composable
    override fun Preview(profileSlug: String, onOpenCoffee: (String) -> Unit, modifier: Modifier) {
        key(this, profileSlug) {
            val model = viewModel(key = "shelf-profile-public:$instanceKey:$profileSlug") { PublicShelfProfileViewModel(repository, profileSlug) }
            ShelfProfilePreviewScreen(model, repository, onOpenCoffee, modifier)
        }
    }
}
