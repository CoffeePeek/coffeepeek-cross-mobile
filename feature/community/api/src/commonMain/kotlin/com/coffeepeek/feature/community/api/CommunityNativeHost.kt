package com.coffeepeek.feature.community.api

import kotlinx.coroutines.flow.Flow

/** Platform services for the native feature owner; none of these callbacks renders shared UI. */
class CommunityNativeHost(
    val languages: Flow<String>,
    val displayDate: (String) -> String,
    val brewMethodLabel: (String) -> String?,
    val share: (String) -> Unit,
    val message: suspend (String) -> Unit,
)

class CommunityNavigation(
    val onSignIn: () -> Unit,
    val onOpenAuthor: (slug: String, name: String, avatarUrl: String?) -> Unit,
    val onOpenShop: (String) -> Unit,
    val onEditHome: (String) -> Unit,
    val onOpenPost: (String) -> Unit,
    val onOpenCoffee: (String) -> Unit,
)

class CommunityPostNavigation(
    val onBack: () -> Unit,
    val onSignIn: () -> Unit,
    val onOpenAuthor: (slug: String, name: String, avatarUrl: String?) -> Unit,
    val onOpenShop: (String) -> Unit,
    val onOpenCoffee: (String) -> Unit,
    val onEditHome: (String) -> Unit = {},
)
