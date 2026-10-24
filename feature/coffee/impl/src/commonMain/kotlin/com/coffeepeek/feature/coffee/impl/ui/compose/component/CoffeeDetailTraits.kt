package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.layout.Arrangement
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.coffee.impl.resources.Res
import com.coffeepeek.feature.coffee.impl.resources.coffee_acidity
import com.coffeepeek.feature.coffee.impl.resources.coffee_profile
import com.coffeepeek.feature.coffee.impl.resources.coffee_roast
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadgeColors
import com.coffeepeek.core.designsystem.component.CoffeePeekInfoItem
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme

@Composable
internal fun CoffeeDetailTraits(roast: String?, profile: String?, acidity: String?) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
    ) {
        roast?.let { Trait(it, stringResource(Res.string.coffee_roast), CoffeePeekIcons.Fire) }
        profile?.let { Trait(it, stringResource(Res.string.coffee_profile), CoffeePeekIcons.Sparkle) }
        acidity?.let { Trait(it, stringResource(Res.string.coffee_acidity), CoffeePeekIcons.Drop) }
    }
}

@Composable
private fun Trait(value: String, caption: String, icon: ImageVector) {
    CoffeePeekInfoItem(
        value = value,
        caption = caption,
        modifier = Modifier.width(168.dp * LocalDensity.current.fontScale),
        leading = {
            CoffeePeekIconBadge(
                icon = icon,
                colors = CoffeePeekIconBadgeColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface),
            )
        },
    )
}

@Preview @Composable private fun CoffeeDetailTraitsPreview() = CoffeePeekTheme {
    Surface { CoffeeDetailTraits("Средняя", "Ягоды", "Высокая") }
}
