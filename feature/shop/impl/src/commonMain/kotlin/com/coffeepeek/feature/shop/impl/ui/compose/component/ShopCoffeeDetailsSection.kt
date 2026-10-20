package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import com.coffeepeek.feature.shop.api.ShopExposure
import com.coffeepeek.feature.shop.impl.ui.data.shopExposure
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopRoaster
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_beans
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_equipment
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_roasters
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCoffeeDetailsSection(
    coffee: ShopCoffeeDetails,
    onOpenRoaster: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (coffee.beans.isEmpty() && coffee.roasters.isEmpty() && coffee.equipment.isEmpty()) return
    val section = if (coffee.roasters.isNotEmpty()) modifier.shopExposure(ShopExposure.Roasters) else modifier
    Column(section.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        if (coffee.roasters.isNotEmpty()) {
            ShopSectionTitle(stringResource(Res.string.shop_coffee_roasters))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(CoffeePeekDimens.radius2xl),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                coffee.roasters.forEachIndexed { index, roaster ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = CoffeePeekDimens.spacing4),
                        color = MaterialTheme.colorScheme.outline)
                    ShopRoasterRow(roaster, onOpenRoaster)
                }
            }
        }
        ShopCoffeeCatalogGroup(stringResource(Res.string.shop_coffee_beans), coffee.beans)
        ShopCoffeeCatalogGroup(stringResource(Res.string.shop_coffee_equipment), coffee.equipment)
    }
}

@Preview @Composable private fun ShopCoffeeDetailsSectionLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopCoffeeDetailsSection(previewCoffee(), onOpenRoaster = {})
}
@Preview @Composable private fun ShopCoffeeDetailsSectionDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopCoffeeDetailsSection(previewCoffee(), onOpenRoaster = {})
}
private fun previewCoffee() = ShopCoffeeDetails(
    beans = listOf("Эфиопия", "Бразилия"),
    roasters = listOf(ShopRoaster("roaster-1", "Местная обжарка", null)),
    equipment = listOf("V60", "Аэропресс"),
)
