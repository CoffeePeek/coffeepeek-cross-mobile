package com.coffeepeek.feature.shelf.impl.ui.compose.component

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.shelf.domain.model.ShelfGrind
import com.coffeepeek.feature.shelf.domain.model.ShelfIssue
import com.coffeepeek.feature.shelf.impl.resources.*
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfBagItem
import com.coffeepeek.feature.shelf.impl.ui.compose.model.ShelfSummary
import com.coffeepeek.feature.shelf.impl.ui.data.ShelfDay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val months = listOf(
    Res.string.shelf_month_1, Res.string.shelf_month_2, Res.string.shelf_month_3, Res.string.shelf_month_4,
    Res.string.shelf_month_5, Res.string.shelf_month_6, Res.string.shelf_month_7, Res.string.shelf_month_8,
    Res.string.shelf_month_9, Res.string.shelf_month_10, Res.string.shelf_month_11, Res.string.shelf_month_12,
)
private val monthsOf = listOf(
    Res.string.shelf_month_of_1, Res.string.shelf_month_of_2, Res.string.shelf_month_of_3, Res.string.shelf_month_of_4,
    Res.string.shelf_month_of_5, Res.string.shelf_month_of_6, Res.string.shelf_month_of_7, Res.string.shelf_month_of_8,
    Res.string.shelf_month_of_9, Res.string.shelf_month_of_10, Res.string.shelf_month_of_11, Res.string.shelf_month_of_12,
)

@Composable
internal fun ShelfBagItem.title(): String = bag.name?.takeIf(String::isNotBlank) ?: stringResource(Res.string.shelf_bag_untitled)

@Composable
internal fun shelfDaysText(days: Int): String = pluralStringResource(Res.plurals.shelf_days, days, days)

/** Days for the first two weeks, then whole weeks. */
@Composable
internal fun shelfAgeText(days: Int): String =
    if (days < 14) shelfDaysText(days) else pluralStringResource(Res.plurals.shelf_weeks, days / 7, days / 7)

@Composable
internal fun shelfMonthText(year: Int, month: Int, today: ShelfDay): String {
    val name = stringResource(months[month - 1])
    return if (year == today.year) name else stringResource(Res.string.shelf_month_year, name, year)
}

@Composable
internal fun ShelfDay.text(today: ShelfDay): String {
    val month = stringResource(monthsOf[month - 1])
    return if (year == today.year) stringResource(Res.string.shelf_date, day, month)
    else stringResource(Res.string.shelf_date_year, day, month, year)
}

/** "≈ 750 г" below a kilogram, "≈ 1,9 кг" from one. */
@Composable
internal fun shelfWeightText(grams: Int): String {
    if (grams < 1000) return stringResource(Res.string.shelf_weight_g, grams)
    val tenths = (grams + 50) / 100
    val kilograms = if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10},${tenths % 10}"
    return stringResource(Res.string.shelf_weight_kg, kilograms)
}

@Composable
internal fun ShelfSummary.text(): String = listOfNotNull(
    pluralStringResource(Res.plurals.shelf_bags, bags, bags),
    grams?.let { shelfWeightText(it) },
    roasters.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.shelf_roasters, it, it) },
).joinToString(" · ")

/** Weight, grind and whether the bag is a catalog coffee, for the bag sheet. */
@Composable
internal fun ShelfBagItem.traits(): List<String> = listOfNotNull(
    bag.weightGrams?.let { stringResource(Res.string.shelf_weight, it) },
    when (bag.grind) {
        ShelfGrind.Beans -> stringResource(Res.string.shelf_grind_beans)
        ShelfGrind.Ground -> stringResource(Res.string.shelf_grind_ground)
        null -> null
    },
    bag.coffeeProductSlug?.let { stringResource(Res.string.shelf_from_catalog) },
)

/** "На полке 9 дней" or "Добавлена сегодня"; null without an added date. */
@Composable
internal fun ShelfBagItem.shelfAgeText(today: ShelfDay): String? = daysOnShelf(today)?.let { days ->
    if (days == 0 && finishedOn == null) stringResource(Res.string.shelf_added_today)
    else stringResource(Res.string.shelf_on_shelf_for, shelfDaysText(days))
}

@Composable
internal fun ShelfBagItem.roastText(today: ShelfDay): String? = daysSinceRoast(today)?.let { days ->
    if (days == 0) stringResource(Res.string.shelf_roasted_today) else stringResource(Res.string.shelf_roasted_ago, shelfAgeText(days))
}

@Composable
internal fun ShelfIssue.text(): String = stringResource(issueText(this))

private fun issueText(issue: ShelfIssue): StringResource = when (issue) {
    ShelfIssue.Offline, ShelfIssue.Timeout -> Res.string.shelf_error_offline
    ShelfIssue.Stale -> Res.string.shelf_error_stale
    ShelfIssue.NotFound -> Res.string.shelf_error_not_found
    ShelfIssue.SignedOut, ShelfIssue.AccountChanged, ShelfIssue.Unauthorized -> Res.string.shelf_error_signed_out
    else -> Res.string.shelf_error_generic
}
