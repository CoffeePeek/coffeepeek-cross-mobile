package com.coffeepeek.admin.di.profile

import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.screen.contributions.ContributionKind
import com.coffeepeek.feature.profile.api.ProfileContribution

internal fun openProfileContributions(kind: ProfileContribution) {
    val destination = when (kind) {
        ProfileContribution.Changes -> ContributionKind.Changes
        ProfileContribution.Shops -> ContributionKind.Shops
        ProfileContribution.Roasters -> ContributionKind.Roasters
    }
    Navigator.navigate(Navigator.Screen.MyContributions(destination.name))
}
