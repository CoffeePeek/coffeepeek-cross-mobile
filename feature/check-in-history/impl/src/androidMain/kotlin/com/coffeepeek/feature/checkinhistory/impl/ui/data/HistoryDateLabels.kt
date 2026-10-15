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

@Composable
internal fun historyMonthTitle(month: HistoryMonth): String = stringResource(Res.string.history_month_title,
    stringArrayResource(Res.array.history_months)[month.month - 1], month.year)

@Composable
internal fun historyDateTitle(date: String): String {
    val month = HistoryMonth.fromDate(date) ?: return date
    return stringResource(Res.string.history_date_title, date.takeLast(2).toInt(),
        stringArrayResource(Res.array.history_months_genitive)[month.month - 1], month.year)
}

@Composable
internal fun historyVisitCountLabel(count: Int): String = stringResource(Res.string.history_count_label,
    count, stringArrayResource(Res.array.history_visit_forms)[historyCountForm(count)])

@Composable
internal fun historyShopCountLabel(count: Int): String = stringResource(Res.string.history_count_label,
    count, stringArrayResource(Res.array.history_shop_forms)[historyCountForm(count)])
