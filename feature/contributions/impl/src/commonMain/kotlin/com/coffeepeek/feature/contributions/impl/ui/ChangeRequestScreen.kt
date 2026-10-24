package com.coffeepeek.feature.contributions.impl.ui

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.contributions.impl.resources.Res
import com.coffeepeek.feature.contributions.impl.resources.contributions_back
import com.coffeepeek.feature.contributions.impl.resources.contributions_load_failed
import com.coffeepeek.feature.contributions.impl.resources.contributions_loading
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_added
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_description_removed
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_email
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_empty
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_gallery_note
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_instagram
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_menu_items
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_menu_photos
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_not_found
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_open_shop
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_payload
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_phone
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_price
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_rejection
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_removed
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_retained
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_selected_brew_methods
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_selected_equipment
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_selected_roasters
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_selected_tags
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_set_note
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_site
import com.coffeepeek.feature.contributions.impl.resources.contributions_request_title
import com.coffeepeek.feature.contributions.impl.resources.contributions_retry
import com.coffeepeek.feature.contributions.impl.resources.contributions_sign_in
import com.coffeepeek.feature.contributions.impl.resources.contributions_sign_in_prompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coffeepeek.core.designsystem.component.CoffeePeekButton
import com.coffeepeek.core.designsystem.component.CoffeePeekContentCard
import com.coffeepeek.core.designsystem.component.CoffeePeekIconBadge
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekTopBar
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.contributions.domain.model.*
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestAction
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestState
import com.coffeepeek.feature.contributions.impl.request.ChangeRequestViewModel
import com.coffeepeek.feature.contributions.impl.request.RequestError

internal class RequestCallbacks(val onBack: () -> Unit, val onSignIn: () -> Unit, val onOpenShop: (String) -> Unit)

@Composable
internal fun ChangeRequestScreen(viewModel: ChangeRequestViewModel, callbacks: RequestCallbacks, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ChangeRequestContent(state, callbacks, { viewModel.onAction(ChangeRequestAction.Refresh) }, modifier)
}

@Composable
internal fun ChangeRequestContent(state: ChangeRequestState, callbacks: RequestCallbacks, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { CoffeePeekTopBar(stringResource(Res.string.contributions_request_title), stringResource(Res.string.contributions_back), onBack = callbacks.onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val request = state.request
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                state.isLoading -> CoffeePeekLoader(stringResource(Res.string.contributions_loading))
                state.error == RequestError.SignInRequired -> Failure(stringResource(Res.string.contributions_sign_in_prompt), stringResource(Res.string.contributions_sign_in), callbacks.onSignIn)
                request == null -> Failure(failureText(state), stringResource(Res.string.contributions_retry), onRetry)
                else -> RequestBody(request, callbacks.onOpenShop)
            }
        }
    }
}

@Composable
private fun failureText(state: ChangeRequestState) = state.serverMessage ?: stringResource(
    if (state.error == RequestError.NotFound) Res.string.contributions_request_not_found else Res.string.contributions_load_failed,
)

@Composable
private fun Failure(text: String, action: String, onAction: () -> Unit) {
    Column(Modifier.padding(CoffeePeekDimens.spacing4), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(CoffeePeekDimens.spacing3))
        CoffeePeekButton(action, onAction)
    }
}

@Composable
private fun RequestBody(request: ChangeRequest, onOpenShop: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CoffeePeekDimens.spacing4),
        verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing4),
    ) {
        Hero(request)
        request.rejectionReason?.takeIf(String::isNotBlank)?.let { Rejection(it) }
        PayloadCard(request.section, request.payload)
        OutlinedButton(
            onClick = { onOpenShop(request.shopSlug) }, enabled = request.shopSlug.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(CoffeePeekDimens.buttonHeight), shape = RoundedCornerShape(percent = 50),
        ) {
            Icon(CoffeePeekIcons.Coffee, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(CoffeePeekDimens.spacing2))
            Text(stringResource(Res.string.contributions_request_open_shop))
        }
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
    }
}

