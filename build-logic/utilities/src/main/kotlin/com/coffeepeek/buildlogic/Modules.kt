package com.coffeepeek.buildlogic

/** Actual Gradle paths only; do not register speculative feature modules. */
object Modules {
    const val composeApp = ":composeApp"

    val legacy = Legacy
    val core = Core
    val feature = Feature

    object Feature {
        val appUpdate = AppUpdate
        object AppUpdate {
            const val api = ":feature:app-update:api"
            const val domain = ":feature:app-update:domain"
            const val data = ":feature:app-update:data"
        }

        const val notifications = ":features:notifications"
        val authorBlocking = AuthorBlocking
        object AuthorBlocking {
            const val api = ":feature:author-blocking:api"
            const val domain = ":feature:author-blocking:domain"
            const val data = ":feature:author-blocking:data"
            const val impl = ":feature:author-blocking:impl"
        }

        val favorites = Favorites
        val shopReport = ShopReport
        object Favorites {
            const val api = ":feature:favorites:api"
            const val domain = ":feature:favorites:domain"
            const val data = ":feature:favorites:data"
            const val impl = ":feature:favorites:impl"
        }
        object ShopReport {
            const val api = ":feature:shop-report:api"
            const val domain = ":feature:shop-report:domain"
            const val data = ":feature:shop-report:data"
            const val impl = ":feature:shop-report:impl"
        }

        val shop = Shop
        val roaster = Roaster
        object Roaster {
            const val api = ":feature:roaster:api"
            const val domain = ":feature:roaster:domain"
            const val data = ":feature:roaster:data"
            const val impl = ":feature:roaster:impl"
        }

        val profile = Profile
        object Profile {
            const val api = ":feature:profile:api"
            const val domain = ":feature:profile:domain"
            const val data = ":feature:profile:data"
            const val impl = ":feature:profile:impl"
        }

        val settings = Settings
        object Settings {
            const val api = ":feature:settings:api"
            const val domain = ":feature:settings:domain"
            const val impl = ":feature:settings:impl"
        }

        val coffee = Coffee
        object Coffee {
            const val api = ":feature:coffee:api"
            const val domain = ":feature:coffee:domain"
            const val data = ":feature:coffee:data"
            const val impl = ":feature:coffee:impl"
        }

        object Shop {
            const val api = ":feature:shop:api"
            const val domain = ":feature:shop:domain"
            const val data = ":feature:shop:data"
            const val impl = ":feature:shop:impl"
        }

        val checkInHistory = CheckInHistory
        val homePreparation = HomePreparation
        object HomePreparation {
            const val api = ":feature:home-check-in:api"
            const val domain = ":feature:home-check-in:domain"
            const val data = ":feature:home-check-in:data"
            const val impl = ":feature:home-check-in:impl"
        }

        object CheckInHistory {
            const val api = ":feature:check-in-history:api"
            const val domain = ":feature:check-in-history:domain"
            const val data = ":feature:check-in-history:data"
            const val impl = ":feature:check-in-history:impl"
        }
    }

    object Legacy {
        const val domain = ":modules:domain"
        const val data = ":modules:data"
        const val network = ":modules:network"
        const val room = ":modules:room"
        const val homeCheckIn = ":modules:home-checkin"
    }

    object Core {
        const val coroutines = ":core:coroutines"
        const val network = ":core:network"
        const val database = ":core:database"
        const val designSystem = ":core:design-system"
        const val presentation = ":core:presentation"
        const val session = ":core:session"
    }

    val all: List<String> = listOf(
        composeApp,
        legacy.domain,
        legacy.data,
        legacy.network,
        legacy.room,
        legacy.homeCheckIn,
        core.coroutines,
        core.network,
        core.database,
        core.designSystem,
        core.presentation,
        core.session,
        feature.favorites.api,
        feature.favorites.domain,
        feature.favorites.data,
        feature.favorites.impl,
        feature.shopReport.api,
        feature.shopReport.domain,
        feature.shopReport.data,
        feature.shopReport.impl,
        feature.shop.api,
        feature.shop.domain,
        feature.shop.data,
        feature.shop.impl,
        feature.roaster.domain,
        feature.roaster.api,
        feature.roaster.data,
        feature.roaster.impl,
        feature.profile.domain,
        feature.profile.api,
        feature.profile.data,
        feature.profile.impl,
        feature.settings.domain,
        feature.settings.api,
        feature.settings.impl,
        feature.coffee.domain,
        feature.coffee.data,
        feature.coffee.api,
        feature.coffee.impl,
        feature.checkInHistory.domain,
        feature.checkInHistory.api,
        feature.checkInHistory.data,
        feature.checkInHistory.impl,
        feature.homePreparation.domain,
        feature.homePreparation.api,
        feature.homePreparation.data,
        feature.homePreparation.impl,
        feature.appUpdate.domain,
        feature.appUpdate.api,
        feature.appUpdate.data,
        feature.authorBlocking.domain,
        feature.authorBlocking.api,
        feature.authorBlocking.data,
        feature.authorBlocking.impl,
        feature.notifications,
    )
}

/** DSL alias: implementation(project(module.core.network)). */
val module: Modules = Modules
