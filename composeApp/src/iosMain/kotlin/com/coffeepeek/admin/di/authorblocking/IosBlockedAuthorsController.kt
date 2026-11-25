package com.coffeepeek.admin.di.authorblocking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.core.designsystem.resources.LocalCoffeePeekLanguageTag
import com.coffeepeek.feature.authorblocking.api.AuthorBlockingPresenter
import org.koin.compose.koinInject
import platform.UIKit.UIViewController

internal val LocalIosBlockedAuthorsController = staticCompositionLocalOf<
    (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController
> { error("The iOS app must provide a blocked authors controller") }

@Composable
internal fun iosBlockedAuthorsContent(): () -> UIViewController {
    val factory = LocalIosBlockedAuthorsController.current
    val languageTag by rememberUpdatedState(LocalCoffeePeekLanguageTag.current)
    val presenter = koinInject<AuthorBlockingPresenter>()
    return iosBlockedAuthorsContent(factory, { languageTag }, presenter)
}

internal fun iosBlockedAuthorsContent(factory: (IosBlockedAuthors, String, () -> Unit, () -> Unit, () -> Unit) -> UIViewController,
    language: () -> String, presenter: AuthorBlockingPresenter,
): () -> UIViewController = {
        factory(IosBlockedAuthors(presenter), language(), Navigator::popBack,
            { Navigator.navigate(Navigator.Screen.Auth) },
            { Navigator.navigate(Navigator.Screen.Register) })
}
