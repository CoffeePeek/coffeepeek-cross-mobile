package com.coffeepeek.admin.di.homecheckin

import platform.Foundation.NSData

/** Read-only presentation values for a native editor; domain and transport models stay behind the session. */
data class IosHomeCheckInState(
    val draft: IosHomeCheckInDraft,
    val status: IosHomeCheckInStatus,
    val catalog: IosHomeCheckInCatalog,
    val issues: IosHomeCheckInIssues,
    val saved: IosHomeCheckInSaved?,
)

data class IosHomeCheckInDraft(
    val preparedOn: String, val dateLabel: String, val methodCode: String?,
    val dose: String, val durationSeconds: Int?, val tasteScore: Int?,
    val note: String, val drinkName: String, val isPublic: Boolean?,
    val beanName: String?, val shelfBagId: String?, val showShelfBag: Boolean,
    val shelfBagCardUrl: String?, val photos: List<IosCheckInPhoto>, val existingPhotoUrls: List<String>,
    val customBeanName: String? = null,
    val drinkSlug: String? = null,
)

data class IosHomeCheckInStatus(
    val editing: Boolean, val ready: Boolean, val loading: Boolean, val saving: Boolean,
    val closed: Boolean, val revoked: Boolean, val canEdit: Boolean, val canAddPhotos: Boolean,
    val dismissAllowed: Boolean, val photosPreparing: Boolean, val saveAttempt: Long,
)

data class IosHomeCheckInCatalog(
    val beanPickerOpen: Boolean, val beanQuery: String,
    val methods: List<IosHomeCheckInOption>, val beans: List<IosHomeCheckInOption>,
    val shelfBags: List<IosHomeCheckInOption>, val methodsLoading: Boolean, val beansLoading: Boolean,
    val methodsIssueKey: String?, val beansIssueKey: String?,
    val drinks: List<IosHomeCheckInOption> = emptyList(), val drinksLoading: Boolean = false,
    val drinksIssueKey: String? = null,
)

data class IosHomeCheckInOption(val id: String, val name: String?, val detail: String?, val imageUrl: String?)
data class IosCheckInPhoto(val fileName: String, val contentType: String, val bytes: NSData)
data class IosHomeCheckInFieldIssue(val field: String, val messageKey: String)
data class IosHomeCheckInIssues(val loadKey: String?, val saveKey: String?, val fields: List<IosHomeCheckInFieldIssue>)
data class IosHomeCheckInSaved(val id: String, val isPublic: Boolean?, val moderation: String, val visibilityUnconfirmed: Boolean)
data class IosHomeCheckInEvent(val kind: String, val preparationId: String? = null, val visibilityUnconfirmed: Boolean = false)
