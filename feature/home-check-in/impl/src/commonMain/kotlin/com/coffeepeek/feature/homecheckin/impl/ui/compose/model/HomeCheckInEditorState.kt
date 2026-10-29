package com.coffeepeek.feature.homecheckin.impl.ui.compose.model

import com.coffeepeek.feature.homecheckin.domain.model.*
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInBeanOption
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInBrewMethod
import com.coffeepeek.feature.homecheckin.impl.ui.data.HomeCheckInShelfBag

internal data class HomeCheckInEditorState(
    val draft: HomePreparationInput = HomePreparationInput(preparedAt = ""),
    val entry: HomePreparation? = null,
    val isReady: Boolean = false,
    val isLoading: Boolean = false,
    val loadIssue: HomePreparationIssue? = null,
    val isSaving: Boolean = false,
    val saveIssue: HomePreparationIssue? = null,
    val failedOperation: HomePreparationOperation? = null,
    val fieldErrors: Map<HomePreparationField, HomePreparationIssue> = emptyMap(),
    val methods: List<HomeCheckInBrewMethod> = emptyList(),
    val methodsLoading: Boolean = false,
    val methodsIssue: HomePreparationIssue? = null,
    val isBeanPickerOpen: Boolean = false,
    val beanQuery: String = "",
    val beanSelectionName: String? = null,
    val beans: List<HomeCheckInBeanOption> = emptyList(),
    val beansLoading: Boolean = false,
    val beansIssue: HomePreparationIssue? = null,
    val shelfBags: List<HomeCheckInShelfBag> = emptyList(),
    /** The chosen bag's card for the "show the bag" switch; null hides the switch (no bag, or a bag without a card). */
    val shelfBagCardUrl: String? = null,
    val isClosed: Boolean = false,
    val isAccessRevoked: Boolean = false,
)
