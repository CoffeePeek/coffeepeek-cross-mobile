package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.modifier.coffeePeekLiquidGlass
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun CoffeePeekMapControlButton(icon: CoffeePeekGlassIcon, onClick: () -> Unit, contentDescription: String,
    modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (LocalCoffeePeekNativeControlsPresenter.current?.MapControlButton(icon, onClick, contentDescription, modifier) == true) return
    Box(modifier.size(CoffeePeekDimens.buttonHeight).coffeePeekLiquidGlass(CircleShape, hazeState = null)
        .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable private fun MapControlPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    CoffeePeekMapControlButton(CoffeePeekGlassIcon.Location, {}, "Моё местоположение") { Icon(CoffeePeekIcons.Navigation, null) }
}
@Preview @Composable private fun CoffeePeekMapControlLightPreview() = MapControlPreview(false)
@Preview @Composable private fun CoffeePeekMapControlDarkPreview() = MapControlPreview(true)
