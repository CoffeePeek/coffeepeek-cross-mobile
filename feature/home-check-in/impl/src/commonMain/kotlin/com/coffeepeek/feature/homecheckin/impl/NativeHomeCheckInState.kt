package com.coffeepeek.feature.homecheckin.impl

/** Read-only presentation values for a native editor; domain and transport models stay behind the session. */
data class NativeHomeCheckInState(
    val draft: NativeHomeCheckInDraft,
    val status: NativeHomeCheckInStatus,
    val catalog: NativeHomeCheckInCatalog,
    val issues: NativeHomeCheckInIssues,
    val saved: NativeHomeCheckInSaved?,
)

data class NativeHomeCheckInDraft(
    val preparedOn: String, val dateLabel: String, val methodCode: String?,
    val dose: String, val durationSeconds: Int?, val tasteScore: Int?,
    val note: String, val drinkName: String, val isPublic: Boolean?,
    val beanName: String?, val shelfBagId: String?, val showShelfBag: Boolean,
    val shelfBagCardUrl: String?, val photos: List<NativeHomeCheckInPhoto>, val existingPhotoUrls: List<String>,
    val customBeanName: String? = null,
    val drinkSlug: String? = null,
)

data class NativeHomeCheckInStatus(
    val editing: Boolean, val ready: Boolean, val loading: Boolean, val saving: Boolean,
    val closed: Boolean, val revoked: Boolean, val canEdit: Boolean, val canAddPhotos: Boolean,
    val dismissAllowed: Boolean, val photosPreparing: Boolean, val saveAttempt: Long,
)

data class NativeHomeCheckInCatalog(
    val beanPickerOpen: Boolean, val beanQuery: String,
    val methods: List<NativeHomeCheckInOption>, val beans: List<NativeHomeCheckInOption>,
    val shelfBags: List<NativeHomeCheckInOption>, val methodsLoading: Boolean, val beansLoading: Boolean,
    val methodsIssueKey: String?, val beansIssueKey: String?,
    val drinks: List<NativeHomeCheckInOption>, val drinksLoading: Boolean, val drinksIssueKey: String?,
)

data class NativeHomeCheckInOption(val id: String, val name: String?, val detail: String?, val imageUrl: String?)
data class NativeHomeCheckInPhoto(val fileName: String, val contentType: String, val bytes: ByteArray)
data class NativeHomeCheckInFieldIssue(val field: String, val messageKey: String)
data class NativeHomeCheckInIssues(val loadKey: String?, val saveKey: String?, val fields: List<NativeHomeCheckInFieldIssue>)
data class NativeHomeCheckInSaved(val id: String, val isPublic: Boolean?, val moderation: String, val visibilityUnconfirmed: Boolean)
data class NativeHomeCheckInEvent(val kind: String, val preparationId: String? = null, val visibilityUnconfirmed: Boolean = false)