@Composable
private fun Hero(request: ChangeRequest) {
    val visual = request.section.visual()
    CoffeePeekContentCard {
        Row(verticalAlignment = Alignment.Top) {
            CoffeePeekIconBadge(visual.icon, visual.colors, Modifier.size(52.dp))
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            Column(Modifier.weight(1f)) {
                Text(request.section.title(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(CoffeePeekDimens.spacing1))
                Text(request.section.hint(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(CoffeePeekDimens.spacing4))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatusPill(request.status, Modifier.padding(vertical = CoffeePeekDimens.spacing1))
            Spacer(Modifier.weight(1f))
            Icon(CoffeePeekIcons.Calendar, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(CoffeePeekDimens.spacing1))
            Text(localDateTime(request.createdAtUtc), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Rejection(reason: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(CoffeePeekDimens.radiusLg)).background(MaterialTheme.colorScheme.error.copy(alpha = 0.09f)).padding(CoffeePeekDimens.spacing4),
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3), verticalAlignment = Alignment.Top,
    ) {
        Icon(CoffeePeekIcons.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
        Column {
            Text(stringResource(Res.string.contributions_request_rejection), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(CoffeePeekDimens.spacing1))
            Text(reason, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PayloadCard(section: ChangeSection, payload: ChangePayload) {
    val visual = section.visual()
    CoffeePeekContentCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Row(Modifier.padding(CoffeePeekDimens.spacing4), verticalAlignment = Alignment.CenterVertically) {
            CoffeePeekIconBadge(visual.icon, visual.colors)
            Spacer(Modifier.width(CoffeePeekDimens.spacing3))
            Text(stringResource(Res.string.contributions_request_payload), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.padding(CoffeePeekDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
            PayloadContent(section, payload)
        }
    }
}

@Composable
private fun ColumnScope.PayloadContent(section: ChangeSection, payload: ChangePayload) {
    when (section) {
        ChangeSection.Description -> DescriptionContent(payload.description)
        ChangeSection.Contacts -> ContactsContent(payload.contacts)
        ChangeSection.Photos -> {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
                StatBlock((payload.photos?.retainedIds?.size ?: 0).toString(), stringResource(Res.string.contributions_request_retained), Modifier.weight(1f))
                StatBlock((payload.photos?.uploaded?.size ?: 0).toString(), stringResource(Res.string.contributions_request_added), Modifier.weight(1f))
            }
            Note(Res.string.contributions_request_gallery_note)
        }
        ChangeSection.Tags -> CountContent(Res.string.contributions_request_selected_tags, payload.tagIds)
        ChangeSection.Roasters -> CountContent(Res.string.contributions_request_selected_roasters, payload.roasterIds)
        ChangeSection.Equipment -> CountContent(Res.string.contributions_request_selected_equipment, payload.equipmentIds)
        ChangeSection.BrewMethods -> CountContent(Res.string.contributions_request_selected_brew_methods, payload.brewMethodIds)
        ChangeSection.Menu -> MenuContent(payload.menu)
    }
}

@Composable
private fun DescriptionContent(description: String?) {
    val text = description?.takeIf(String::isNotBlank)
    Text(
        text ?: stringResource(Res.string.contributions_request_description_removed), style = MaterialTheme.typography.bodyLarge,
        color = if (text == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ContactsContent(contacts: Contacts?) {
    if (contacts == null) return Empty()
    ValueRow(CoffeePeekIcons.Phone, Res.string.contributions_request_phone, contacts.phone)
    ValueRow(CoffeePeekIcons.Email, Res.string.contributions_request_email, contacts.email)
    ValueRow(CoffeePeekIcons.Globe, Res.string.contributions_request_site, contacts.website)
    ValueRow(CoffeePeekIcons.Instagram, Res.string.contributions_request_instagram, contacts.instagram)
}

@Composable
private fun ValueRow(icon: ImageVector, label: StringResource, value: String?) {
    val text = value?.takeIf(String::isNotBlank)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(CoffeePeekDimens.spacing3))
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text ?: stringResource(Res.string.contributions_request_removed), style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (text == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun CountContent(label: StringResource, ids: List<String>?) {
    StatBlock((ids?.size ?: 0).toString(), stringResource(label), Modifier.fillMaxWidth())
    Note(Res.string.contributions_request_set_note)
}

@Composable
private fun MenuContent(menu: ChangeMenu?) {
    if (menu == null) return Empty()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing3)) {
        StatBlock(menu.items.size.toString(), stringResource(Res.string.contributions_request_menu_items), Modifier.weight(1f))
        StatBlock((menu.retainedPhotoIds.size + menu.uploaded.size).toString(), stringResource(Res.string.contributions_request_menu_photos), Modifier.weight(1f))
    }
    menu.items.forEachIndexed { index, entry ->
        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(entry)
    }
}

@Composable
private fun MenuRow(entry: MenuEntry) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(entry.slug, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            entry.price?.let { Text(stringResource(Res.string.contributions_request_price, it.toString()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text(
            entry.availability.title(), style = MaterialTheme.typography.labelMedium,
            color = when (entry.availability) {
                Availability.Present -> CoffeePeekColors.Success
                Availability.Absent -> MaterialTheme.colorScheme.error
                Availability.Unknown -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun StatBlock(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)).background(MaterialTheme.colorScheme.surfaceVariant).padding(CoffeePeekDimens.spacing3)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun Note(text: StringResource) = Text(stringResource(text), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable private fun Empty() = Text(stringResource(Res.string.contributions_request_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

private val noCallbacks = RequestCallbacks({}, {}, {})

private fun sample(section: ChangeSection, payload: ChangePayload, status: ModerationStatus = ModerationStatus.Pending, reason: String? = null) = ChangeRequestState(
    request = ChangeRequest("r", "cafe", section, payload, status, reason, "2026-10-21T07:00:00Z", null), isLoading = false,
)

@Preview @Composable private fun ChangeRequestContactsPreview() = CoffeePeekTheme {
    Surface { ChangeRequestContent(sample(ChangeSection.Contacts, ChangePayload(contacts = Contacts(phone = "+375291112233", website = "sandbox.invalid"))), noCallbacks, {}) }
}

@Preview @Composable private fun ChangeRequestMenuRejectedPreview() = CoffeePeekTheme {
    Surface {
        ChangeRequestContent(
            sample(ChangeSection.Menu, ChangePayload(menu = ChangeMenu(listOf(MenuEntry("latte", Availability.Present, 6.5), MenuEntry("tea", Availability.Absent)), listOf("m1"))),
                ModerationStatus.Rejected, "Фото не подходят"),
            noCallbacks, {},
        )
    }
}

@Preview @Composable private fun ChangeRequestNotFoundPreview() = CoffeePeekTheme {
    Surface { ChangeRequestContent(ChangeRequestState(isLoading = false, error = RequestError.NotFound), noCallbacks, {}) }
}
