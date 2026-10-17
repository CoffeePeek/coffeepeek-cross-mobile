package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

import com.coffeepeek.feature.homecheckin.domain.model.HomePreparation
import com.coffeepeek.feature.homecheckin.domain.model.HomePreparationIssue

internal data class HomeCheckInDetailState(
    val entry: HomePreparation? = null,
    val isLoading: Boolean = false,
    val loadIssue: HomePreparationIssue? = null,
    val isDeleting: Boolean = false,
    val deleteIssue: HomePreparationIssue? = null,
    val showDeleteConfirmation: Boolean = false,
    val isClosed: Boolean = false,
    val isAccessRevoked: Boolean = false,
)
