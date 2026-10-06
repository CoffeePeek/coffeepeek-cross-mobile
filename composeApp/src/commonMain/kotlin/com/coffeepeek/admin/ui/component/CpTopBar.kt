package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.icons.CpIcons

/** Single top bar used across screens: center-aligned title + circular back button in the corner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = { Navigator.popBack() },
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = { onBack?.let { CpCircularBackButton(onClick = it) } },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
        modifier = modifier.padding(horizontal = CpDimens.spacing4),
    )
}

/** Circular Liquid Glass back button — used on plain bars and over hero images. */
@Composable
fun CpCircularBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    PlatformGlassIconButton(
        icon = GlassControlIcon.Back,
        onClick = onClick,
        contentDescription = "Назад",
        modifier = modifier,
    ) {
        Icon(
            imageVector = CpIcons.ChevronLeft,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
