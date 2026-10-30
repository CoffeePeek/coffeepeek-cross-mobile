package com.coffeepeek.feature.contributions.impl

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.contributions.api.ContributionPhotoPicker
import com.coffeepeek.feature.contributions.api.ContributionPhotoPickerController
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import com.coffeepeek.feature.contributions.domain.model.PendingPhoto

/** Native URI work may finish after the form's account has changed. */
internal class AccountContributionPhotoPicker(
    private val picker: ContributionPhotoPicker,
    private val account: ContributionsAccount,
) : ContributionPhotoPicker {
    @Composable
    override fun rememberController(): ContributionPhotoPickerController {
        val controller = picker.rememberController()
        return ContributionPhotoPickerController(controller.isPreparing, { limit, deliver ->
            delivery(deliver)?.let { controller.pickFromGallery(limit, it) }
        }, { deliver -> delivery(deliver)?.let(controller.takePhoto) })
    }

    internal fun delivery(deliver: (List<PendingPhoto>) -> Unit): ((List<PendingPhoto>) -> Unit)? {
        val owner = account.currentAccountKey() ?: return null
        return { if (account.currentAccountKey() == owner) deliver(it) }
    }
}
