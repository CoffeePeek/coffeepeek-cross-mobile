package com.coffeepeek.feature.community.impl.ui

import androidx.compose.foundation.background
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.community.impl.resources.Res
import com.coffeepeek.feature.community.impl.resources.community_actions
import com.coffeepeek.feature.community.impl.resources.community_block_author
import com.coffeepeek.feature.community.impl.resources.community_details_separator
import com.coffeepeek.feature.community.impl.resources.community_dose
import com.coffeepeek.feature.community.impl.resources.community_edit
import com.coffeepeek.feature.community.impl.resources.community_hide
import com.coffeepeek.feature.community.impl.resources.community_label_pending
import com.coffeepeek.feature.community.impl.resources.community_label_private
import com.coffeepeek.feature.community.impl.resources.community_label_public
import com.coffeepeek.feature.community.impl.resources.community_label_published
import com.coffeepeek.feature.community.impl.resources.community_label_rejected
import com.coffeepeek.feature.community.impl.resources.community_label_unknown
import com.coffeepeek.feature.community.impl.resources.community_like
import com.coffeepeek.feature.community.impl.resources.community_likes
import com.coffeepeek.feature.community.impl.resources.community_loading
import com.coffeepeek.feature.community.impl.resources.community_more_photos
import com.coffeepeek.feature.community.impl.resources.community_open_profile
import com.coffeepeek.feature.community.impl.resources.community_photo
import com.coffeepeek.feature.community.impl.resources.community_publish
import com.coffeepeek.feature.community.impl.resources.community_rating_description
import com.coffeepeek.feature.community.impl.resources.community_report
import com.coffeepeek.feature.community.impl.resources.community_saving
import com.coffeepeek.feature.community.impl.resources.community_seconds
import com.coffeepeek.feature.community.impl.resources.community_shop_default
import com.coffeepeek.feature.community.impl.resources.community_taste
import com.coffeepeek.feature.community.impl.resources.community_unlike
import com.coffeepeek.feature.community.impl.resources.community_user
import com.coffeepeek.feature.community.impl.resources.community_you
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekLikeIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekLoader
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.icons.CoffeePeekIcons
import com.coffeepeek.core.designsystem.theme.CoffeePeekColors
import com.coffeepeek.core.designsystem.theme.CoffeePeekDimens
import com.coffeepeek.core.designsystem.theme.CoffeePeekTheme
import com.coffeepeek.feature.community.api.CommunityHost
import com.coffeepeek.feature.community.domain.model.CommunityPost
import com.coffeepeek.feature.community.domain.model.HomePreparation
import com.coffeepeek.feature.community.domain.model.PostAuthor
import com.coffeepeek.feature.community.domain.model.PostModeration
import com.coffeepeek.feature.community.domain.model.PostPhotos
import com.coffeepeek.feature.community.domain.model.PostRating
import com.coffeepeek.feature.community.domain.model.PostVisibility
import com.coffeepeek.feature.community.domain.rules.PublicationLabel
import com.coffeepeek.feature.community.domain.rules.publicationLabel
import kotlin.math.roundToInt

/** What a card needs to know about the viewer; the actions are null when the viewer cannot use them. */
internal class PostCardActions(
    val onAuthor: (() -> Unit)?,
    val onShop: (() -> Unit)?,
    val onHelpful: (() -> Unit)?,
    val onBlock: (() -> Unit)?,
    val onPhoto: (index: Int) -> Unit,
    val onEdit: (() -> Unit)? = null,
    val onHide: (() -> Unit)? = null,
    val onReport: (() -> Unit)? = null,
    /** The owner publishes a private post. */
    val onPublish: (() -> Unit)? = null,
)

