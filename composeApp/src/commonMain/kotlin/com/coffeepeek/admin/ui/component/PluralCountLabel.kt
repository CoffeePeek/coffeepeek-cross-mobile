package com.coffeepeek.admin.ui.component

/** The three grammatical forms are prepared by the localized presentation resource adapter. */
internal fun pluralCountLabel(count: Int, forms: List<String>): String = "$count ${pluralCountWord(count, forms)}"

internal fun pluralCountWord(count: Int, forms: List<String>): String = when {
    count % 100 in 11..14 -> forms[2]
    count % 10 == 1 -> forms[0]
    count % 10 in 2..4 -> forms[1]
    else -> forms[2]
}
