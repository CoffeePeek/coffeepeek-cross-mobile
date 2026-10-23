package com.coffeepeek.feature.contributions.impl.change

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.contributions.api.ContributionCatalogs
import com.coffeepeek.feature.contributions.api.ContributionsAccount
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal data class SuggestChangeState(
    /** Null until the first answer about the account; a guest cannot suggest a change. */
    val signedIn: Boolean? = null,
    val isLoading: Boolean = true,
    /** The shop's name, or null when it could not be read: the sections are still offered. */
    val shopTitle: String? = null,
)

internal sealed interface SuggestChangeAction
internal sealed interface SuggestChangeEvent

/** The shop's name over the list of sections. A change of account drops the answer of the previous one. */
internal class SuggestChangeViewModel(
    private val shopSlug: String,
    private val catalogs: ContributionCatalogs,
    private val account: ContributionsAccount,
) : MviViewModel<SuggestChangeState, SuggestChangeAction, SuggestChangeEvent>(SuggestChangeState()) {
    private var generation = 0L

    init {
        viewModelScope.launch {
            account.accountKey.distinctUntilChanged().collect { accountKey ->
                generation++
                if (accountKey == null) updateState { SuggestChangeState(signedIn = false, isLoading = false) }
                else { updateState { SuggestChangeState(signedIn = true) }; loadTitle() }
            }
        }
    }

    override suspend fun handleActionInternal(action: SuggestChangeAction) = Unit

    private fun loadTitle() {
        val mine = generation
        viewModelScope.launch {
            val title = catalogs.shop(shopSlug).getOrNull()?.title?.takeIf(String::isNotBlank)
            if (mine == generation) updateState { copy(isLoading = false, shopTitle = title) }
        }
    }
}
