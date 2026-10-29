package com.coffeepeek.admin.ui.screen.contributions

import androidx.compose.runtime.Composable

/** Application route adapter; both platforms bind migrated feature entries explicitly. */
interface ContributionsScreenRenderer {
    /** [kind] is a [ContributionKind] name. */
    @Composable
    fun List(kind: ContributionKind)

    @Composable
    fun AddShop()

    @Composable
    fun AddRoaster()

    @Composable
    fun Suggest(shopId: String)

    /** [section] is the existing serialized section name; a blank [requestId] is a new request. */
    @Composable
    fun Editor(shopId: String, section: String, requestId: String)

    @Composable
    fun RequestDetail(requestId: String)
}
