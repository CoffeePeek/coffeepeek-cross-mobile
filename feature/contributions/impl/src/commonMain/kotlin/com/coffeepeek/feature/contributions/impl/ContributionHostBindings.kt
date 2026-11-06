package com.coffeepeek.feature.contributions.impl

import com.coffeepeek.feature.contributions.api.*

internal fun AddShopHost.forAccount(account: ContributionsAccount) = AddShopHost(
    locationPermission = locationPermission, mapCanvas = mapCanvas,
    photoPicker = AccountContributionPhotoPicker(photoPicker, account), feedback = feedback,
)

internal fun AddRoasterHost.forAccount(account: ContributionsAccount) = AddRoasterHost(
    photoPicker = AccountContributionPhotoPicker(photoPicker, account), feedback = feedback,
)

internal fun ChangeEditorHost.forAccount(account: ContributionsAccount) = ChangeEditorHost(
    photoPicker = AccountContributionPhotoPicker(photoPicker, account), feedback = feedback,
)
