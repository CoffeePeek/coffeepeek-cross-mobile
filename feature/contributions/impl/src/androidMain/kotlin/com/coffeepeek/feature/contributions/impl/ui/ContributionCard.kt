package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.coffeepeek.core.designsystem.component.CoffeePeekCard
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.api.ContributionKind
import com.coffeepeek.feature.contributions.domain.model.ChangeSection
import com.coffeepeek.feature.contributions.domain.model.ModerationStatus
import com.coffeepeek.feature.contributions.impl.ChangeSummary
import com.coffeepeek.feature.contributions.impl.R
import com.coffeepeek.feature.contributions.impl.list.ContributionItem
import com.coffeepeek.feature.contributions.impl.list.ContributionNote
import com.coffeepeek.feature.contributions.impl.list.ContributionTarget

@Composable
internal fun ContributionCard(
    item: ContributionItem,
    kind: ContributionKind,
    status: ModerationStatus,
    onOpen: ((ContributionTarget) -> Unit),
    modifier: Modifier = Modifier,
) {
    val target = item.target
    CoffeePeekCard(
        modifier = modifier,
        onClick = target?.let { { onOpen(it) } },
        shape = RoundedCornerShape(CoffeePeekDimens.radiusLg),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = 0.dp,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(Modifier.padding(CoffeePeekDimens.spacing4), verticalAlignment = Alignment.Top) {
            val visual = kind.visual(item.section)
            CoffeePeekIconBadge(visual.icon, visual.colors)
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            CardText(item, kind, Modifier.weight(1f))
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            StatusPill(status)
        }
        item.rejectedReason?.takeIf(String::isNotBlank)?.let { RejectedReason(it) }
        if (target != null) OpenFooter(target)
    }
}

@Composable
private fun CardText(item: ContributionItem, kind: ContributionKind, modifier: Modifier) {
    val section = item.section
    val title = item.title ?: section?.title().orEmpty()
    val subtitle = item.subtitle ?: section?.let { (item.summary ?: ChangeSummary.Empty).text(it) }
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(CoffeePeekDimens.spacing1))
            Text(
                subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (kind == ContributionKind.Roasters) 3 else 2, overflow = TextOverflow.Ellipsis,
            )
        }
        val meta = when {
            item.note == ContributionNote.WaitingForCatalog -> stringResource(R.string.contributions_note_waiting) to CoffeePeekIcons.Info
            item.createdAtUtc != null -> localDateTime(item.createdAtUtc) to CoffeePeekIcons.Time
            else -> null
        }
        meta?.let { (text, icon) -> Meta(text, icon) }
    }
}

@Composable
private fun Meta(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Spacer(Modifier.height(CoffeePeekDimens.spacing2))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OpenFooter(target: ContributionTarget) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = CoffeePeekDimens.spacing4, vertical = CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = if (target is ContributionTarget.Request) R.string.contributions_open_request else R.string.contributions_open_catalog
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        Icon(CoffeePeekIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RejectedReason(reason: String) {
    Row(
        Modifier.padding(horizontal = CoffeePeekDimens.spacing4).padding(bottom = CoffeePeekDimens.spacing4).fillMaxWidth()
            .clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f))
            .padding(CoffeePeekDimens.spacing3),
        verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        Icon(CoffeePeekIcons.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
        Column {
            Text(stringResource(R.string.contributions_rejected_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Text(reason, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun StatusPill(status: ModerationStatus, modifier: Modifier = Modifier) {
    val color = status.color()
    Row(
        modifier.clip(RoundedCornerShape(percent = 50)).background(color.copy(alpha = 0.12f))
            .padding(horizontal = CoffeePeekDimens.spacing2, vertical = CoffeePeekDimens.spacing1),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1),
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(percent = 50)).background(color))
        Text(status.shortTitle(), style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
    }
}

@PreviewLightDark @Composable private fun ContributionCardPreview() = CoffeePeekTheme {
    androidx.compose.material3.Surface {
        Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            ContributionCard(ContributionItem("a", title = "Sandbox Cafe", subtitle = "Тестовая улица, 1", target = ContributionTarget.Shop("cafe")), ContributionKind.Shops, ModerationStatus.Approved, {})
            ContributionCard(ContributionItem("b", title = "Sandbox Point", subtitle = "Тестовая улица, 7", note = ContributionNote.WaitingForCatalog), ContributionKind.Shops, ModerationStatus.Approved, {})
            ContributionCard(
                ContributionItem("c", section = ChangeSection.Photos, summary = ChangeSummary.Photos(2, 1), createdAtUtc = "2026-10-21T07:00:00Z",
                    rejectedReason = "Фото не подходят", target = ContributionTarget.Request("c")),
                ContributionKind.Changes, ModerationStatus.Rejected, {},
            )
        }
    }
}
