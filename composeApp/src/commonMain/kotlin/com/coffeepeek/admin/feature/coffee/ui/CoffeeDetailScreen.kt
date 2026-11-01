package com.coffeepeek.admin.feature.coffee.ui

import coffeepeek.composeapp.generated.resources.*
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource

import com.coffeepeek.core.designsystem.component.coffeePeekScrollContentPadding

import com.coffeepeek.core.designsystem.component.CoffeePeekScaffold as Scaffold

import com.coffeepeek.admin.analytics.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.admin.feature.coffee.domain.CoffeeAvailability
import com.coffeepeek.admin.feature.coffee.domain.CoffeeOffer
import com.coffeepeek.admin.theme.CpColor
import com.coffeepeek.admin.theme.CpDimens
import com.coffeepeek.admin.ui.Navigator
import com.coffeepeek.admin.ui.component.*
import com.coffeepeek.admin.ui.icons.CpIcons
import com.coffeepeek.admin.utils.OpenInBrowser
import com.coffeepeek.admin.utils.ShareHelper
import com.coffeepeek.admin.utils.utcIsoToLocalDateTime
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.coffeepeek.admin.di.platformViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun CoffeeDetailScreen(slug: String) {
    val localizedRetry = stringResource(Res.string.retry)
    val localizedLegacyCoffeeDetailScreenCoffeeOpenPhotos = stringResource(Res.string.legacy_coffee_detail_screen_coffee_open_photos)
    val localizedLegacyCoffeeDetailScreenCoffeePhotoMissing = stringResource(Res.string.legacy_coffee_detail_screen_coffee_photo_missing)
    val localizedLegacyGroupedListDiscoveryFiltersCollapse = stringResource(Res.string.legacy_grouped_list_discovery_filters_collapse)
    val localizedLegacyReviewContentShopCheckinsReadMore = stringResource(Res.string.legacy_review_content_shop_checkins_read_more)
    val localizedLegacyCoffeeDetailScreenCoffeeRoast = stringResource(Res.string.legacy_coffee_detail_screen_coffee_roast)
    val localizedLegacyCoffeeDetailScreenCoffeeProfile = stringResource(Res.string.legacy_coffee_detail_screen_coffee_profile)
    val localizedLegacyCoffeeDetailScreenCoffeeAcidity = stringResource(Res.string.legacy_coffee_detail_screen_coffee_acidity)
    val localizedLegacyCoffeeDetailScreenCoffeeTaste = stringResource(Res.string.legacy_coffee_detail_screen_coffee_taste)
    val localizedLegacyCoffeeDetailScreenCoffeeAbout = stringResource(Res.string.legacy_coffee_detail_screen_coffee_about)
    val localizedLegacyCoffeeDetailScreenCoffeeCountry = stringResource(Res.string.legacy_coffee_detail_screen_coffee_country)
    val localizedLegacyCoffeeDetailScreenCoffeeUnspecifiedFeminine = stringResource(Res.string.legacy_coffee_detail_screen_coffee_unspecified_feminine)
    val localizedLegacyCoffeeDetailScreenCoffeeForm = stringResource(Res.string.legacy_coffee_detail_screen_coffee_form)
    val localizedLegacyCoffeeDetailScreenShelfFormRoaster = stringResource(Res.string.legacy_coffee_detail_screen_shelf_form_roaster)
    val localizedLegacyCoffeeDetailScreenCoffeeOffers = stringResource(Res.string.legacy_coffee_detail_screen_coffee_offers)
    val localizedLegacyCoffeeDetailScreenCoffeeNoOffers = stringResource(Res.string.legacy_coffee_detail_screen_coffee_no_offers)
    val localizedLegacyCoffeeDetailScreenCoffeeSimilar = stringResource(Res.string.legacy_coffee_detail_screen_coffee_similar)
    val localizedLegacyCoffeeDetailScreenCoffeeRetrySimilar = stringResource(Res.string.legacy_coffee_detail_screen_coffee_retry_similar)
    val localizedProfileCheckinsBack = stringResource(Res.string.profile_checkins_back)
    val localizedLegacyCoffeeDetailScreenCoffeeShare = stringResource(Res.string.legacy_coffee_detail_screen_coffee_share)

    val vm: CoffeeDetailViewModel = platformViewModel(key = slug, parameters = { parametersOf(slug) })
    val state by vm.state.collectAsState()
    com.coffeepeek.admin.analytics.TrackEntityDetail(com.coffeepeek.admin.analytics.Entity.Coffee, state.details != null)
    val details = state.details
    val hazeState = rememberHazeState()
    var showPhotos by rememberSaveable(slug) { mutableStateOf(false) }
    var expandedDescription by rememberSaveable(slug) { mutableStateOf(false) }
    var descriptionOverflows by remember(slug) { mutableStateOf(false) }
    val labels = state.filterGroups.associate { group -> group.code to group.options.associate { it.code to it.name } }
    val tasteNames = labels["taste"].orEmpty()
    val traitWidth = 168.dp * LocalDensity.current.fontScale
    val actionColors = ButtonDefaults.textButtonColors(
        contentColor = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) CpColor.AccentTextDark else CpColor.AccentTextLight,
    )

    if (showPhotos && details != null) FullScreenImageDialog(
        imageUrls = details.photos, initialIndex = 0, onDismiss = { showPhotos = false },
    )

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.hazeSource(hazeState),
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                state.error != null -> Column(
                    Modifier.fillMaxSize().padding(padding).padding(CpDimens.spacing6),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = vm::load, colors = actionColors) { Text(localizedRetry) }
                }
                details != null -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = coffeePeekScrollContentPadding(PaddingValues(bottom = CpDimens.spacing6)),
                    verticalArrangement = Arrangement.spacedBy(CpDimens.spacing6),
                ) {
                    item(key = "photo") {
                        Box(
                            Modifier.fillMaxWidth().then(if (details.photos.isNotEmpty()) Modifier.analyticsExposure("coffee_photos", "coffee_detail", "coffee_detail") else Modifier).aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = details.photos.isNotEmpty(), role = Role.Button, onClickLabel = localizedLegacyCoffeeDetailScreenCoffeeOpenPhotos) { Analytics.track(AnalyticsEvent.ENTITY_CONTENT_INTERACTED, "entity_type" to "coffee", "section" to "photos", "action" to "open"); showPhotos = true },
                        ) {
                            val photo = details.photos.firstOrNull()
                            if (photo != null) CoffeeShopImage(
                                imageUrl = photo, contentDescription = details.coffee.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(top = 56.dp, bottom = CpDimens.spacing4),
                            ) else CoffeeShopPlaceholderImage(contentDescription = localizedLegacyCoffeeDetailScreenCoffeePhotoMissing)
                        }
                    }
                    item(key = "intro") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            Text(details.coffee.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            val kind = productKindLabel(details.productKind)
                            val form = productFormLabel(details.productForm)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                                details.coffee.countries.forEach { CoffeeLabel(it) }
                                listOfNotNull(kind, form).takeIf { it.isNotEmpty() }?.let { CoffeeLabel(it.joinToString(" · ")) }
                            }
                            details.description?.let { description ->
                                Column {
                                    Text(
                                        description, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = if (expandedDescription) Int.MAX_VALUE else 4,
                                        overflow = TextOverflow.Ellipsis,
                                        onTextLayout = { if (!expandedDescription) descriptionOverflows = it.hasVisualOverflow },
                                    )
                                    if (descriptionOverflows || expandedDescription) TextButton(onClick = { expandedDescription = !expandedDescription; Analytics.track(AnalyticsEvent.ENTITY_CONTENT_INTERACTED, "entity_type" to "coffee", "section" to "description", "action" to if (expandedDescription) "open" else "close") }, colors = actionColors) {
                                        Text(if (expandedDescription) localizedLegacyGroupedListDiscoveryFiltersCollapse else localizedLegacyReviewContentShopCheckinsReadMore)
                                    }
                                }
                            }
                            val traits = listOf(
                                Triple(localizedLegacyCoffeeDetailScreenCoffeeRoast, labels["roast"]?.get(details.roastLevel), CpIcons.Fire),
                                Triple(localizedLegacyCoffeeDetailScreenCoffeeProfile, details.coffee.tasteCodes.mapNotNull(tasteNames::get).takeIf { it.isNotEmpty() }?.joinToString(", "), CpIcons.Sparkle),
                                Triple(localizedLegacyCoffeeDetailScreenCoffeeAcidity, labels["acidity"]?.get(details.acidity), CpIcons.Drop),
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
                                traits.forEach { (label, value, icon) ->
                                    if (value != null) Row(Modifier.width(traitWidth), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
                                        SettingsIconBadge(icon, SettingsIconColors(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface))
                                        Column(Modifier.weight(1f)) {
                                            Text(value, style = MaterialTheme.typography.titleSmall)
                                            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (details.tasteDescriptors.isNotEmpty()) item(key = "taste") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            SectionTitle(localizedLegacyCoffeeDetailScreenCoffeeTaste)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                                details.tasteDescriptors.forEach { CoffeeLabel(it) }
                            }
                        }
                    }
                    item(key = "about") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            SectionTitle(localizedLegacyCoffeeDetailScreenCoffeeAbout)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing6), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                listOf(
                                    localizedLegacyCoffeeDetailScreenCoffeeCountry to details.coffee.countries.joinToString(", ").ifBlank { localizedLegacyCoffeeDetailScreenCoffeeUnspecifiedFeminine },
                                    localizedLegacyCoffeeDetailScreenCoffeeForm to (productFormLabel(details.productForm) ?: localizedLegacyCoffeeDetailScreenCoffeeUnspecifiedFeminine),
                                ).forEach { (label, value) ->
                                    Column(Modifier.widthIn(min = 140.dp), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing1)) {
                                        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(value, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                            Text(localizedLegacyCoffeeDetailScreenShelfFormRoaster, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.analyticsExposure("coffee_roaster", "coffee_detail", "coffee_detail"))
                            OutlinedContentCard(contentPadding = PaddingValues(0.dp)) {
                                RoasterLinkRow(
                                    name = details.coffee.roasterName, photoUrl = details.coffee.roasterPhotoUrl,
                                    onClick = details.roasterSlug?.let { roaster -> { Analytics.track(AnalyticsEvent.ENTITY_CONTENT_INTERACTED, "entity_type" to "coffee", "section" to "roaster", "action" to "select"); DetailEntryMemory.record(Entity.Roaster, ResultSource.Related, null); Navigator.navigate(Navigator.Screen.RoasterDetail(roaster)) } },
                                )
                            }
                        }
                    }
                    item(key = "offers-title") {
                        Column(Modifier.padding(horizontal = CpDimens.spacing4).analyticsExposure("coffee_offers", "coffee_detail", "coffee_detail"), verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2)) {
                            SectionTitle(localizedLegacyCoffeeDetailScreenCoffeeOffers)
                            details.checkedAtUtc?.let { Text(stringResource(Res.string.legacy_coffee_verified_time, utcIsoToLocalDateTime(it)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            if (details.coffee.offers.isEmpty()) Text(localizedLegacyCoffeeDetailScreenCoffeeNoOffers, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    itemsIndexed(details.coffee.offers) { _, offer ->
                        Box(Modifier.padding(horizontal = CpDimens.spacing4)) { CoffeeOfferCard(offer, labels["brew"].orEmpty()) }
                    }
                    if (state.similarLoading || state.similarError || state.similar.isNotEmpty()) item(key = "similar") {
                        Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                            Box(Modifier.padding(horizontal = CpDimens.spacing4).analyticsExposure("similar_coffees", "coffee_detail", "coffee_detail")) { SectionTitle(localizedLegacyCoffeeDetailScreenCoffeeSimilar) }
                            when {
                                state.similarLoading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CoffeePeekLoader() }
                                state.similarError -> TextButton(onClick = vm::loadSimilar, modifier = Modifier.padding(horizontal = CpDimens.spacing4), colors = actionColors) { Text(localizedLegacyCoffeeDetailScreenCoffeeRetrySimilar) }
                                else -> LazyRow(contentPadding = PaddingValues(horizontal = CpDimens.spacing4), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing3)) {
                                    items(state.similar, key = { it.slug }) { CoffeeCard(it, tasteNames, Modifier.width(280.dp), onOpen = { Analytics.track(AnalyticsEvent.ENTITY_CONTENT_INTERACTED, "entity_type" to "coffee", "section" to "similar_coffees", "action" to "select"); DetailEntryMemory.record(Entity.Coffee, ResultSource.Related, null) }) }
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(CpDimens.spacing3),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PlatformGlassIconButton(GlassControlIcon.Back, Navigator::popBack, localizedProfileCheckinsBack, hazeState = hazeState) {
                Icon(CpIcons.Back, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            PlatformGlassIconButton(
                GlassControlIcon.Share,
                onClick = { Analytics.track(AnalyticsEvent.ENTITY_CONTENT_INTERACTED, "entity_type" to "coffee", "section" to "contacts", "action" to "share"); details?.let { ShareHelper.shareText("${it.coffee.name} — https://coffeepeek.by${it.canonicalPath}") } },
                contentDescription = localizedLegacyCoffeeDetailScreenCoffeeShare, enabled = details != null, hazeState = hazeState,
            ) {
                Icon(CpIcons.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun CoffeeOfferCard(offer: CoffeeOffer, brewNames: Map<String, String>) {
    val localizedLegacyCoffeeDetailScreenCoffeeInStock = stringResource(Res.string.legacy_coffee_detail_screen_coffee_in_stock)
    val localizedLegacyCoffeeDetailScreenCoffeeOutOfStock = stringResource(Res.string.legacy_coffee_detail_screen_coffee_out_of_stock)
    val localizedLegacyCoffeeDetailScreenCoffeePreorder = stringResource(Res.string.legacy_coffee_detail_screen_coffee_preorder)
    val localizedLegacyCoffeeDetailScreenCoffeeAvailabilityUnknown = stringResource(Res.string.legacy_coffee_detail_screen_coffee_availability_unknown)
    val localizedLegacyCoffeeListScreenCoffeeWeightMissing = stringResource(Res.string.legacy_coffee_list_screen_coffee_weight_missing)
    val localizedLegacyCoffeeDetailScreenCoffeeOpenSeller = stringResource(Res.string.legacy_coffee_detail_screen_coffee_open_seller)
    val localizedLegacyCoffeeDetailScreenCoffeeSeller = stringResource(Res.string.legacy_coffee_detail_screen_coffee_seller)
    val localizedLegacyHomeCheckInDetailHomeCheckInUnspecified = stringResource(Res.string.legacy_home_check_in_detail_home_check_in_unspecified)
    val localizedLegacyCoffeeDetailScreenCoffeeGrind = stringResource(Res.string.legacy_coffee_detail_screen_coffee_grind)
    val localizedLegacyCoffeeDetailScreenCoffeeBrew = stringResource(Res.string.legacy_coffee_detail_screen_coffee_brew)
    val localizedLegacyCoffeeDetailScreenCoffeeUnconfirmed = stringResource(Res.string.legacy_coffee_detail_screen_coffee_unconfirmed)
    val localizedLegacyCoffeeDetailScreenCoffeeSource = stringResource(Res.string.legacy_coffee_detail_screen_coffee_source)
    val localizedLegacyCoffeeDetailScreenCoffeeOnline = stringResource(Res.string.legacy_coffee_detail_screen_coffee_online)
    val localizedLegacyCoffeeDetailScreenCoffeeOffline = stringResource(Res.string.legacy_coffee_detail_screen_coffee_offline)
    val localizedLegacyCoffeeDetailScreenCoffeeBoth = stringResource(Res.string.legacy_coffee_detail_screen_coffee_both)
    val localizedLegacyCoffeeDetailScreenCoffeeChecked = stringResource(Res.string.legacy_coffee_detail_screen_coffee_checked)

    OutlinedContentCard {
        Column(verticalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
            val (availability, color) = when (offer.availability) {
                CoffeeAvailability.InStock -> localizedLegacyCoffeeDetailScreenCoffeeInStock to CpColor.Success
                CoffeeAvailability.OutOfStock -> localizedLegacyCoffeeDetailScreenCoffeeOutOfStock to CpColor.Error
                CoffeeAvailability.PreOrder -> localizedLegacyCoffeeDetailScreenCoffeePreorder to CpColor.Warning
                CoffeeAvailability.Unknown -> localizedLegacyCoffeeDetailScreenCoffeeAvailabilityUnknown to MaterialTheme.colorScheme.onSurfaceVariant
            }
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.spacedBy(CpDimens.spacing2), itemVerticalAlignment = Alignment.CenterVertically) {
                Text(offer.weightGrams?.let { stringResource(Res.string.legacy_grams, it.toString()) } ?: localizedLegacyCoffeeListScreenCoffeeWeightMissing, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing2), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Text(availability, style = MaterialTheme.typography.bodySmall)
                }
            }
            CoffeePrice(offer)
            AppButton(localizedLegacyCoffeeDetailScreenCoffeeOpenSeller, onClick = { offer.sourceUrl?.let(OpenInBrowser::openInBrowser) }, enabled = offer.sourceUrl != null)
            val fields = listOf(
                localizedLegacyCoffeeDetailScreenCoffeeSeller to (offer.sellerName ?: localizedLegacyHomeCheckInDetailHomeCheckInUnspecified),
                localizedLegacyCoffeeDetailScreenCoffeeGrind to (productFormLabel(offer.grind) ?: localizedLegacyHomeCheckInDetailHomeCheckInUnspecified),
                localizedLegacyCoffeeDetailScreenCoffeeBrew to (brewNames[offer.brewPurpose] ?: localizedLegacyCoffeeDetailScreenCoffeeUnconfirmed),
                localizedLegacyCoffeeDetailScreenCoffeeSource to when (offer.availabilityScope) { "online" -> localizedLegacyCoffeeDetailScreenCoffeeOnline; "offline" -> localizedLegacyCoffeeDetailScreenCoffeeOffline; "both" -> localizedLegacyCoffeeDetailScreenCoffeeBoth; else -> localizedLegacyHomeCheckInDetailHomeCheckInUnspecified },
            ) + listOfNotNull(offer.checkedAtUtc?.let { localizedLegacyCoffeeDetailScreenCoffeeChecked to utcIsoToLocalDateTime(it) })
            fields.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CpDimens.spacing4)) {
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun productKindLabel(code: String?): String? = when (code) {
    "roasted_beans" -> stringResource(Res.string.legacy_coffee_detail_screen_coffee_roasted)
    "green_beans" -> stringResource(Res.string.legacy_coffee_detail_screen_coffee_green)
    else -> null
}

@Composable
private fun productFormLabel(code: String?): String? = when (code) {
    "whole_beans" -> stringResource(Res.string.legacy_coffee_detail_screen_coffee_whole_beans)
    "ground" -> stringResource(Res.string.legacy_coffee_detail_screen_shelf_grind_ground)
    "capsules" -> stringResource(Res.string.legacy_coffee_detail_screen_coffee_capsules)
    "drip_bags" -> stringResource(Res.string.legacy_coffee_detail_screen_coffee_drip_bags)
    else -> null
}
