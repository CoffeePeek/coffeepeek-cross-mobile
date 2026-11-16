package com.coffeepeek.feature.coffee.impl

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.coffeepeek.feature.coffee.api.CoffeeDetailAnalytics
import com.coffeepeek.feature.coffee.domain.model.Coffee
import com.coffeepeek.feature.coffee.domain.model.CoffeeOffer
import com.coffeepeek.feature.coffee.domain.repository.CoffeeRepository
import com.coffeepeek.feature.coffee.impl.ui.CoffeeDetailViewModel
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeDetailAction
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeDetailEvent
import com.coffeepeek.feature.coffee.impl.ui.compose.model.CoffeeDetailState
import com.coffeepeek.feature.coffee.impl.ui.data.formatCoffeePrice
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun createIosCoffeeDetailSession(slug: String, repository: CoffeeRepository, analytics: CoffeeDetailAnalytics): IosCoffeeDetailSession =
    IosCoffeeDetailSession(CoffeeDetailViewModel(slug, repository, analytics))

class CoffeeDetailSnapshot internal constructor(state: CoffeeDetailState) {
    private val labels = state.filterGroups.associate { group -> group.code to group.options.associate { it.code to it.name } }

    val details = state.details
    val isLoading = state.isLoading
    val loadFailed = state.loadFailed
    val similar = state.similar
    val similarLoading = state.similarLoading
    val similarFailed = state.similarFailed
    val arePhotosOpen = state.arePhotosOpen
    val isDescriptionExpanded = state.isDescriptionExpanded

    fun label(group: String, code: String?): String? = labels[group]?.get(code)

    fun tastes(coffee: Coffee): List<String> = coffee.tasteCodes.mapNotNull(labels["taste"].orEmpty()::get)

    fun offerPrice(offer: CoffeeOffer): String? = offer.price?.let(::formatCoffeePrice)
}

/** Owns the existing ViewModel for the lifetime of one pushed iOS destination. */
class IosCoffeeDetailSession internal constructor(private val viewModel: CoffeeDetailViewModel) {
    private val store = ViewModelStore().apply { put("coffee-detail", viewModel) }

    fun currentState(): CoffeeDetailSnapshot = CoffeeDetailSnapshot(viewModel.state.value)

    fun observeState(onState: (CoffeeDetailSnapshot) -> Unit): Job = viewModel.viewModelScope.launch {
        viewModel.state.collect { onState(CoffeeDetailSnapshot(it)) }
    }

    fun observeEvents(
        onBack: () -> Unit,
        onCoffee: (String) -> Unit,
        onRoaster: (String) -> Unit,
        onSeller: (String) -> Unit,
        onShare: (String, String) -> Unit,
    ): Job = viewModel.viewModelScope.launch {
        viewModel.events.collect { event ->
            when (event) {
                CoffeeDetailEvent.Back -> onBack()
                is CoffeeDetailEvent.OpenCoffee -> onCoffee(event.slug)
                is CoffeeDetailEvent.OpenRoaster -> onRoaster(event.slug)
                is CoffeeDetailEvent.OpenSeller -> onSeller(event.url)
                is CoffeeDetailEvent.Share -> onShare(event.name, event.canonicalPath)
            }
        }
    }

    fun retry() = viewModel.onAction(CoffeeDetailAction.Retry)
    fun retrySimilar() = viewModel.onAction(CoffeeDetailAction.RetrySimilar)
    fun back() = viewModel.onAction(CoffeeDetailAction.Back)
    fun openPhotos() = viewModel.onAction(CoffeeDetailAction.OpenPhotos)
    fun closePhotos() = viewModel.onAction(CoffeeDetailAction.ClosePhotos)
    fun toggleDescription() = viewModel.onAction(CoffeeDetailAction.ToggleDescription)
    fun openRoaster() = viewModel.onAction(CoffeeDetailAction.OpenRoaster)
    fun openSimilar(slug: String) = viewModel.onAction(CoffeeDetailAction.OpenSimilar(slug))
    fun openSeller(index: Int) = viewModel.onAction(CoffeeDetailAction.OpenSeller(index))
    fun share() = viewModel.onAction(CoffeeDetailAction.Share)
    fun close() = store.clear()
}
