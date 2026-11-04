package com.coffeepeek.admin.feature.community.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.coffeepeek.admin.ui.Navigator

/** A post opened from a shared link. Android composes the migrated entry; iOS does not open post links yet. */
interface CheckInDetailScreenRenderer {
    @Composable
    fun Content(id: String)
}

/** iOS leaves post links to the website (see handleAppLink), so this route is never expected there; if it is reached, it leaves at once. */
internal object UnsupportedCheckInDetailScreenRenderer : CheckInDetailScreenRenderer {
    @Composable
    override fun Content(id: String) = LaunchedEffect(id) { Navigator.popBack() }
}
