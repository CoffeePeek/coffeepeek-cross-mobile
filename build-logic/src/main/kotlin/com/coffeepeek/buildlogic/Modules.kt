package com.coffeepeek.buildlogic

/** Actual Gradle paths only; do not register speculative feature modules. */
object Modules {
    const val composeApp = ":composeApp"

    val legacy = Legacy
    val core = Core
    val features = Features

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
    }

    object Features {
        const val homeCheckIn = ":features:home-checkin"
        const val authorBlocking = ":features:author-blocking"
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
        features.homeCheckIn,
        features.authorBlocking,
    )
}

/** DSL alias: implementation(project(module.core.network)). */
val module: Modules = Modules
