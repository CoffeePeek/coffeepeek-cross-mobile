package com.coffeepeek.feature.community.impl.ui

import org.jetbrains.compose.ui.tooling.preview.Preview
import com.coffeepeek.feature.community.impl.resources.Res
import com.coffeepeek.feature.community.impl.resources.community_share_shop
import com.coffeepeek.feature.community.impl.resources.community_share_home
import com.coffeepeek.feature.community.impl.resources.community_share
import com.coffeepeek.feature.community.impl.resources.community_open_post
import com.coffeepeek.feature.community.impl.resources.community_actions
import com.coffeepeek.feature.community.impl.resources.community_block_author
import com.coffeepeek.feature.community.impl.resources.community_details_separator
import com.coffeepeek.feature.community.impl.resources.community_dose
import com.coffeepeek.feature.community.impl.resources.community_edit
import com.coffeepeek.feature.community.impl.resources.community_hide
import com.coffeepeek.feature.community.impl.resources.community_label_private
import com.coffeepeek.feature.community.impl.resources.community_label_public
import com.coffeepeek.feature.community.impl.resources.community_label_rejected
import com.coffeepeek.feature.community.impl.resources.community_label_unknown
import com.coffeepeek.feature.community.impl.resources.community_like
import com.coffeepeek.feature.community.impl.resources.community_likes
import com.coffeepeek.feature.community.impl.resources.community_open_profile
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.coffeepeek.core.designsystem.component.CoffeePeekTextButton as TextButton
import com.coffeepeek.core.designsystem.component.CoffeePeekPlainIconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.coffeepeek.core.designsystem.resources.coffeePeekStringResource as stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coffeepeek.core.designsystem.component.CoffeePeekActionMenu
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineContent
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineHeader
import com.coffeepeek.core.designsystem.component.CoffeePeekTimelineFooter
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuAction
import com.coffeepeek.core.designsystem.component.CoffeePeekMenuIcon
import com.coffeepeek.core.designsystem.component.CoffeePeekRating
import com.coffeepeek.core.designsystem.component.CoffeePeekIcon
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
import com.coffeepeek.feature.community.domain.rules.shareLink
import com.coffeepeek.feature.community.impl.resources.*
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
    /** Only a published post has a link to share. */
    val onShare: (() -> Unit)? = null,
    /** The whole card opens the post on its own screen; the author, shop, photos, like and menu keep their own taps. */
    val onOpen: (() -> Unit)? = null,
    /** A home post's bag opens its coffee in the catalogue; null without a published catalogue coffee. */
    val onCoffee: (() -> Unit)? = null,
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
    horizontalInset: Dp = 0.dp,
) {
    host.nativeCard?.let { renderer ->
        val native = nativePostPresentation(post, isOwn, publicTimeline, showHelpfulCount, enabled, changingVisibility,
            actions, host, horizontalInset.value.toDouble())
        renderer(native, nativePostActions(actions, enabled, changingVisibility), modifier)
        return
    }
    val openLabel = stringResource(Res.string.community_open_post)
    // The gallery scrolls through the side inset; the other content stays inside it.
    val photoScrollInset = if (post.photos.originals.size > 1) horizontalInset else 0.dp
    val textInset = Modifier.padding(horizontal = photoScrollInset)
    CoffeePeekTimelineContent(
        note = post.note,
        modifier = modifier.padding(horizontal = horizontalInset - photoScrollInset).fillMaxWidth().clip(RoundedCornerShape(CoffeePeekDimens.radiusMd))
            .then(actions.onOpen?.let { Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = it) } ?: Modifier),
        header = { PostHeader(post, isOwn, actions, enabled, changingVisibility, host, textInset) },
        heading = { PostIntroduction(post, actions, host, textInset) },
        photos = { PostPhotoStrip(post.photos, actions.onPhoto, host, horizontalInset = photoScrollInset) },
        supporting = { PostPublication(post, isOwn, publicTimeline, actions, enabled, textInset) },
        footer = { PostFooter(post, showHelpfulCount, enabled, changingVisibility, actions, textInset) },
        noteModifier = textInset,
    )
}

