package com.coffeepeek.feature.settings.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.coffeepeek.feature.settings.api.LanguageEntry
import com.coffeepeek.feature.settings.api.LanguagePort
import com.coffeepeek.feature.settings.impl.ui.LanguageViewModel
import com.coffeepeek.feature.settings.impl.ui.compose.LanguageScreen

/** A single shared leaf; app-owned ports persist/apply the language and the caller owns navigation. */
fun createLanguageEntry(language: LanguagePort): LanguageEntry = object : LanguageEntry {
    @Composable
    override fun Content(onBack: () -> Unit, modifier: Modifier) {
        LanguageScreen(viewModel(key = "settings-language") { LanguageViewModel(language) }, onBack, modifier)
    }
}
