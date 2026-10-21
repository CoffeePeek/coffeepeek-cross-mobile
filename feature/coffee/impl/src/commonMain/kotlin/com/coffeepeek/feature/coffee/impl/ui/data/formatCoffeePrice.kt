package com.coffeepeek.feature.coffee.impl.ui.data

import kotlin.math.round

/** Preserve catalogue decimal formatting independently of the device locale. */
internal fun formatCoffeePrice(price: Double): String {
    val cents = round(price * 100).toLong()
    return if (cents % 100 == 0L) "${cents / 100}"
    else "${cents / 100}.${(cents % 100).toString().padStart(2, '0').trimEnd('0')}"
}
