package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.coffeepeek.core.designsystem.component.CoffeePeekFilledButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.theme.Theme
import com.coffeepeek.admin.ui.icons.CpIcons
import androidx.compose.material3.Icon as MaterialIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekIconButton as MaterialIconButton
object Buttons {


    @Composable
    fun Common(
        text: String,
        onclick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Button(
            onClick = onclick,
            modifier = modifier.height(CpDimens.buttonHeight),
            shape = RoundedCornerShape(percent = 50),
            colors = ButtonDefaults.buttonColors().copy(
                containerColor = CpColor.Primary
            )
        ) { Text(text) }
    }

    @Composable
    fun IconButton(
        imageVector: ImageVector,
        onClick: () -> Unit,
        backgroundColor: Color = Color.Transparent,
        enabled: Boolean = true,
        modifier: Modifier = Modifier
    ) {
        MaterialIconButton(
            onClick = onClick,
            enabled = enabled,
            shape = Theme.shape,
            colors = IconButtonDefaults.iconButtonColors(containerColor = backgroundColor),
            modifier = modifier.size(CpDimens.buttonHeight),
        ) {
            MaterialIcon(
                imageVector = imageVector,
                contentDescription = null
            )
        }
    }

    @Composable
    fun BackButton(
        onClick: () -> Unit
    ){
        CpCircularBackButton(onClick = onClick)
    }


}