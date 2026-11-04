package com.coffeepeek.feature.collections.api

/** Stable public business identity; callers never depend on collection domain models. */
enum class CollectionTargetKind { CoffeeShop, Roaster }

data class CollectionTarget(val kind: CollectionTargetKind, val slug: String)
