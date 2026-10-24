package com.coffeepeek.feature.checkinhistory.impl.ui.data

import androidx.compose.runtime.Composable
import com.coffeepeek.feature.checkinhistory.domain.model.HistoryMonth
import com.coffeepeek.feature.checkinhistory.impl.resources.Res
import com.coffeepeek.feature.checkinhistory.impl.resources.history_date_title
import com.coffeepeek.feature.checkinhistory.impl.resources.history_month_title
import com.coffeepeek.feature.checkinhistory.impl.resources.history_months
import com.coffeepeek.feature.checkinhistory.impl.resources.history_months_genitive
import com.coffeepeek.feature.checkinhistory.impl.resources.history_count_label
import com.coffeepeek.feature.checkinhistory.impl.resources.history_visit_forms
import com.coffeepeek.feature.checkinhistory.impl.resources.history_shop_forms
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import com.coffeepeek.feature.checkinhistory.impl.resources.history_date_without_year

@Composable
internal fun historyMonthTitle(month: HistoryMonth): String = stringResource(Res.string.history_month_title,
    stringArrayResource(Res.array.history_months)[month.month - 1], month.year)

@Composable
internal fun historyDateTitle(date: String, includeYear: Boolean = true): String {
    val month = HistoryMonth.fromDate(date) ?: return date
    val name = stringArrayResource(Res.array.history_months_genitive)[month.month - 1]
    return if (includeYear) stringResource(Res.string.history_date_title, date.takeLast(2).toInt(), name, month.year)
        else stringResource(Res.string.history_date_without_year, date.takeLast(2).toInt(), name)
}

@Composable
internal fun historyVisitCountLabel(count: Long): String = stringResource(Res.string.history_count_label,
    count, stringArrayResource(Res.array.history_visit_forms)[historyCountForm((count % 100).toInt())])

@Composable
internal fun historyShopCountLabel(count: Int): String = stringResource(Res.string.history_count_label,
    count, stringArrayResource(Res.array.history_shop_forms)[historyCountForm(count)])
