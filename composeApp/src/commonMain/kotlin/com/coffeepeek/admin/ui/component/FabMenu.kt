package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekFloatingActionButton as FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens

object FabMenu {

    data class Menu(
        val icon: ImageVector,
    )


    @Composable
    operator fun invoke(
        items: List<Menu>,
        buttonSize: Dp = 48.dp,
        padding: Dp = 1.dp,
        onClick: (Menu) -> Unit = {},
        modifier: Modifier = Modifier
    ) {
        Row(
            modifier =  modifier
        ) {
            items.forEachIndexed { i, menu ->
                val isStart = i == 0
                val isEnd = i == items.lastIndex
                FloatingActionButton(
                    onClick = { onClick(menu) },
                    shape = RoundedCornerShape(
                        topStart = if (isStart) 18.dp else CpDimens.radiusSm,
                        bottomStart = if (isStart) 18.dp else CpDimens.radiusSm,
                        topEnd = if (isEnd) 18.dp else CpDimens.radiusSm,
                        bottomEnd = if (isEnd) 18.dp else CpDimens.radiusSm
                    ),
                    containerColor = CpColor.Success,
                    contentColor = CpColor.LightSurface,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
                    modifier = Modifier.padding(padding)
                        .size(buttonSize)
                ) {
                    Icon(
                        imageVector = menu.icon,
                        contentDescription = null
                    )
                }
            }
        }
    }

}