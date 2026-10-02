package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CpDimens
import com.coffeepeek.feature.shop.domain.model.ShopCoffeeDetails
import com.coffeepeek.feature.shop.domain.model.ShopRoaster
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_beans
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_equipment
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_open_roaster
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_roaster_photo
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_roaster_photo_missing
import com.coffeepeek.feature.shop.impl.resources.shop_coffee_roasters
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopCoffeeDetailsSection(
    coffee: ShopCoffeeDetails,
    onOpenRoaster: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (coffee.beans.isEmpty() && coffee.roasters.isEmpty() && coffee.equipment.isEmpty()) return
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
            if (coffee.roasters.isNotEmpty()) {
                Text(stringResource(Res.string.shop_coffee_roasters), style = MaterialTheme.typography.titleMedium)
                coffee.roasters.forEach { roaster -> RoasterRow(roaster, onOpenRoaster) }
            }
            if (coffee.beans.isNotEmpty()) {
                CoffeeItemGroup(stringResource(Res.string.shop_coffee_beans), coffee.beans)
            }
            if (coffee.equipment.isNotEmpty()) {
                CoffeeItemGroup(stringResource(Res.string.shop_coffee_equipment), coffee.equipment)
            }
        }
    }
}

@Composable
private fun RoasterRow(roaster: ShopRoaster, onOpenRoaster: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            val photoUrl = roaster.photoUrl
            if (photoUrl == null) {
                RoasterPlaceholder(roaster.name)
            } else {
                KamelImage(
                    resource = { asyncPainterResource(photoUrl) },
                    contentDescription = stringResource(Res.string.shop_coffee_roaster_photo, roaster.name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp),
                    onLoading = { RoasterPlaceholder(roaster.name) },
                    onFailure = { RoasterPlaceholder(roaster.name) },
                )
            }
        }
        if (roaster.id.isNotBlank()) {
            val description = stringResource(Res.string.shop_coffee_open_roaster, roaster.name)
            TextButton(onClick = { onOpenRoaster(roaster.id) },
                modifier = Modifier.weight(1f).semantics { contentDescription = description }) {
                Text(roaster.name)
            }
        } else {
            Text(roaster.name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RoasterPlaceholder(name: String) {
    val description = stringResource(Res.string.shop_coffee_roaster_photo_missing)
    Surface(Modifier.size(40.dp).semantics { contentDescription = description },
        color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoffeeItemGroup(title: String, items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2),
            verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
            items.forEach { item ->
                Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small) {
                    Text(item, Modifier.padding(CpDimens.spacing2),
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
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
