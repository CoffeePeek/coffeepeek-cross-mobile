package com.coffeepeek.admin.feature.checkin.data

import com.coffeepeek.domain.model.PendingPhotoUpload
import kotlinx.coroutines.runBlocking

/** Included only by the local release-test init script; not part of distributed builds. */
public object ReleaseDrinkPhotoProbe {
    @JvmStatic
    public fun extract(bytes: ByteArray): Array<ByteArray> = runBlocking {
        createDrinkPhotoExtractor()
            .extract(PendingPhotoUpload("release-fixture.png", "image/png", bytes))
            .map { it.bytes }
            .toTypedArray()
    }
}
