package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.clickable
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_back
import com.coffeepeek.feature.contributions.impl.resources.contributions_loading
import com.coffeepeek.feature.contributions.impl.resources.contributions_suggest_heading
import com.coffeepeek.feature.contributions.impl.resources.contributions_suggest_heading_shop
import com.coffeepeek.feature.contributions.impl.resources.contributions_suggest_sign_in_prompt
import com.coffeepeek.feature.contributions.impl.resources.contributions_suggest_title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekGroupSection
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekRowSeparator
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.impl.change.SuggestChangeState
import com.coffeepeek.feature.contributions.impl.change.SuggestChangeViewModel

internal class SuggestCallbacks(val onBack: () -> Unit, val onSignIn: () -> Unit, val onPickSection: (ChangeSection) -> Unit)

/** The legacy list left the equipment out although the editor supports it; the list keeps that. */
private val offered = ChangeSection.entries.filterNot { it == ChangeSection.Equipment }

@Composable
internal fun SuggestChangeScreen(viewModel: SuggestChangeViewModel, callbacks: SuggestCallbacks, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SuggestChangeContent(state, callbacks, modifier)
}

@Composable
internal fun SuggestChangeContent(state: SuggestChangeState, callbacks: SuggestCallbacks, modifier: Modifier = Modifier) {
    val title = stringResource(Res.string.contributions_suggest_title)
    when {
        state.signedIn == false -> FormSignInPrompt(title, stringResource(Res.string.contributions_suggest_sign_in_prompt), callbacks.onBack, callbacks.onSignIn, modifier)
        else -> Scaffold(
            modifier = modifier,
            topBar = { CoffeePeekTopBar(title, stringResource(Res.string.contributions_back), onBack = callbacks.onBack) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            if (state.signedIn == null || state.isLoading) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CoffeePeekLoader(stringResource(Res.string.contributions_loading)) }
            else Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4)) {
                Text(
                    state.shopTitle?.let { stringResource(Res.string.contributions_suggest_heading_shop, it) } ?: stringResource(Res.string.contributions_suggest_heading),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(CoffeePeekDimens.spacing5))
                CoffeePeekGroupSection {
                    offered.forEachIndexed { index, section ->
                        if (index > 0) CoffeePeekRowSeparator()
                        SectionRow(section, callbacks.onPickSection)
                    }
                }
                Spacer(Modifier.height(CoffeePeekDimens.spacing8))
            }
        }
    }
}

@Composable
private fun SectionRow(section: ChangeSection, onPick: (ChangeSection) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button) { onPick(section) }.heightIn(min = 56.dp).padding(horizontal = CoffeePeekDimens.spacing4, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3),
    ) {
        Column(Modifier.weight(1f)) {
            Text(section.title(), style = MaterialTheme.typography.bodyLarge)
            Text(section.hint(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(CoffeePeekIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

private val noCallbacks = SuggestCallbacks({}, {}, {})

@Preview @Composable private fun SuggestChangePreview() = CoffeePeekTheme {
    Surface { SuggestChangeContent(SuggestChangeState(signedIn = true, isLoading = false, shopTitle = "Sandbox Cafe"), noCallbacks) }
}

@Preview @Composable private fun SuggestChangeGuestPreview() = CoffeePeekTheme {
    Surface { SuggestChangeContent(SuggestChangeState(signedIn = false, isLoading = false), noCallbacks) }
}
