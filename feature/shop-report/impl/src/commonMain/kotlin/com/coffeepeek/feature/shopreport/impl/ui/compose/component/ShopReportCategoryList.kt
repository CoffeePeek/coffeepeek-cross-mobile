package com.coffeepeek.feature.shopreport.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.shopreport.domain.model.ShopIssueCategory
import com.coffeepeek.feature.shopreport.impl.resources.Res
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_incorrect_address
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_incorrect_photos
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_other
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_outdated_menu
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_shop_closed
import com.coffeepeek.feature.shopreport.impl.resources.shop_report_wrong_hours
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopReportCategoryList(selected: ShopIssueCategory?, onSelect: (ShopIssueCategory) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Column(Modifier.selectableGroup()) {
            CATEGORY_ORDER.forEachIndexed { index, category ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = CoffeePeekDimens.spacing4),
                    color = MaterialTheme.colorScheme.outline)
                val label = stringResource(when (category) {
                    ShopIssueCategory.OutdatedMenu -> Res.string.shop_report_outdated_menu
                    ShopIssueCategory.ShopClosed -> Res.string.shop_report_shop_closed
                    ShopIssueCategory.IncorrectAddress -> Res.string.shop_report_incorrect_address
                    ShopIssueCategory.WrongOpeningHours -> Res.string.shop_report_wrong_hours
                    ShopIssueCategory.IncorrectPhotos -> Res.string.shop_report_incorrect_photos
                    ShopIssueCategory.Other -> Res.string.shop_report_other
                })
                ShopReportCategoryOption(label, selected == category, { onSelect(category) }, enabled = enabled)
            }
        }
    }
}

@Preview @Composable private fun ShopReportCategoryListLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopReportCategoryList(ShopIssueCategory.ShopClosed, {})
}
@Preview @Composable private fun ShopReportCategoryListDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopReportCategoryList(null, {})
}

/** Production order of the reasons. */
private val CATEGORY_ORDER = listOf(
    ShopIssueCategory.OutdatedMenu, ShopIssueCategory.ShopClosed, ShopIssueCategory.WrongOpeningHours,
    ShopIssueCategory.IncorrectPhotos, ShopIssueCategory.IncorrectAddress, ShopIssueCategory.Other,
)
