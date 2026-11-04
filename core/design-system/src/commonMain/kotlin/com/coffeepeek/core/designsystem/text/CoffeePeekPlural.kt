package com.coffeepeek.core.designsystem.text

import kotlin.math.absoluteValue

/**
 * Picks the Russian form for [count]: 1, 21 → [one]; 2–4, 22 → [few]; 0, 5–20, 25 → [many].
 *
 * The app's texts are Russian on every device, while `<plurals>` choose a form by the device language's
 * rules: on an English phone everything but 1 falls to "other" ("5 пачки", "21 дня"). Keep the three forms
 * as separate strings and choose between them here instead.
 */
fun <T> coffeePeekPluralForm(count: Int, one: T, few: T, many: T): T {
    val n = count.absoluteValue
    return when {
        n % 100 in 11..14 -> many
        n % 10 == 1 -> one
        n % 10 in 2..4 -> few
        else -> many
    }
}
