package com.coffeepeek.feature.settings.impl.ui

import androidx.lifecycle.viewModelScope
import com.coffeepeek.core.presentation.MviViewModel
import com.coffeepeek.feature.settings.api.CityPort
import com.coffeepeek.feature.settings.api.SettingsAccountPort
import com.coffeepeek.feature.settings.api.ThemePort
import com.coffeepeek.feature.settings.api.UpdateCheckPort
import com.coffeepeek.feature.settings.domain.SettingsCity
import com.coffeepeek.feature.settings.domain.UpdateCheckOutcome
import com.coffeepeek.feature.settings.impl.ui.model.SettingsAction
import com.coffeepeek.feature.settings.impl.ui.model.SettingsEvent
import com.coffeepeek.feature.settings.impl.ui.model.SettingsState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The tab's read-only summary: signed-in flag, theme, selected city name and the update row. Like the legacy tab it loads the city catalog when
 * it is created (which also resolves the saved city), and loads it again when the selection points at a city it does not know yet, for example
 * after the city screen got the catalog on a retry.
 */
internal class SettingsViewModel(
    private val account: SettingsAccountPort,
    private val theme: ThemePort,
    private val city: CityPort,
    private val update: UpdateCheckPort,
    private val minimumCheckMillis: Long = MINIMUM_CHECK_MILLIS,
) : MviViewModel<SettingsState, SettingsAction, SettingsEvent>(SettingsState(account.isSignedIn(), theme.current())) {
    private var cities: List<SettingsCity> = emptyList()
    private var selectedId: String? = null
    private var loading = false

    init {
        viewModelScope.launch { account.observeSignedIn().collect { signedIn -> updateState { copy(signedIn = signedIn) } } }
        viewModelScope.launch { theme.observe().collect { mode -> updateState { copy(theme = mode) } } }
        viewModelScope.launch { update.observe().collect { state -> updateState { copy(update = state) } } }
        viewModelScope.launch {
            city.observeSelectedCityId().collect { id ->
                selectedId = id
                publishCity()
                if (id != null && cities.none { it.id == id }) loadCities()
            }
        }
        viewModelScope.launch { loadCities() }
    }

    override suspend fun handleActionInternal(action: SettingsAction) {
        when (action) {
            SettingsAction.CheckForUpdates -> checkForUpdates()
        }
    }

    private suspend fun checkForUpdates() {
        if (currentState.updateBusy) return
        updateState { copy(manualUpdateCheck = true) }
        try {
            // A check can finish in a few milliseconds; hold the loader so the tap visibly does something.
            val minimum = viewModelScope.async { delay(minimumCheckMillis) }
            // Cancellation must pass through: leaving the screen is not a failed check and must not report one.
            val outcome = try {
                update.check()
            } catch (cancelled: CancellationException) {
                minimum.cancel()
                throw cancelled
            } catch (_: Exception) {
                UpdateCheckOutcome.Failed
            }
            minimum.await()
            when (outcome) {
                UpdateCheckOutcome.UpToDate -> sendEvent(SettingsEvent.UpToDate)
                UpdateCheckOutcome.Failed -> sendEvent(SettingsEvent.UpdateCheckFailed)
                UpdateCheckOutcome.Available -> Unit
                UpdateCheckOutcome.Disabled -> Unit
            }
        } finally {
            updateState { copy(manualUpdateCheck = false) }
        }
    }

    private suspend fun loadCities() {
        if (loading) return
        loading = true
        try {
            city.loadCities().onSuccess { list -> cities = list; publishCity() }
        } finally {
            loading = false
        }
    }

    private fun publishCity() {
        val name = cities.firstOrNull { it.id == selectedId }?.name
        updateState { copy(cityName = name) }
    }
}

private const val MINIMUM_CHECK_MILLIS = 700L
