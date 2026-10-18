package com.coffeepeek.admin.utils.image

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One presentation's permission, separate from the retained encoded representation. */
class PhotoDisplayPermission internal constructor() {
    private val _permission = MutableStateFlow<PhotoPermission?>(null)
    internal val permission = _permission.asStateFlow()
    internal fun grant(value: PhotoPermission) { _permission.value = value }
}
