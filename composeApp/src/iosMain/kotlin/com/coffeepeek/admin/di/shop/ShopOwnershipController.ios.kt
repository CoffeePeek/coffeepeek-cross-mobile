@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.coffeepeek.admin.di.shop

import androidx.compose.ui.window.ComposeUIViewController
import com.coffeepeek.admin.AppContent
import com.coffeepeek.feature.ownership.api.BusinessOwnershipEntry
import platform.UIKit.UIViewController

/** Temporary owner-only host; the shared Business checkpoint replaces this with iosOwnershipActionsController. */
internal fun shopOwnershipController(ownership: BusinessOwnershipEntry?, id: String, slug: String,
    allowed: () -> Boolean, onSuggest: () -> Unit, onOwnerEdit: () -> Unit): UIViewController =
    ComposeUIViewController(configure = { opaque = false }) {
        AppContent {
            ownership?.ProfileActions("CoffeeShop", id, slug,
                onSuggest = { if (allowed()) onSuggest() }, onOwnerEdit = { if (allowed()) onOwnerEdit() },
                favorite = {}, share = {})
        }
    }
