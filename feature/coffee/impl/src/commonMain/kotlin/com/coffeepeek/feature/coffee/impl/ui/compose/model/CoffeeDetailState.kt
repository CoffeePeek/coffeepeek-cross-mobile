package com.coffeepeek.feature.coffee.impl.ui.compose.model

import com.coffeepeek.feature.coffee.domain.model.Coffee
import com.coffeepeek.feature.coffee.domain.model.CoffeeDetails
import com.coffeepeek.feature.coffee.domain.model.CoffeeFilterGroup

internal data class CoffeeDetailState(
    val details: CoffeeDetails? = null,
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val filterGroups: List<CoffeeFilterGroup> = emptyList(),
    val similar: List<Coffee> = emptyList(),
    val similarLoading: Boolean = false,
    val similarFailed: Boolean = false,
    val arePhotosOpen: Boolean = false,
    val isDescriptionExpanded: Boolean = false,
)
