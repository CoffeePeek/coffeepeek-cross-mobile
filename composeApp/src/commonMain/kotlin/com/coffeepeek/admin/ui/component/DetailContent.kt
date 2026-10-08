package com.coffeepeek.admin.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.icons.CpIcons

@Composable
internal fun OutlinedContentCard(
    contentPadding: PaddingValues = PaddingValues(CpDimens.spacing4),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CpDimens.radius2xl),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
}

@Composable
internal fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
internal fun RoasterLinkRow(name: String, photoUrl: String?, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() })
            .padding(CpDimens.spacing4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing5),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape)) {
            if (!photoUrl.isNullOrBlank()) CoffeeShopImage(
                imageUrl = photoUrl,
                contentDescription = "Фото обжарщика $name",
                contentScale = ContentScale.Crop,
                placeholderLabelSize = 7.sp,
                modifier = Modifier.fillMaxSize(),
            ) else CoffeeShopPlaceholderImage(labelSize = 7.sp, contentDescription = "Фото обжарщика $name отсутствует")
        }
        Text(
            name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onClick != null) Icon(
            CpIcons.ChevronRight,
            contentDescription = "Открыть обжарщика $name",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
