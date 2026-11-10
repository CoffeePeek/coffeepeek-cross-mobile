package com.coffeepeek.feature.contributions.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** «Отправленные кофейни / обжарщики / Мои правки». The caller owns navigation and the ViewModel store. */
interface ContributionsListEntry {
    @Composable
    fun Content(
        kind: ContributionKind,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (slug: String) -> Unit,
        onOpenRequest: (id: String) -> Unit,
        modifier: Modifier,
    )
}

/** The details of one sent change request. */
interface ChangeRequestEntry {
    @Composable
    fun Content(
        requestId: String,
        onBack: () -> Unit,
        onSignIn: () -> Unit,
        onOpenShop: (slug: String) -> Unit,
        modifier: Modifier,
    )
}
