package com.coffeepeek.admin.feature.homecheckin.ui

import com.coffeepeek.legacy.homecheckin.domain.HomeCheckInVisibility
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HomeVisibilityTest {
    @Test
    fun missingOrDifferentServerVisibilityNeverClaimsSuccessfulPublication() {
        assertNotNull(homeVisibilitySaveNotice(HomeCheckInVisibility.Public, null))
        assertNotNull(homeVisibilitySaveNotice(HomeCheckInVisibility.Public, HomeCheckInVisibility.Private))
        assertNotNull(homeVisibilitySaveNotice(HomeCheckInVisibility.Private, HomeCheckInVisibility.Public))
        assertNull(homeVisibilitySaveNotice(HomeCheckInVisibility.Public, HomeCheckInVisibility.Public))
        assertNull(homeVisibilitySaveNotice(HomeCheckInVisibility.Private, HomeCheckInVisibility.Private))
        assertNull(homeVisibilitySaveNotice(null, null))
    }
}
