package com.coffeepeek.admin.di.community

import com.coffeepeek.feature.community.api.CommunityCardPresentation

internal data class NativeCheckInDetailPresentation(
    val card: CommunityCardPresentation?, val title: String, val backLabel: String,
    val active: Boolean = true, val dismissAllowed: Boolean = true,
    val loadingLabel: String? = null, val errorText: String? = null, val retryLabel: String? = null,
    val notice: String? = null,
    val confirmation: IosCheckInDetailConfirmation? = null,
)

class IosCheckInDetailState internal constructor(data: NativeCheckInDetailPresentation?, card: IosCommunityCardState) {
    val active = data?.active == true && card.active
    val title = data?.title.orEmpty()
    val backLabel = data?.backLabel.orEmpty()
    val dismissAllowed = data?.dismissAllowed != false
    val loadingLabel = data?.loadingLabel.takeIf { active }
    val errorText = data?.errorText.takeIf { active }
    val retryLabel = data?.retryLabel.takeIf { active }
    val notice = data?.notice.takeIf { active }
    val card = card.takeIf { active && data?.card != null }
    val confirmation = data?.confirmation.takeIf { active }
}

class IosCheckInDetailConfirmation internal constructor(val title: String, val body: String, val confirmLabel: String, val cancelLabel: String)
