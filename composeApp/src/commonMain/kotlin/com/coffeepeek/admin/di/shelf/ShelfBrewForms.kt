package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.di.homecheckin.FeatureCheckInCreateFlowRenderer
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean

/** "Заварить чашку" opens the migrated Home check-in form with the bag's coffee. */
internal fun shelfBrewForm(checkIns: FeatureCheckInCreateFlowRenderer): ShelfBrewForm = { coffee, onClose ->
    checkIns.Home(HomeCheckInBean(coffee.slug, coffee.name, coffee.shelfBagId), onDismiss = { onClose(false) }, onSaved = { onClose(true) })
}
