package com.coffeepeek.admin.ui.screen.review

enum class ConsumedDrinkIssue { CustomNameLength, CustomNameOnlyForOther }

/** A named drink must be "other" with a 1..100 character name; any other drink carries no custom name. */
fun consumedDrinkIssue(slug: String?, customName: String?): ConsumedDrinkIssue? = when {
    slug == "other" && customName.orEmpty().trim().length !in 1..100 -> ConsumedDrinkIssue.CustomNameLength
    slug != "other" && customName != null -> ConsumedDrinkIssue.CustomNameOnlyForOther
    else -> null
}
