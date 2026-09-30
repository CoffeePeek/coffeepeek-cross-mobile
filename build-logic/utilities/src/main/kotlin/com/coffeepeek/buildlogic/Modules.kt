package com.coffeepeek.buildlogic

/** Actual Gradle paths only; do not register speculative feature modules. */
object Modules {
    const val composeApp = ":composeApp"

    val legacy = Legacy
    val core = Core
    val feature = Feature

    object Feature {
        val favorites = Favorites
        object Favorites {
            const val api = ":feature:favorites:api"
            const val domain = ":feature:favorites:domain"
            const val data = ":feature:favorites:data"
            const val impl = ":feature:favorites:impl"
        }
    }

    object Legacy {
        const val domain = ":modules:domain"
        const val data = ":modules:data"
        const val network = ":modules:network"
        const val room = ":modules:room"
    }

    object Core {
        const val coroutines = ":core:coroutines"
        const val network = ":core:network"
        const val database = ":core:database"
        const val designSystem = ":core:design-system"
        const val presentation = ":core:presentation"
    }

    val all: List<String> = listOf(
        composeApp,
        legacy.domain,
        legacy.data,
        legacy.network,
        legacy.room,
        core.coroutines,
        core.network,
        core.database,
        core.designSystem,
        core.presentation,
        feature.favorites.api,
        feature.favorites.domain,
        feature.favorites.data,
        feature.favorites.impl,
    )
}

/** DSL alias: implementation(project(module.core.network)). */
val module: Modules = Modules
