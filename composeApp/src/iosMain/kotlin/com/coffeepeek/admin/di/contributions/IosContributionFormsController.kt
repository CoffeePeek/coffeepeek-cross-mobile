@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.coffeepeek.admin.di.contributions

import androidx.compose.runtime.*
import com.coffeepeek.admin.di.media.IosCatalogueMedia
import com.coffeepeek.admin.locator.Constants
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.media.ImageUrlFetcher
import com.coffeepeek.core.session.SessionReader
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.repository.ContributionsRepository
import com.coffeepeek.feature.contributions.impl.NativeContributionFormKind
import com.coffeepeek.feature.contributions.impl.NativeContributionFormSession
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

@Composable
internal fun iosContributionFormsContent(
    factory: (IosContributionsRoute, String) -> UIViewController,
    language: () -> String,
): (Navigator.Screen) -> (() -> UIViewController)? {
    val repository = koinInject<ContributionsRepository>()
    val catalogs = koinInject<ContributionCatalogs>()
    val sessions = koinInject<SessionReader>()
    val images = koinInject<ImageUrlFetcher>()
    val media = remember(images) { IosCatalogueMedia(images, Constants.BASE_URL) }
    return iosContributionFormsContent(factory, language, repository, catalogs, sessions, media)
}

internal fun iosContributionFormsContent(factory: (IosContributionsRoute, String) -> UIViewController, language: () -> String,
    repository: ContributionsRepository, catalogs: ContributionCatalogs, sessions: SessionReader, media: IosCatalogueMedia,
): (Navigator.Screen) -> (() -> UIViewController)? {
    val dependencies = NativeContributionForms(repository, catalogs, sessions, media)
    return { screen -> screen.nativeFormArgs()?.let { args -> { factory(dependencies.create(args), language()) } } }
}

internal data class NativeFormArgs(val kind: NativeContributionFormKind, val slug: String = "",
    val section: String = "", val requestId: String = "")

internal fun Navigator.Screen.nativeFormArgs(): NativeFormArgs? = when (this) {
    Navigator.Screen.AddShop -> NativeFormArgs(NativeContributionFormKind.AddShop)
    Navigator.Screen.AddRoaster -> NativeFormArgs(NativeContributionFormKind.AddRoaster)
    is Navigator.Screen.SuggestShopChange -> NativeFormArgs(NativeContributionFormKind.Suggest, shopId)
    is Navigator.Screen.ShopChangeEditor -> NativeFormArgs(NativeContributionFormKind.Editor, shopId, section, requestId)
    else -> null
}

private class NativeContributionForms(
    private val repository: ContributionsRepository, private val catalogs: ContributionCatalogs,
    private val sessions: SessionReader, private val media: IosCatalogueMedia,
) {
    fun create(args: NativeFormArgs): IosContributionForm {
        val section = ChangeSection.entries.firstOrNull { it.name == args.section } ?: ChangeSection.Description
        val session = NativeContributionFormSession(args.kind, repository, catalogs, SessionContributionsAccount(sessions),
            AppContributionLocation(), args.slug, section, args.requestId.ifBlank { null })
        return IosContributionForm(session, args.slug, media)
    }
}