@Composable
internal fun PostCard(
    post: CommunityPost,
    isOwn: Boolean,
    publicTimeline: Boolean,
    showHelpfulCount: Boolean,
    enabled: Boolean,
    actions: PostCardActions,
    host: CommunityHost,
    modifier: Modifier = Modifier,
    changingVisibility: Boolean = false,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        PostHeader(post, isOwn, actions, enabled, changingVisibility, host)
        if (post.isHome) {
            post.home?.let { recipe ->
                recipe.beanName?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                val details = recipe.details(host.brewMethodLabel)
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else PostHeading(post.shopName.ifBlank { stringResource(Res.string.community_shop_default) }, post.rating, actions.onShop)
        if (post.note.isNotBlank()) Text(post.note, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        PostPhotoStrip(post.photos, actions.onPhoto, host)
        if (!publicTimeline) Text(publicationText(post.publicationLabel()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (isOwn && post.moderation == PostModeration.Rejected) post.rejectionReason?.takeIf(String::isNotBlank)?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        val drinkName = post.drink?.displayName(Locale.current.language)
        if (showHelpfulCount || drinkName != null || actions.onPublish != null) Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
        ) {
            if (showHelpfulCount) HelpfulButton(post.helpfulCount, post.isHelpfulByCurrentUser, actions.onHelpful)
            Spacer(Modifier.weight(1f))
            actions.onPublish?.let { publish ->
                TextButton(onClick = publish, enabled = enabled) {
                    Text(stringResource(if (changingVisibility) Res.string.community_saving else Res.string.community_publish))
                }
            }
            drinkName?.let { DrinkLabel(it) }
        }
    }
}

@Composable
private fun PostHeader(post: CommunityPost, isOwn: Boolean, actions: PostCardActions, enabled: Boolean, hiding: Boolean, host: CommunityHost) {
    val name = post.author.name.ifBlank { stringResource(if (isOwn) Res.string.community_you else Res.string.community_user) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val openLabel = stringResource(Res.string.community_open_profile, name)
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)).then(
                actions.onAuthor?.let { Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = it) } ?: Modifier,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(name, post.author.avatarUrl, host)
            Column(Modifier.weight(1f).padding(start = CoffeePeekDimens.spacing3), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (post.createdAt.isNotBlank()) Text(host.displayDate(post.createdAt), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        PostMenu(actions, enabled, hiding)
    }
}

@Composable
private fun Avatar(name: String, photoUrl: String?, host: CommunityHost) {
    val avatar = Modifier.size(44.dp).clip(CircleShape)
    Box(avatar.background(CoffeePeekColors.GoldWarmSoft), contentAlignment = Alignment.Center) {
        Text(name.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold, color = CoffeePeekColors.LightTextPrimary)
        if (photoUrl != null) host.image(photoUrl, null, avatar, ContentScale.Crop)
    }
}

/** The overflow menu: block, edit, hide, report — only what this viewer may do with this post. */
@Composable
private fun PostMenu(actions: PostCardActions, enabled: Boolean, hiding: Boolean) {
    if (actions.onBlock == null && actions.onEdit == null && actions.onHide == null && actions.onReport == null) return
    val label = stringResource(Res.string.community_actions)
    val items = buildList {
        actions.onBlock?.let { add(CoffeePeekMenuAction(stringResource(Res.string.community_block_author), CoffeePeekIcons.VisibilityOff, destructive = true, nativeIcon = CoffeePeekMenuIcon.Block, onClick = it)) }
        actions.onEdit?.let { add(CoffeePeekMenuAction(stringResource(Res.string.community_edit), CoffeePeekIcons.Edit, nativeIcon = CoffeePeekMenuIcon.Edit, onClick = it)) }
        actions.onHide?.let {
            // The menu has no disabled rows: while the hide request is running the row only says so and does nothing.
            add(CoffeePeekMenuAction(stringResource(if (hiding) Res.string.community_saving else Res.string.community_hide), CoffeePeekIcons.VisibilityOff, nativeIcon = CoffeePeekMenuIcon.Hide, onClick = if (hiding) ({}) else it))
        }
        actions.onReport?.let { add(CoffeePeekMenuAction(stringResource(Res.string.community_report), CoffeePeekIcons.Error, nativeIcon = CoffeePeekMenuIcon.Report, onClick = it)) }
    }
    CoffeePeekActionMenu(items, menuDescription = label, enabled = enabled) { open ->
        IconButton(onClick = open, enabled = enabled) { Icon(CoffeePeekIcons.MoreHorizontal, label, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun publicationText(label: PublicationLabel): String = stringResource(
    when (label) {
        PublicationLabel.Private -> Res.string.community_label_private
        PublicationLabel.Pending -> Res.string.community_label_pending
        PublicationLabel.Published -> Res.string.community_label_published
        PublicationLabel.Rejected -> Res.string.community_label_rejected
        PublicationLabel.Public -> Res.string.community_label_public
        PublicationLabel.Unknown -> Res.string.community_label_unknown
    },
)

@Composable
private fun PostHeading(shopName: String, rating: PostRating?, onShop: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
        Text(shopName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.then(onShop?.let { Modifier.clickable(role = Role.Button, onClick = it) } ?: Modifier))
        rating?.let { PostRatingStars(it) }
    }
}

@Composable
private fun PostRatingStars(rating: PostRating) {
    val average = (rating.coffee + rating.service + rating.place) / 3.0
    val text = oneDecimal(average)
    CoffeePeekRating(
        average, activeColor = CoffeePeekColors.Primary, valueText = text, valueColor = MaterialTheme.colorScheme.onSurface,
        contentDescription = stringResource(Res.string.community_rating_description, rating.coffee, rating.service, rating.place, text),
    )
}

internal fun oneDecimal(value: Double): String {
    val tenths = (value * 10.0).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

@Composable
private fun HelpfulButton(count: Int, helpful: Boolean, onClick: (() -> Unit)?) {
    val description = when {
        onClick == null -> stringResource(Res.string.community_likes, count)
        helpful -> stringResource(Res.string.community_unlike, count)
        else -> stringResource(Res.string.community_like, count)
    }
    Row(
        Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp).clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2),
    ) {
        CoffeePeekLikeIcon(helpful, contentDescription = null)
        Text(count.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DrinkLabel(name: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1), verticalAlignment = Alignment.CenterVertically) {
        Icon(CoffeePeekIcons.Coffee, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PostPhotoStrip(photos: PostPhotos, onPhoto: (Int) -> Unit, host: CommunityHost) {
    val count = photos.originals.size
    if (count == 0) return
    @Composable
    fun Photo(index: Int, modifier: Modifier) {
        val description = stringResource(Res.string.community_photo, index + 1, count)
        Box(
            modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)).clickable(role = Role.Button) { onPhoto(index) }
                .semantics(mergeDescendants = true) { contentDescription = description },
        ) {
            // The loader stays under the photo: a photo that is not loaded yet (a fresh list after leaving the tab) never shows an empty hole.
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                CoffeePeekLoader(stringResource(Res.string.community_loading), size = 32.dp)
            }
            host.image(photos.thumbnails.getOrNull(index) ?: photos.originals[index], photos.inlineSource(index), Modifier.fillMaxSize(), ContentScale.Crop)
            if (index == 2 && count > 3) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.community_more_photos, count - 3), style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
        }
    }
    if (count == 1) Photo(0, Modifier.fillMaxWidth().aspectRatio(2f))
    else Row(Modifier.fillMaxWidth().aspectRatio(4f / 3f), horizontalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
        Photo(0, Modifier.weight(1f).fillMaxHeight())
        if (count == 2) Photo(1, Modifier.weight(1f).fillMaxHeight())
        else Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing1)) {
            Photo(1, Modifier.weight(1f).fillMaxWidth())
            Photo(2, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun HomePreparation.details(methodLabel: (String) -> String?): String {
    val separator = stringResource(Res.string.community_details_separator)
    return listOfNotNull(
        brewMethodCode?.let(methodLabel),
        doseGrams?.let { stringResource(Res.string.community_dose, it.toString()) },
        brewDurationSeconds?.let { stringResource(Res.string.community_seconds, it) },
        tasteScore?.let { stringResource(Res.string.community_taste, it) },
    ).joinToString(separator)
}

private fun com.coffeepeek.feature.community.domain.model.PostDrink.displayName(language: String): String? =
    customName?.takeIf(String::isNotBlank) ?: if (language == "en") nameEn?.takeIf(String::isNotBlank) ?: nameRu?.takeIf(String::isNotBlank)
    else nameRu?.takeIf(String::isNotBlank) ?: nameEn?.takeIf(String::isNotBlank)

internal fun previewPost() = CommunityPost(
    "preview", "cafe", "Sandbox Cafe", PostAuthor("a", "anna", "Анна", null), "Уютное место и отличный кофе.", "2026-10-07T10:00:00Z", "2026-10-07T09:00:00Z",
    PostPhotos(listOf("o1", "o2", "o3", "o4"), listOf("t1", "t2", "t3", "t4"), listOf("d1", "d2", "d3", "d4")), PostRating(4, 4, 5),
    com.coffeepeek.feature.community.domain.model.PostDrink("cappuccino", null, "Капучино", "Cappuccino"), PostVisibility.Public, PostModeration.Approved,
    1, null, 3, true, false, null,
)

@Composable private fun PostCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostCard(previewPost(), isOwn = false, publicTimeline = true, showHelpfulCount = true, enabled = true, PostCardActions({}, {}, {}, {}, {}), CommunityHost())
}

@Composable private fun HomePostCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostCard(previewPost().copy(isHome = true, home = HomePreparation("2026-10-07", "hario-v60", "Эфиопия", 18.0, 150, 4), photos = PostPhotos.None, rating = null),
        isOwn = true, publicTimeline = false, showHelpfulCount = false, enabled = true, PostCardActions(null, null, null, null, {}), CommunityHost())
}

@Preview @Composable private fun PostCardLightPreview() = PostCardPreview(false)
@Preview @Composable private fun PostCardDarkPreview() = PostCardPreview(true)
@Preview @Composable private fun HomePostCardLightPreview() = HomePostCardPreview(false)
@Preview @Composable private fun HomePostCardDarkPreview() = HomePostCardPreview(true)
