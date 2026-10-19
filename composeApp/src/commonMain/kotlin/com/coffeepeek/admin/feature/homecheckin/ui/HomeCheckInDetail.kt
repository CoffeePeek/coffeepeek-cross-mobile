package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.admin.feature.catalog.api.methodLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.ui.component.AppButton
import com.coffeepeek.admin.ui.component.ReviewPhotoStrip
import com.coffeepeek.admin.ui.component.PhotoViewerImages
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import com.coffeepeek.feature.homecheckin.domain.HomeCheckIn
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInVisibility
import com.coffeepeek.feature.homecheckin.domain.HomeCheckInPhoto

@Composable
internal fun HomeDetail(
    entry: HomeCheckIn,
    vm: HomeCheckInViewModel,
    modifier: Modifier,
    notice: String? = null,
    onPhotoClick: (PhotoViewerImages, Int) -> Unit,
) = HomeDetailContent(entry, modifier, notice, vm::edit, vm::confirmDelete, onPhotoClick)

@Composable
internal fun HomeDetailContent(
    entry: HomeCheckIn,
    modifier: Modifier = Modifier,
    notice: String? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (PhotoViewerImages, Int) -> Unit,
) {
    val photos = entry.photos.filter { it.url.isNotBlank() }
    LazyColumn(
        modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(key = "heading") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(CpIcons.Coffee, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Кофе дома", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    entry.visibilityLabel()?.let { label ->
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(if (entry.visibility == HomeCheckInVisibility.Private) CpIcons.Lock else CpIcons.Globe,
                                    null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Text(entry.beanName?.takeIf(String::isNotBlank) ?: "Домашнее приготовление",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val date = entry.preparedOn.split('-').reversed().joinToString(".")
                val time = utcIsoToLocalDateTime(entry.preparedAtUtc).substringAfterLast(' ', "")
                Text(listOf(date, time).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        notice?.let { message -> item(key = "notice") {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
                Text(message, Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.bodyMedium)
            }
        } }
        item(key = "recipe") {
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center) {
                            Icon(CpIcons.Coffee, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Способ приготовления", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(entry.brewMethodCode?.methodLabel() ?: "Не указан",
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    entry.drinkName?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (entry.doseGrams != null || entry.brewDurationSeconds != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            entry.doseGrams?.let { RecipeMetric("Кофе на приготовление", homeDoseLabel(it)) }
                            entry.brewDurationSeconds?.let { RecipeMetric("Время заваривания", homeDurationLabel(it)) }
                        }
                    }
                }
            }
        }
        entry.tasteScore?.let { score -> item(key = "rating") {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
                Text("Оценка вкуса", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "Оценка вкуса $score из 5" }) {
                    repeat(5) { index ->
                        Icon(if (index < score) CpIcons.StarFilled else CpIcons.StarOutline, null,
                            Modifier.size(20.dp), tint = if (index < score) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant)
                    }
                    Text("$score/5", Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
        } }
        if (photos.isNotEmpty()) item(key = "photos") {
            ReviewPhotoStrip(photos.map { it.detailUrl }, thumbnailUrls = photos.map { it.thumbnailUrl },
                singlePhotoAspectRatio = 16f / 10f,
                onPhotoClick = { _, index -> onPhotoClick(homePhotoViewerImages(photos), index) })
        }
        entry.text?.takeIf(String::isNotBlank)?.let { note -> item(key = "note") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Впечатления", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(note, style = MaterialTheme.typography.bodyLarge)
            }
        } }
        item(key = "actions") {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppButton("Редактировать", onClick = onEdit)
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(CpIcons.Delete, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Удалить запись")
                }
            }
        }
    }
}

internal fun homePhotoViewerImages(photos: List<HomeCheckInPhoto>) =
    PhotoViewerImages(photos.map { it.detailUrl }, photos.map { it.url })

@Composable
private fun RecipeMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

internal fun homeDoseLabel(value: String): String =
    (if ('.' in value) value.trimEnd('0').trimEnd('.') else value).replace('.', ',') + " г"

internal fun homeDurationLabel(seconds: Int): String = buildList {
    if (seconds >= 3600) add("${seconds / 3600} ч")
    if (seconds / 60 % 60 > 0) add("${seconds / 60 % 60} мин")
    if (seconds % 60 > 0 || seconds == 0) add("${seconds % 60} с")
}.joinToString(" ")
