package com.coffeepeek.admin.sandbox.backend

internal data class SandboxCollectionMembershipState(
    val collectionIds: Set<String>,
    val isFavorite: Boolean,
)
