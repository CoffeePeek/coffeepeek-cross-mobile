package com.coffeepeek.admin.di.shelf

import com.coffeepeek.admin.di.homecheckin.AndroidCheckInCreateFlowRenderer
import com.coffeepeek.feature.homecheckin.api.HomeCheckInBean

/** "Заварить чашку" opens the migrated Home check-in form with the bag's coffee. */
internal fun androidShelfBrewForm(checkIns: AndroidCheckInCreateFlowRenderer): ShelfBrewForm = { coffee, onClose ->
    checkIns.Home(HomeCheckInBean(coffee.slug, coffee.name, coffee.shelfBagId), onDismiss = { onClose(false) }, onSaved = { onClose(true) })
}
