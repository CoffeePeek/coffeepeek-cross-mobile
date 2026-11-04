package com.coffeepeek.feature.ownership.impl.ui.compose

import androidx.compose.runtime.Composable

@Composable
internal expect fun OwnershipProfileMenu(
    editLabel: String?, representativeLabel: String, actionsLabel: String,
    onEdit: () -> Unit, onRepresentative: () -> Unit,
)
