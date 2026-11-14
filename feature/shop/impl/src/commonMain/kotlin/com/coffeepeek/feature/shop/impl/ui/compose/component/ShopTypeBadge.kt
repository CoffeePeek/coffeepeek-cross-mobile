package com.coffeepeek.feature.shop.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekCoffeeIcon
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.feature.shop.domain.model.ShopType
import com.coffeepeek.feature.shop.impl.resources.Res
import com.coffeepeek.feature.shop.impl.resources.shop_type_cafe
import com.coffeepeek.feature.shop.impl.resources.shop_type_coffee_bar
import com.coffeepeek.feature.shop.impl.resources.shop_type_specialty
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun ShopTypeBadge(type: ShopType, modifier: Modifier = Modifier) {
    val label = stringResource(when (type) {
        ShopType.Specialty -> Res.string.shop_type_specialty
        ShopType.CoffeeBar -> Res.string.shop_type_coffee_bar
        ShopType.Cafe -> Res.string.shop_type_cafe
    })
    Surface(modifier, shape = CircleShape, color = CoffeePeekColors.DarkSurface, contentColor = Color.White) {
        Row(Modifier.padding(horizontal = CoffeePeekDimens.spacing2, vertical = CoffeePeekDimens.spacing1),
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
            verticalAlignment = Alignment.CenterVertically) {
            CoffeePeekCoffeeIcon(null, Modifier.size(14.dp), tint = CoffeePeekColors.Primary)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Preview @Composable private fun ShopTypeBadgeLightPreview() = CoffeePeekTheme(darkTheme = false) {
    ShopTypeBadge(ShopType.Specialty)
}

@Preview @Composable private fun ShopTypeBadgeDarkPreview() = CoffeePeekTheme(darkTheme = true) {
    ShopTypeBadge(ShopType.Cafe)
}
