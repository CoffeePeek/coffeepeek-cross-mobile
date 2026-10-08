package com.coffeepeek.admin.feature.coffee.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coffeepeek.admin.feature.coffee.domain.Coffee
import com.coffeepeek.admin.feature.coffee.domain.CoffeeDetails
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilterGroup
import com.coffeepeek.admin.feature.coffee.domain.CoffeeFilters
import com.coffeepeek.admin.feature.coffee.domain.CoffeeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class CoffeeDetailUiState(
    val details: CoffeeDetails? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val filterGroups: List<CoffeeFilterGroup> = emptyList(),
    val similar: List<Coffee> = emptyList(),
    val similarLoading: Boolean = false,
    val similarError: Boolean = false,
)

internal class CoffeeDetailViewModel(private val slug: String, private val repository: CoffeeRepository) : ViewModel() {
    private val _state = MutableStateFlow(CoffeeDetailUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var similarJob: Job? = null

    init {
        viewModelScope.launch {
            repository.getFilterGroups().onSuccess { groups -> _state.update { it.copy(filterGroups = groups) } }
        }
        load()
    }

    fun load() {
        loadJob?.cancel()
        similarJob?.cancel()
        _state.update { CoffeeDetailUiState(filterGroups = it.filterGroups) }
        loadJob = viewModelScope.launch {
            repository.getDetails(slug).onSuccess { details ->
                _state.update { it.copy(details = details, isLoading = false) }
                loadSimilar()
            }.onFailure {
                _state.update { it.copy(isLoading = false, error = "Не удалось загрузить кофе") }
            }
        }
    }

    fun loadSimilar() {
        val coffee = state.value.details?.coffee ?: return
        val tastes = coffee.tasteCodes.toSet()
        if (tastes.isEmpty()) return
        similarJob?.cancel()
        _state.update { it.copy(similarLoading = true, similarError = false) }
        similarJob = viewModelScope.launch {
            repository.search("", CoffeeFilters(mapOf("taste" to tastes)), 1).onSuccess { page ->
                _state.update { it.copy(similar = page.items.filter { item -> item.slug != coffee.slug }.take(6), similarLoading = false) }
            }.onFailure {
                _state.update { it.copy(similarLoading = false, similarError = true) }
            }
        }
    }
}
