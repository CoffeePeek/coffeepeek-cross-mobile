package com.coffeepeek.feature.contributions.impl

/** iOS form presentation values. Validation and submission remain in the shared feature ViewModels. */
data class NativeContributionFormState(
    val kind: String, val accountVersion: Int, val signedIn: Int, val loading: Boolean,
    val loadError: String? = null, val loadMessage: String? = null,
    val submit: String = "idle", val submitMessage: String? = null, val step: Int = 0,
    val canSubmit: Boolean = false, val title: String = "", val section: String? = null,
    val requestId: String? = null, val resultStatus: String? = null, val editing: Boolean = false,
    val fields: List<NativeContributionInput> = emptyList(), val groups: List<NativeContributionGroup> = emptyList(),
    val sections: List<String> = emptyList(),
    val photos: List<NativeContributionPhoto> = emptyList(), val menu: List<NativeContributionMenuRow> = emptyList(),
    val photoLimit: Int = 0, val menuPhotoLimit: Int = 0,
    val perDay: Boolean = false, val open: String = "", val close: String = "",
    val days: List<NativeContributionDay> = emptyList(), val closedDays: List<Int> = emptyList(),
    val locationHint: String? = null, val resolvingAddress: Boolean = false,
    val map: NativeContributionMap? = null, val hasEdits: Boolean = false, val addressValidated: Boolean? = null,
)
data class NativeContributionInput(val key: String, val value: String, val issues: List<String> = emptyList())
data class NativeContributionGroup(val key: String, val options: List<NativeContributionOption>)
data class NativeContributionOption(val id: String, val name: String, val selected: Boolean, val photoUrl: String? = null)
data class NativeContributionPhoto(val key: String, val url: String?, val fileName: String, val menu: Boolean,
    val groupedRemoval: Boolean = false)
data class NativeContributionMenuRow(val slug: String, val name: String, val availability: String, val price: String)
data class NativeContributionDay(val day: Int, val closed: Boolean, val open: String, val close: String)
data class NativeContributionMap(val latitude: Double?, val longitude: Double?, val cameraLatitude: Double?,
    val cameraLongitude: Double?, val address: String?, val resolving: Boolean, val failed: Boolean, val canConfirm: Boolean)

