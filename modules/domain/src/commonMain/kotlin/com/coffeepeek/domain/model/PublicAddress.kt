package com.coffeepeek.domain.model

data class PublicAddress(
    val slug: String,
    val canonicalPath: String,
    val revision: Int,
    val isAlias: Boolean,
)
