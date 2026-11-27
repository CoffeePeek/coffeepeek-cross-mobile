package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.*

class IosCommunityCardState internal constructor(data: CommunityCardPresentation?, actions: CommunityCardActions?, val active: Boolean) {
    val key = data?.key.orEmpty()
    val header = IosCommunityCardHeader(data?.header)
    val body = IosCommunityCardBody(data?.body)
    val footer = IosCommunityCardFooter(data?.footer)
    val status = IosCommunityCardStatus(data?.status)
    val menu = data?.menu?.map(::IosCommunityCardMenuItem).orEmpty()
    val openLabel = data?.openLabel.orEmpty()
    val menuLabel = data?.menuLabel.orEmpty()
    val horizontalInset = data?.horizontalInset ?: 0.0
    val availableActions = if (active) actions?.actions?.keys?.toList().orEmpty() else emptyList()
}

class IosCommunityCardHeader internal constructor(data: CommunityCardHeader?) {
    val name = data?.name.orEmpty()
    val avatar = data?.avatar
    val dateLabel = data?.dateLabel.orEmpty()
    val openAuthorLabel = data?.openAuthorLabel.orEmpty()
}

class IosCommunityCardBody internal constructor(data: CommunityCardBody?) {
    val isHome = data?.isHome ?: false
    val title = data?.title.orEmpty()
    val note = data?.note.orEmpty()
    val rating = data?.rating
    val ratingLabel = data?.ratingLabel.orEmpty()
    val beanName = data?.beanName
    val roasterName = data?.roasterName
    val bagUrl = data?.bagUrl
    val recipe = data?.recipe.orEmpty()
    val photos = data?.photos?.map(::IosCommunityCardPhoto).orEmpty()
    val recipeSeparator = data?.recipeSeparator.orEmpty()
    val openCoffeeLabel = data?.openCoffeeLabel.orEmpty()
}

class IosCommunityCardPhoto internal constructor(data: CommunityCardPhoto) {
    val previewUrl = data.previewUrl
    val detailUrl = data.detailUrl
    val label = data.label
}

class IosCommunityCardFooter internal constructor(data: CommunityCardFooter?) {
    val likes = data?.likes
    val liked = data?.liked ?: false
    val likesLabel = data?.likesLabel.orEmpty()
    val drink = data?.drink
    val publishLabel = data?.publishLabel
}

class IosCommunityCardStatus internal constructor(data: CommunityCardStatus?) {
    val publication = data?.publication
    val rejectedTitle = data?.rejectedTitle
    val rejectedReason = data?.rejectedReason
    val editLabel = data?.editLabel
}

class IosCommunityCardMenuItem internal constructor(data: CommunityCardMenuItem) {
    val id = data.id
    val label = data.label
    val enabled = data.enabled
    val destructive = data.destructive
}