@Composable
private fun PostPublication(post: CommunityPost, isOwn: Boolean, publicTimeline: Boolean,
    actions: PostCardActions, enabled: Boolean, modifier: Modifier) {
    if (!publicTimeline && !(isOwn && post.moderation == PostModeration.Rejected)) {
        Text(publicationText(post.publicationLabel()), modifier = modifier,
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (isOwn && post.moderation == PostModeration.Rejected) Box(modifier) {
        PostRejection(post.rejectionReason, actions.onEdit, enabled)
    }
}

@Composable
private fun PostFooter(post: CommunityPost, showHelpfulCount: Boolean, enabled: Boolean,
    changingVisibility: Boolean, actions: PostCardActions, modifier: Modifier) {
    CoffeePeekTimelineFooter(
        count = post.helpfulCount.takeIf { showHelpfulCount }, selected = post.isHelpfulByCurrentUser,
        likesDescription = helpfulDescription(post.helpfulCount, post.isHelpfulByCurrentUser, actions.onHelpful),
        drink = post.drink?.displayName(Locale.current.language), modifier = modifier, onLike = actions.onHelpful,
        actions = actions.onPublish?.let { publish ->
            {
                TextButton(onClick = publish, enabled = enabled) {
                    Text(stringResource(if (changingVisibility) Res.string.community_saving else Res.string.community_publish))
                }
            }
        },
    )
}

@Composable
private fun PostIntroduction(post: CommunityPost, actions: PostCardActions, host: CommunityHost, modifier: Modifier) {
    if (!post.isHome) {
        PostHeading(post.shopName.ifBlank { stringResource(Res.string.community_shop_default) }, post.rating, actions.onShop, modifier)
        return
    }
    val recipe = post.home ?: return
    val details = recipe.details(host.brewMethodLabel)
    val bag = recipe.shelfBagCardUrl
    // A shown bag is the title and carries the recipe line itself.
    if (bag != null) Box(modifier) {
        PostShelfBag(bag, recipe.beanName, recipe.roasterName, details, actions.onCoffee, host)
    } else {
        recipe.beanName?.takeIf(String::isNotBlank)?.let { Text(it, modifier = modifier, style = MaterialTheme.typography.titleMedium) }
        if (details.isNotBlank()) Text(details, modifier = modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PostHeader(post: CommunityPost, isOwn: Boolean, actions: PostCardActions, enabled: Boolean, hiding: Boolean,
    host: CommunityHost, modifier: Modifier) {
    val name = post.author.name.ifBlank { stringResource(if (isOwn) Res.string.community_you else Res.string.community_user) }
    val openLabel = stringResource(Res.string.community_open_profile, name)
    CoffeePeekTimelineHeader(
        name, post.createdAt.takeIf(String::isNotBlank)?.let { host.displayDate(it) }.orEmpty(), modifier,
        avatar = { avatar -> post.author.avatarUrl?.let { host.image(it, null, avatar, ContentScale.Crop, null) } },
        actions = { PostMenu(actions, enabled, hiding) },
        identityModifier = Modifier.clip(RoundedCornerShape(CoffeePeekDimens.radiusMd)).then(
            actions.onAuthor?.let { Modifier.clickable(role = Role.Button, onClickLabel = openLabel, onClick = it) } ?: Modifier,
        ),
    )
}

/** The overflow menu: share, edit, hide, report, block (last, in red) вЂ” only what this viewer may do with this post. */
@Composable
private fun PostMenu(actions: PostCardActions, enabled: Boolean, hiding: Boolean) {
    if (actions.onShare == null && actions.onBlock == null && actions.onEdit == null && actions.onHide == null && actions.onReport == null) return
    val label = stringResource(Res.string.community_actions)
    val items = buildList {
        actions.onShare?.let { add(CoffeePeekMenuAction(stringResource(Res.string.community_share), CoffeePeekIcons.Share, nativeIcon = CoffeePeekMenuIcon.Share, onClick = it)) }
        actions.onEdit?.let { add(CoffeePeekMenuAction(stringResource(Res.string.community_edit), CoffeePeekIcons.Edit, nativeIcon = CoffeePeekMenuIcon.Edit, onClick = it)) }
        actions.onHide?.let {
            // The menu has no disabled rows: while the hide request is running the row only says so and does nothing.
            add(CoffeePeekMenuAction(stringResource(if (hiding) Res.string.community_saving else Res.string.community_hide), CoffeePeekIcons.VisibilityOff, nativeIcon = CoffeePeekMenuIcon.Hide, onClick = if (hiding) ({}) else it))
        }
        actions.onReport?.let { add(CoffeePeekMenuAction(stringResource(Res.string.community_report), CoffeePeekIcons.Error, nativeIcon = CoffeePeekMenuIcon.Report, onClick = it)) }
        actions.onBlock?.let { add(CoffeePeekMenuAction(stringResource(Res.string.community_block_author), CoffeePeekIcons.VisibilityOff, destructive = true, nativeIcon = CoffeePeekMenuIcon.Block, onClick = it)) }
    }
    CoffeePeekActionMenu(items, enabled = enabled) { open ->
        CoffeePeekPlainIconButton(onClick = open, enabled = enabled) { CoffeePeekIcon(CoffeePeekIcons.MoreHorizontal, label, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun publicationText(label: PublicationLabel): String = stringResource(
    when (label) {
        PublicationLabel.Private -> Res.string.community_label_private
        PublicationLabel.Rejected -> Res.string.community_label_rejected
        PublicationLabel.Public -> Res.string.community_label_public
        PublicationLabel.Unknown -> Res.string.community_label_unknown
    },
)

/** Sends a post's link to the system share sheet with the title the website gives its page: В«{author} РІ В«{shop}В»В» or В«РљРѕС„Рµ РґРѕРјР° вЂ” {author}В». */
internal class PostSharer(
    private val shopFormat: String,
    private val homeFormat: String,
    private val userFallback: String,
    private val shopFallback: String,
    private val share: (String) -> Unit,
) {
    /** Null for a post that has no public link; [before] runs first, for analytics. */
    fun action(post: CommunityPost, before: () -> Unit = {}): (() -> Unit)? {
        val link = post.shareLink() ?: return null
        return { before(); share(text(post, link)) }
    }

    private fun text(post: CommunityPost, link: String): String {
        val author = post.author.name.ifBlank { userFallback }
        val arguments = if (post.isHome) listOf(author, link) else listOf(author, post.shopName.ifBlank { shopFallback }, link)
        val format = if (post.isHome) homeFormat else shopFormat
        return Regex("%([1-3])\\\u0024s").replace(format) { arguments[it.groupValues[1].toInt() - 1] }
    }
}

/** Fills the `%1$s`-style placeholders of a share template, as String.format does for these. */
private fun String.fill(vararg args: String): String = args.foldIndexed(this) { index, text, arg -> text.replace("%${index + 1}\$s", arg) }

@Composable
internal fun postSharer(host: CommunityHost) = PostSharer(
    stringResource(Res.string.community_share_shop), stringResource(Res.string.community_share_home),
    stringResource(Res.string.community_user), stringResource(Res.string.community_shop_default), host.share,
)

@Composable
private fun PostHeading(shopName: String, rating: PostRating?, onShop: (() -> Unit)?, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CoffeePeekDimens.spacing2)) {
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
internal fun helpfulDescription(count: Int, helpful: Boolean, onClick: (() -> Unit)?): String =
    when {
        onClick == null -> stringResource(Res.string.community_likes, count)
        helpful -> stringResource(Res.string.community_unlike, count)
        else -> stringResource(Res.string.community_like, count)
    }

@Composable
internal fun HomePreparation.details(methodLabel: (String) -> String?): String {
    val formattedMethod = brewMethodCode?.let(methodLabel)
    val generic = brewMethodCode == "other" && (formattedMethod == null || formattedMethod == brewMethodCode)
    val method = if (generic) stringResource(Res.string.community_drink_other) else formattedMethod
    val separator = stringResource(Res.string.community_details_separator)
    return listOfNotNull(
        method,
        doseGrams?.let { stringResource(Res.string.community_dose, it.toString()) },
        brewDurationSeconds?.let { stringResource(Res.string.community_seconds, it) },
        tasteScore?.let { stringResource(Res.string.community_taste, it) },
    ).joinToString(separator)
}

internal fun com.coffeepeek.feature.community.domain.model.PostDrink.displayName(language: String): String? =
    customName?.takeIf(String::isNotBlank) ?: if (language == "en") nameEn?.takeIf(String::isNotBlank) ?: nameRu?.takeIf(String::isNotBlank)
    else nameRu?.takeIf(String::isNotBlank) ?: nameEn?.takeIf(String::isNotBlank)

internal fun previewPost() = CommunityPost(
    "preview", "cafe", "Sandbox Cafe", PostAuthor("a", "anna", "РђРЅРЅР°", null), "РЈСЋС‚РЅРѕРµ РјРµСЃС‚Рѕ Рё РѕС‚Р»РёС‡РЅС‹Р№ РєРѕС„Рµ.", "2026-10-07T10:00:00Z", "2026-10-07T09:00:00Z",
    PostPhotos(listOf("o1", "o2", "o3", "o4"), listOf("t1", "t2", "t3", "t4"), listOf("d1", "d2", "d3", "d4")), PostRating(4, 4, 5),
    com.coffeepeek.feature.community.domain.model.PostDrink("cappuccino", null, "РљР°РїСѓС‡РёРЅРѕ", "Cappuccino"), PostVisibility.Public, PostModeration.Approved,
    1, null, 3, true, false, null,
)

@Composable private fun PostCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostCard(previewPost(), isOwn = false, publicTimeline = true, showHelpfulCount = true, enabled = true,
        PostCardActions({}, {}, {}, {}, {}, onReport = {}, onShare = {}, onOpen = {}),
        CommunityHost(), horizontalInset = CoffeePeekDimens.spacing4)
}
@Preview @Composable private fun PostCardLightPreview() = PostCardPreview(false)
@Preview @Composable private fun PostCardDarkPreview() = PostCardPreview(true)

@Composable private fun HomePostCardPreview(dark: Boolean) = CoffeePeekTheme(darkTheme = dark) {
    PostCard(previewPost().copy(isHome = true, home = HomePreparation("2026-10-07", "hario-v60", "Р­С„РёРѕРїРёСЏ", 18.0, 150, 4), photos = PostPhotos.None, rating = null),
        isOwn = true, publicTimeline = false, showHelpfulCount = false, enabled = true,
        PostCardActions(null, null, null, null, {}, onEdit = {}, onPublish = {}), CommunityHost())
}

@Preview @Composable private fun HomePostCardLightPreview() = HomePostCardPreview(false)
@Preview @Composable private fun HomePostCardDarkPreview() = HomePostCardPreview(true)
