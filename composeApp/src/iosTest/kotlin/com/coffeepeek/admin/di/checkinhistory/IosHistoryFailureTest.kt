package com.coffeepeek.admin.di.checkinhistory

import kotlin.test.Test
import kotlin.test.assertEquals

class IosHistoryFailureTest {
    @Test fun callbacksUseTheExistingOwnerOnlyWhileMounted() {
        var retry = 0
        var back = 0
        val source = IosHistoryFailure("message", "retry", "back", { retry++ }, { back++ })
        source.retry()
        source.back()
        source.close()
        source.retry()
        source.back()
        assertEquals(1, retry)
        assertEquals(1, back)
    }
}
