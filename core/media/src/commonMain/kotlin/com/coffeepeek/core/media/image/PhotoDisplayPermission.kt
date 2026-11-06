package com.coffeepeek.core.media.image

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One presentation's permission, separate from the retained encoded representation. */
class PhotoDisplayPermission internal constructor(internal val revalidate: Boolean = false) {
    private val _permission = MutableStateFlow<PhotoPermission?>(null)
    private val _reusablePermission = MutableStateFlow<PhotoPermission?>(null)
    internal val permission = _permission.asStateFlow()
    internal val reusablePermission: PhotoPermission? get() = _reusablePermission.value
    internal fun grant(value: PhotoPermission, reusablePermission: PhotoPermission? = null) {
        _reusablePermission.value = reusablePermission
        _permission.value = value
    }
}
