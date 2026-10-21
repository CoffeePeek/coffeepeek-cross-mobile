package com.coffeepeek.feature.coffee.impl.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.component.CoffeePeekImagePlaceholder
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.coffee.impl.R

@Composable
internal fun CoffeeRoasterRow(name: String, photoUrl: String?, onClick: (() -> Unit)?,
    imageContent: @Composable (String, String, Modifier, ContentScale) -> Unit) {
    CoffeePeekContentCard(contentPadding = PaddingValues(0.dp)) {
        Row(Modifier.fillMaxWidth().clickable(enabled = onClick != null, role = Role.Button,
            onClickLabel = stringResource(R.string.coffee_open_roaster, name), onClick = { onClick?.invoke() })
            .padding(CoffeePeekDimens.spacing4), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing5)) {
            val description = stringResource(R.string.coffee_roaster_photo, name)
            Box(Modifier.size(64.dp).clip(CircleShape)) {
                if (!photoUrl.isNullOrBlank()) imageContent(photoUrl, description, Modifier.fillMaxSize(), ContentScale.Crop)
                else CoffeePeekImagePlaceholder(labelSize = 7.sp, contentDescription = description)
            }
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (onClick != null) Icon(CoffeePeekIcons.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@PreviewLightDark @Composable private fun CoffeeRoasterRowPreview() = CoffeePeekTheme {
    CoffeeRoasterRow("CoffeePeek", null, {}, { _, _, _, _ -> })
}
