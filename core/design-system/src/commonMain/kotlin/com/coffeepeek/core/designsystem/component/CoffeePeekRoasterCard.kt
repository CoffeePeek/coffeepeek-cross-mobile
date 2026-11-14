package com.coffeepeek.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * The roaster card of the catalogue, the favorites and the discovery carousel. It knows no feature models and loads no images:
 * the logo and the favorite control are slots, the texts come from the caller (so they are localized there).
 *
 * When the parent supplies an exact height (a carousel next to a coffee shop card), the footer with the two counters is measured first,
 * so the counters stay visible and the description shrinks. A null [onClick] makes a read-only card.
 *
 * @param hasLogo the logo box is white around [logo] when true, otherwise it shows a neutral placeholder and [logo] is not drawn.
 * @param favorite placed at the top end, next to the name; the name leaves room for it.
 */
@Composable
fun CoffeePeekRoasterCard(
    name: String,
    description: String?,
    shopsCount: Int,
    shopsLabel: String,
    productsCount: Int,
    productsLabel: String,
    modifier: Modifier = Modifier,
    hasLogo: Boolean = false,
    onClick: (() -> Unit)? = null,
    logo: @Composable BoxScope.() -> Unit = {},
    favorite: (@Composable () -> Unit)? = null,
    showCounters: Boolean = true,
) {
    val summary = description?.takeIf(String::isNotBlank)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val exactHeight = if (constraints.hasBoundedHeight && constraints.minHeight == constraints.maxHeight) maxHeight else null
        Card(
            modifier = Modifier.fillMaxWidth().clickable(enabled = onClick != null, role = Role.Button, onClick = { onClick?.invoke() }),
            shape = RoundedCornerShape(CoffeePeekDimens.radiusXl),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Layout(
                modifier = Modifier.fillMaxWidth()
                    .then(if (exactHeight != null) Modifier.height(exactHeight) else Modifier)
                    .padding(CoffeePeekDimens.spacing3),
                content = {
                    RoasterHeader(name, summary, hasLogo, logo, favorite)
                    if (showCounters) RoasterCounters(shopsCount, shopsLabel, productsCount, productsLabel)
                    else Spacer(Modifier.height(0.dp))
                },
            ) { measurables, constraints ->
                // Reserve the footer before measuring the flexible description.
                val footer = measurables[1].measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                val gap = if (showCounters) CoffeePeekDimens.spacing2.roundToPx() else 0
                val bodyHeight = if (constraints.hasBoundedHeight) (constraints.maxHeight - footer.height - gap).coerceAtLeast(0) else Constraints.Infinity
                val body = measurables[0].measure(constraints.copy(minHeight = 0, maxHeight = bodyHeight))
                val height = constraints.constrainHeight(body.height + gap + footer.height)
                layout(constraints.maxWidth, height) {
                    body.placeRelative(0, 0)
                    footer.placeRelative(0, height - footer.height)
                }
            }
        }
    }
}

@Composable
private fun RoasterHeader(name: String, summary: String?, hasLogo: Boolean, logo: @Composable BoxScope.() -> Unit, favorite: (@Composable () -> Unit)?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
                .background(if (hasLogo) Color.White else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (hasLogo) logo() else Icon(CoffeePeekIcons.Factory, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name, Modifier.fillMaxWidth().padding(end = 36.dp), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (summary != null) Text(
                    summary, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis,
                )
            }
            if (favorite != null) Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-12).dp)) { favorite() }
        }
    }
}

@Composable
private fun RoasterCounters(shopsCount: Int, shopsLabel: String, productsCount: Int, productsLabel: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RoasterCounter(CoffeePeekIcons.Coffee, shopsLabel, shopsCount, Modifier.weight(1f))
        VerticalDivider(Modifier.height(20.dp), color = MaterialTheme.colorScheme.outlineVariant)
        RoasterCounter(CoffeePeekIcons.CoffeeBean, productsLabel, productsCount, Modifier.weight(1f))
    }
}

@Composable
private fun RoasterCounter(icon: ImageVector, label: String, count: Int, modifier: Modifier) {
    Row(
        modifier.semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(coffeePeekCountLabel(count), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            label, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A counter as the cards show it: a dash for none (or a nonsense negative value). */
fun coffeePeekCountLabel(count: Int): String = if (count > 0) count.toString() else "—"

@Composable
private fun RoasterCardPreviewContent(darkTheme: Boolean) = CoffeePeekTheme(darkTheme = darkTheme) {
    Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        CoffeePeekRoasterCard("Sandbox Roaster", "Локальный обжарщик для тестирования", 7, "Кофейни", 21, "Каталог", onClick = {}, favorite = {
            CoffeePeekLikeButton(true, "В избранном", {}, modifier = Modifier.size(48.dp))
        })
        CoffeePeekRoasterCard("Очень длинное название обжарщика для проверки обрезки строки", null, 0, "Кофейни", 0, "Каталог")
        CoffeePeekRoasterCard("Мой обжарщик", "Описание профиля", 0, "", 0, "", showCounters = false, onClick = {})
    }
}

@Preview @Composable private fun CoffeePeekRoasterCardLightPreview() = RoasterCardPreviewContent(darkTheme = false)
@Preview @Composable private fun CoffeePeekRoasterCardDarkPreview() = RoasterCardPreviewContent(darkTheme = true)
